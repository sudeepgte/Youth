import sys
with open('src/main/resources/templates/wallet.html', 'r', encoding='utf-8') as f:
    content = f.read()

start_marker = '<!-- Center Feed -->'
end_marker = '<!-- Right Sidebar (Ads & Notifications) -->'

if start_marker in content and end_marker in content:
    pre = content.split(start_marker)[0]
    post = content.split(end_marker)[1]
    
    wallet_html = '''
        <!-- Center Feed (Wallet) -->
        <main class="social-feed">
            <div class="profile-header" style="flex-direction: column; align-items: center; text-align: center;">
                <div style="width: 80px; height: 80px; border-radius: 50%; background: linear-gradient(135deg, #10B981, #3B82F6); display: flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                    <i class="fas fa-wallet" style="font-size: 36px; color: white;"></i>
                </div>
                <h1 style="font-size: 28px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px;">My Wallet</h1>
                <p style="color: var(--text-secondary); margin-bottom: 24px;">Manage your funds and transactions</p>
                
                <div style="background: rgba(16, 185, 129, 0.1); border: 2px solid rgba(16, 185, 129, 0.3); border-radius: 24px; padding: 30px; width: 100%; max-width: 400px; margin-bottom: 30px;">
                    <h3 style="font-size: 14px; text-transform: uppercase; color: #10B981; font-weight: 700; letter-spacing: 1px; margin-bottom: 8px;">Current Balance</h3>
                    <div style="font-size: 48px; font-weight: 900; color: var(--text-primary); display: flex; justify-content: center; align-items: center; gap: 8px;">
                        <span style="color: #10B981;">&#8377;</span> <span th:text="${user.walletBalance}">0.0</span>
                    </div>
                </div>
                
                <div style="display: flex; gap: 16px; width: 100%; max-width: 400px;">
                    <button onclick="openM('addFundsModal')" class="btn-follow" style="flex: 1; padding: 12px; font-size: 15px; background: #10B981;"><i class="fas fa-plus-circle"></i> Add Funds</button>
                    <button onclick="openM('withdrawModal')" class="btn-edit" style="flex: 1; justify-content: center; padding: 12px; font-size: 15px;"><i class="fas fa-money-bill-wave"></i> Withdraw</button>
                </div>
            </div>
            
            <h3 style="font-size: 18px; font-weight: 800; margin: 30px 0 20px 0;"><i class="fas fa-history"></i> Recent Transactions</h3>
            
            <div style="background: var(--card-bg); border: 1px solid var(--glass-border); border-radius: 16px; overflow: hidden;">
                <div class="empty-state" style="padding: 40px 20px;">
                    <i class="fas fa-receipt"></i>
                    <p>No recent transactions.</p>
                </div>
            </div>
            
        </main>
        
        <!-- Add Funds Modal -->
        <div class="overlay" id="addFundsModal">
            <div class="modal" style="max-width: 400px;">
                <div style="padding: 20px; border-bottom: 1px solid var(--glass-border); display: flex; justify-content: space-between; align-items: center;">
                    <h3 style="font-size: 18px; font-weight: 700;">Add Funds</h3>
                    <button onclick="closeM('addFundsModal')" style="background: none; border: none; font-size: 20px; color: var(--text-secondary); cursor: pointer;"><i class="fas fa-times"></i></button>
                </div>
                <div style="padding: 20px;">
                    <form th:action="@{/wallet/add}" method="POST">
                        <div class="form-group" style="margin-bottom: 16px;">
                            <label style="display: block; font-size: 13px; font-weight: 600; color: var(--text-secondary); margin-bottom: 8px;">Amount (&#8377;)</label>
                            <input type="number" name="amount" min="1" step="1" placeholder="e.g., 500" required style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--glass-border); background: var(--bg-color); color: var(--text-primary);">
                        </div>
                        <button type="submit" class="btn-follow" style="width: 100%; padding: 12px; background: #10B981;">Proceed to Pay</button>
                    </form>
                </div>
            </div>
        </div>

        <!-- Withdraw Modal -->
        <div class="overlay" id="withdrawModal">
            <div class="modal" style="max-width: 400px;">
                <div style="padding: 20px; border-bottom: 1px solid var(--glass-border); display: flex; justify-content: space-between; align-items: center;">
                    <h3 style="font-size: 18px; font-weight: 700;">Withdraw Funds</h3>
                    <button onclick="closeM('withdrawModal')" style="background: none; border: none; font-size: 20px; color: var(--text-secondary); cursor: pointer;"><i class="fas fa-times"></i></button>
                </div>
                <div style="padding: 20px;">
                    <form th:action="@{/wallet/withdraw}" method="POST">
                        <div class="form-group" style="margin-bottom: 16px;">
                            <label style="display: block; font-size: 13px; font-weight: 600; color: var(--text-secondary); margin-bottom: 8px;">Amount to Withdraw (&#8377;)</label>
                            <input type="number" name="amount" min="1" step="1" placeholder="e.g., 500" required style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--glass-border); background: var(--bg-color); color: var(--text-primary);">
                        </div>
                        <button type="submit" class="btn-follow" style="width: 100%; padding: 12px;">Withdraw</button>
                    </form>
                </div>
            </div>
        </div>

        <!-- Right Sidebar (Ads & Notifications) -->
    '''
    
    with open('src/main/resources/templates/wallet.html', 'w', encoding='utf-8') as f:
        f.write(pre + wallet_html + post)
