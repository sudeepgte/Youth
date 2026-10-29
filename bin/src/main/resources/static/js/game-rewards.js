const GameRewards = {
    showHistory: async function() {
        // Create modal overlay
        let overlay = document.getElementById('coin-history-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'coin-history-overlay';
            overlay.style.cssText = 'position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.7); z-index:99999; display:flex; align-items:center; justify-content:center; padding:20px; box-sizing:border-box; backdrop-filter:blur(4px);';
            
            const modal = document.createElement('div');
            modal.style.cssText = 'background:#1a1a2e; border:1px solid #22D3EE; border-radius:16px; width:100%; max-width:400px; max-height:80vh; display:flex; flex-direction:column; box-shadow:0 10px 40px rgba(0,0,0,0.5); overflow:hidden; font-family:"Inter",sans-serif;';
            
            const header = document.createElement('div');
            header.style.cssText = 'padding:16px 20px; border-bottom:1px solid rgba(255,255,255,0.1); display:flex; justify-content:space-between; align-items:center; background:linear-gradient(90deg, #1a1a2e 0%, #121230 100%);';
            header.innerHTML = '<h3 style="margin:0; color:#fff; font-size:16px; display:flex; align-items:center; gap:8px;"><i class="fas fa-history" style="color:#22D3EE;"></i> Coin History</h3><button id="close-coin-modal" style="background:none; border:none; color:#94a3b8; font-size:20px; cursor:pointer;">&times;</button>';
            
            const content = document.createElement('div');
            content.id = 'coin-history-content';
            content.style.cssText = 'padding:0; overflow-y:auto; flex:1; background:#0f0f23;';
            content.innerHTML = '<div style="padding:40px; text-align:center; color:#94a3b8;"><div class="spinner" style="margin:0 auto 10px; border:3px solid rgba(255,255,255,0.1); border-top-color:#22D3EE; border-radius:50%; width:24px; height:24px; animation:spin 1s linear infinite;"></div>Loading...</div>';
            
            // Add keyframes for spinner if not exists
            if (!document.getElementById('coin-spinner-style')) {
                const style = document.createElement('style');
                style.id = 'coin-spinner-style';
                style.innerHTML = '@keyframes spin { to { transform: rotate(360deg); } }';
                document.head.appendChild(style);
            }
            
            modal.appendChild(header);
            modal.appendChild(content);
            overlay.appendChild(modal);
            document.body.appendChild(overlay);
            
            document.getElementById('close-coin-modal').addEventListener('click', () => {
                overlay.style.display = 'none';
            });
            overlay.addEventListener('click', (e) => {
                if (e.target === overlay) overlay.style.display = 'none';
            });
        }
        
        overlay.style.display = 'flex';
        const content = document.getElementById('coin-history-content');
        
        try {
            const res = await fetch('/api/coins/history');
            if (!res.ok) throw new Error('Failed to fetch');
            const data = await res.json();
            
            if (data.length === 0) {
                content.innerHTML = '<div style="padding:40px; text-align:center; color:#94a3b8; font-size:14px;"><i class="fas fa-box-open" style="font-size:32px; margin-bottom:12px; opacity:0.5; display:block;"></i>No coin history found yet.</div>';
                return;
            }
            
            let html = '<div style="display:flex; flex-direction:column;">';
            data.forEach(tx => {
                const date = new Date(tx.timestamp).toLocaleString(undefined, { month:'short', day:'numeric', hour:'2-digit', minute:'2-digit' });
                const isPositive = tx.amount > 0;
                const sign = isPositive ? '+' : '';
                const color = isPositive ? '#10B981' : '#EF4444';
                
                html += `
                <div style="display:flex; justify-content:space-between; align-items:center; padding:16px 20px; border-bottom:1px solid rgba(255,255,255,0.05); transition:background 0.2s;">
                    <div style="display:flex; flex-direction:column; gap:4px;">
                        <span style="color:#fff; font-size:14px; font-weight:600;">${tx.source || 'Zentrix Game'}</span>
                        <span style="color:#fff; font-size:12px;">${tx.reason || 'Reward'} , ${date}</span>
                    </div>
                    <div style="font-size:16px; font-weight:700; color:${color}; background:${color}15; padding:4px 10px; border-radius:8px;">
                        ${sign}${tx.amount}
                    </div>
                </div>`;
            });
            html += '</div>';
            content.innerHTML = html;
        } catch (err) {
            console.error(err);
            content.innerHTML = '<div style="padding:30px; text-align:center; color:#EF4444; font-size:14px;">Error loading history.</div>';
        }
    }
};
window.GameRewards = GameRewards;
