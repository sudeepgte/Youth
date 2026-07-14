function getContextPath() {
    const path = window.location.pathname;
    return path.startsWith('/zentrix') ? '/' + path.split('/')[1] : '';
}
/**
 * game-rewards.js
 * Shared utility to handle awarding Zentrix coins after playing/winning games.
 */
const GameRewards = {
    /**
     * Report game result to server to earn rewards.
     * @param {string} gameName - Name of the game
     * @param {string} result - "WIN", "PLAY", or "SCORE"
     * @param {number} score - The numeric score (used if result is "SCORE")
     */
    award: async function(gameName, result = "PLAY", score = 0) {
        console.log(`[GameRewards] Reporting ${result} for ${gameName}` + (score ? ` with score ${score}` : ''));
        
        try {
            const response = await fetch(getContextPath() + '/api/games/reward', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ gameName, result, score: score.toString() })
            });

            if (response.ok) {
                const data = await response.json();
                this.showRewardToast(data.message);
                window.dispatchEvent(new CustomEvent('zentrixCoinsUpdated', { detail: data }));
            }
        } catch (error) {
            console.error(`[GameRewards] Error:`, error);
        }
    },

    /** Shortcut for score-based rewards */
    awardScore: function(gameName, score) {
        this.award(gameName, "SCORE", score);
    },

    /** Fetch and show coin history in a modal */
    showHistory: async function() {
        try {
            const response = await fetch(getContextPath() + '/api/games/history');
            if (!response.ok) throw new Error("Failed to fetch history");
            const history = await response.json();
            this.renderHistoryModal(history);
        } catch (error) {
            console.error(error);
            this.showRewardToast("Failed to load coin history.");
        }
    },

    renderHistoryModal: function(history) {
        // Remove existing modal if any
        const existing = document.getElementById('coinHistoryModal');
        if (existing) existing.remove();

        const modal = document.createElement('div');
        modal.id = 'coinHistoryModal';
        modal.style.cssText = `
            position: fixed;
            inset: 0;
            background: rgba(0,0,0,0.85);
            backdrop-filter: blur(8px);
            z-index: 10001;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            font-family: 'Inter', sans-serif;
            color: #fff;
        `;

        const content = document.createElement('div');
        content.style.cssText = `
            background: #1a1a20;
            width: 100%;
            max-width: 500px;
            border-radius: 24px;
            border: 1px solid rgba(255,193,7,0.3);
            overflow: hidden;
            display: flex;
            flex-direction: column;
            max-height: 80vh;
            box-shadow: 0 25px 50px rgba(0,0,0,0.5);
            animation: modalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
        `;

        let listHtml = history.map(t => `
            <div style="padding: 15px 20px; border-bottom: 1px solid rgba(255,255,255,0.05); display: flex; align-items: center; justify-content: space-between;">
                <div>
                    <div style="font-weight: 700; font-size: 14px; color: #fff;">${t.source}</div>
                    <div style="font-size: 12px; color: rgba(255,255,255,0.5); margin-top: 2px;">${t.reason} \u2022 ${new Date(t.timestamp).toLocaleDateString()}</div>
                </div>
                <div style="color: #FFC107; font-weight: 900; font-size: 16px;">+${t.amount} <i class="fas fa-coins"></i></div>
            </div>
        `).join('');

        if (history.length === 0) {
            listHtml = '<div style="padding: 40px; text-align: center; color: rgba(255,255,255,0.4);">No transactions yet. Start playing to earn coins!</div>';
        }

        content.innerHTML = `
            <div style="padding: 25px; border-bottom: 1px solid rgba(255,255,255,0.05); display: flex; align-items: center; justify-content: space-between; background: rgba(255,193,7,0.05);">
                <h2 style="margin: 0; font-size: 18px; font-weight: 800; color: #FFC107;"><i class="fas fa-history"></i> Coin History</h2>
                <button onclick="document.getElementById('coinHistoryModal').remove()" style="background: none; border: none; color: #fff; font-size: 20px; cursor: pointer; opacity: 0.5;"><i class="fas fa-times"></i></button>
            </div>
            <div style="overflow-y: auto; flex: 1;">
                ${listHtml}
            </div>
            <div style="padding: 20px; text-align: center; background: rgba(0,0,0,0.2);">
                <button onclick="document.getElementById('coinHistoryModal').remove()" style="background: #FFC107; color: #000; border: none; border-radius: 12px; padding: 10px 25px; font-weight: 800; cursor: pointer; width: 100%;">Close</button>
            </div>
        `;

        if (!document.getElementById('modalAnims')) {
            const s = document.createElement('style');
            s.id = 'modalAnims';
            s.textContent = `@keyframes modalPop { from { opacity:0; transform:scale(0.9); } to { opacity:1; transform:scale(1); } }`;
            document.head.appendChild(s);
        }

        modal.appendChild(content);
        modal.onclick = (e) => { if (e.target === modal) modal.remove(); };
        document.body.appendChild(modal);
    },

    /**
     * UI helper to show a premium reward notification
     */
    showRewardToast: function(message) {
        let toastContainer = document.getElementById('rewardToastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'rewardToastContainer';
            toastContainer.style.cssText = `
                position: fixed;
                bottom: 30px;
                left: 50%;
                transform: translateX(-50%);
                z-index: 10002;
                display: flex;
                flex-direction: column;
                gap: 10px;
                pointer-events: none;
            `;
            document.body.appendChild(toastContainer);
        }

        const toast = document.createElement('div');
        toast.style.cssText = `
            background: rgba(20, 20, 25, 0.95);
            color: #fff;
            padding: 12px 20px;
            border-radius: 16px;
            border: 1px solid #FFC107;
            box-shadow: 0 10px 25px rgba(0,0,0,0.3);
            font-family: 'Inter', sans-serif;
            font-weight: 600;
            font-size: 13px;
            display: flex;
            align-items: center;
            gap: 10px;
            animation: rewardSlideUp 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
            pointer-events: auto;
        `;

        toast.innerHTML = `
            <div style="background: #FFC107; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #000; flex-shrink:0;">
                <i class="fas fa-coins" style="font-size:12px;"></i>
            </div>
            <span>${message}</span>
        `;

        toastContainer.appendChild(toast);

        if (!document.getElementById('rewardToastStyles')) {
            const style = document.createElement('style');
            style.id = 'rewardToastStyles';
            style.textContent = `
                @keyframes rewardSlideUp { from { opacity: 0; transform: translateY(50px); } to { opacity: 1; transform: translateY(0); } }
                @keyframes rewardFadeOut { from { opacity: 1; transform: scale(1); } to { opacity: 0; transform: scale(0.9); } }
            `;
            document.head.appendChild(style);
        }

        setTimeout(() => {
            toast.style.animation = 'rewardFadeOut 0.4s ease forwards';
            setTimeout(() => toast.remove(), 400);
        }, 4000);
    }
};


