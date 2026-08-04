/**
 * Zentrix Session Management & Keep-Alive Script
 * - Configurable Session Timeout (Default: 8 hours = 28,800 seconds)
 * - Warning Popup 5 minutes (300 seconds) before expiration
 * - Activity-driven auto-refresh (mousemove, keydown, click, scroll, touchstart)
 * - Multi-Tab Synchronization via localStorage
 */

(function () {
    'use strict';

    const TOTAL_SESSION_SECONDS = 28800; // 8 hours
    const WARNING_SECONDS = 300;         // 5 minutes before expiration
    const PING_THROTTLE_MS = 180000;      // Auto-ping at most once every 3 minutes during user activity

    let lastPingTime = Date.now();
    let sessionRemainingSeconds = TOTAL_SESSION_SECONDS;
    let countdownInterval = null;
    let warningModalOpen = false;

    // Detect Context Path dynamically (e.g. for Tomcat deployments)
    function getContextPath() {
        const path = window.location.pathname;
        const appPages = ['dashboard', 'profile', 'explore', 'wallet', 'events', 'battles', 'messages', 'reels', 'games', 'shop', 'achievements', 'user'];
        for (let i = 1; i < path.split('/').length; i++) {
            if (appPages.includes(path.split('/')[i])) {
                return path.split('/').slice(0, i).join('/');
            }
        }
        return '';
    }

    const ctx = getContextPath();

    // ── 1. Create Warning Modal HTML dynamically if not present ──
    function createWarningModal() {
        if (document.getElementById('sessionWarningModal')) return;

        const modalHtml = `
        <div class="overlay" id="sessionWarningModal" style="display:none; z-index:99999;">
            <div class="modal" style="display:none; max-width:420px; background:var(--card-bg, #1e293b); border:1.5px solid rgba(245,158,11,0.4); border-radius:18px; padding:24px; text-align:center; box-shadow:0 20px 50px rgba(0,0,0,0.5);">
                <div style="width:64px; height:64px; border-radius:50%; background:rgba(245,158,11,0.15); color:#f59e0b; display:flex; align-items:center; justify-content:center; margin:0 auto 16px; font-size:28px;">
                    <i class="fas fa-hourglass-half"></i>
                </div>
                <h3 style="font-size:20px; font-weight:800; color:var(--text-primary, #f8fafc); margin-bottom:8px;">Session Expiring Soon</h3>
                <p style="font-size:14px; color:var(--text-secondary, #94a3b8); margin-bottom:16px; line-height:1.5;">
                    Your session will expire in <strong id="sessionCountdownTimer" style="color:#f59e0b; font-size:16px;">05:00</strong>. Click <strong>'Continue Session'</strong> to stay logged in.
                </p>
                <div style="display:flex; gap:12px; margin-top:20px;">
                    <button id="btnContinueSession" style="flex:1; padding:12px 18px; font-size:14px; font-weight:700; background:linear-gradient(135deg,#10b981,#059669); border:none; color:white; border-radius:10px; cursor:pointer; transition:transform 0.2s, box-shadow 0.2s;">
                        <i class="fas fa-sync-alt" style="margin-right:6px;"></i> Continue Session
                    </button>
                </div>
            </div>
        </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);

        const btn = document.getElementById('btnContinueSession');
        if (btn) {
            btn.addEventListener('click', function () {
                extendSession(true);
            });
        }
    }

    // Format seconds into MM:SS
    function formatTime(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s);
    }

    // ── 2. Keep-Alive AJAX Request ──
    function sendKeepAlive(isExplicitClick) {
        const now = Date.now();
        if (!isExplicitClick && (now - lastPingTime < PING_THROTTLE_MS)) {
            return;
        }

        lastPingTime = now;
        fetch(ctx + '/api/session/keepalive', {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Content-Type': 'application/json'
            }
        })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else if (response.status === 401) {
                handleSessionExpired();
            }
        })
        .then(data => {
            if (data && data.status === 'ok') {
                sessionRemainingSeconds = TOTAL_SESSION_SECONDS;
                localStorage.setItem('zentrix_session_active_time', String(Date.now()));
                dismissWarningModal();
            }
        })
        .catch(err => {
            console.warn('Session keepalive request deferred:', err);
        });
    }

    function extendSession(isExplicitClick) {
        sendKeepAlive(isExplicitClick);
    }

    // ── 3. Warning Modal Show / Dismiss ──
    function showWarningModal() {
        if (warningModalOpen) return;
        warningModalOpen = true;
        createWarningModal();

        const modalOverlay = document.getElementById('sessionWarningModal');
        if (modalOverlay) {
            modalOverlay.style.display = 'flex';
            modalOverlay.classList.add('open');
            const innerModal = modalOverlay.querySelector('.modal');
            if (innerModal) innerModal.style.display = 'block';
        }
    }

    function dismissWarningModal() {
        if (!warningModalOpen) return;
        warningModalOpen = false;

        const modalOverlay = document.getElementById('sessionWarningModal');
        if (modalOverlay) {
            modalOverlay.style.display = 'none';
            modalOverlay.classList.remove('open');
            const innerModal = modalOverlay.querySelector('.modal');
            if (innerModal) innerModal.style.display = 'none';
        }
    }

    // ── 4. Session Timeout Countdown Loop ──
    function startSessionTimer() {
        if (countdownInterval) clearInterval(countdownInterval);

        countdownInterval = setInterval(function () {
            sessionRemainingSeconds--;

            // Synchronize with other tabs if another tab updated activity
            const lastTabActive = localStorage.getItem('zentrix_session_active_time');
            if (lastTabActive) {
                const elapsedSinceTabActive = Math.floor((Date.now() - parseInt(lastTabActive, 10)) / 1000);
                if (elapsedSinceTabActive < TOTAL_SESSION_SECONDS - sessionRemainingSeconds) {
                    sessionRemainingSeconds = Math.max(0, TOTAL_SESSION_SECONDS - elapsedSinceTabActive);
                    if (sessionRemainingSeconds > WARNING_SECONDS) {
                        dismissWarningModal();
                    }
                }
            }

            // Check if warning popup should be displayed (<= 5 mins remaining)
            if (sessionRemainingSeconds <= WARNING_SECONDS && sessionRemainingSeconds > 0) {
                showWarningModal();
                const timerText = document.getElementById('sessionCountdownTimer');
                if (timerText) {
                    timerText.textContent = formatTime(sessionRemainingSeconds);
                }
            }

            // Session Expired
            if (sessionRemainingSeconds <= 0) {
                clearInterval(countdownInterval);
                handleSessionExpired();
            }
        }, 1000);
    }

    function handleSessionExpired() {
        localStorage.removeItem('zentrix_session_active_time');
        window.location.href = ctx + '/login?expired=true';
    }

    // ── 5. User Activity Listeners (Mouse, Keyboard, Scroll, Touch) ──
    function onUserActivity() {
        // If remaining time is well above warning threshold, trigger throttled keep-alive ping
        if (sessionRemainingSeconds > WARNING_SECONDS) {
            sendKeepAlive(false);
        }
    }

    function initActivityListeners() {
        ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'].forEach(eventType => {
            window.addEventListener(eventType, onUserActivity, { passive: true });
        });

        // Listen for multi-tab activity sync
        window.addEventListener('storage', function (e) {
            if (e.key === 'zentrix_session_active_time') {
                sessionRemainingSeconds = TOTAL_SESSION_SECONDS;
                dismissWarningModal();
            }
        });
    }

    // ── 6. Initialization ──
    function initSessionManager() {
        createWarningModal();
        initActivityListeners();
        localStorage.setItem('zentrix_session_active_time', String(Date.now()));
        startSessionTimer();
        // Initial keep-alive ping
        sendKeepAlive(false);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSessionManager);
    } else {
        initSessionManager();
    }
})();
