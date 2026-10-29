(function() {
    // 1. Get token from URL or sessionStorage
    const urlParams = new URLSearchParams(window.location.search);
    let token = urlParams.get('auth');
    
    if (!token) {
        token = sessionStorage.getItem('zentrix_auth_token');
        if (token && !urlParams.has('auth')) {
            // Restore token to URL if it's missing (e.g. on refresh)
            const url = new URL(window.location.href);
            url.searchParams.set('auth', token);
            window.location.replace(url.toString());
            return; // Stop execution, redirecting
        }
    } else {
        // Save to sessionStorage (isolated per tab)
        sessionStorage.setItem('zentrix_auth_token', token);
    }
    
    if (token) {
        // 2. Attach token to all internal links and forms
        function syncTabState() {
            // Update Links
            const links = document.getElementsByTagName('a');
            for (let a of links) {
                try {
                    if (a.href && a.href.startsWith(window.location.origin)) {
                        const url = new URL(a.href);
                        if (!url.searchParams.has('auth')) {
                            url.searchParams.set('auth', token);
                            a.href = url.toString();
                        }
                    }
                } catch(e) {}
            }
            
            // Update Forms
            const forms = document.getElementsByTagName('form');
            for (let f of forms) {
                if (f.action && f.action.startsWith(window.location.origin)) {
                    if (!f.querySelector('input[name="auth"]')) {
                        const input = document.createElement('input');
                        input.type = 'hidden';
                        input.name = 'auth';
                        input.value = token;
                        f.appendChild(input);
                    }
                }
            }
        }
        
        // Run when DOM is ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initSync);
        } else {
            initSync();
        }

        function initSync() {
            // Initial run
            syncTabState();
            
            // Handle dynamic content (like loading more posts)
            const observer = new MutationObserver(syncTabState);
            if (document.body) {
                observer.observe(document.body, { childList: true, subtree: true });
            }
        }
    }

    // Handle logout link specifically
    window.addEventListener('load', function() {
        const logoutLinks = document.querySelectorAll('a[href="/logout"]');
        logoutLinks.forEach(link => {
            link.addEventListener('click', function() {
                sessionStorage.removeItem('zentrix_auth_token');
            });
        });
    });
})();
