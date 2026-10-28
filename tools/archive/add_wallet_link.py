import os

templates_dir = 'src/main/resources/templates'

for filename in os.listdir(templates_dir):
    if filename.endswith('.html'):
        filepath = os.path.join(templates_dir, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        target = '<a th:href="@{/music}" class="sidebar-link"><i class="fas fa-music"></i> Music</a>'
        replacement = '<a th:href="@{/music}" class="sidebar-link"><i class="fas fa-music"></i> Music</a>\n            <a th:href="@{/wallet}" class="sidebar-link"><i class="fas fa-wallet"></i> Wallet</a>'
        
        if target in content and replacement not in content:
            content = content.replace(target, replacement)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Updated {filename}")
