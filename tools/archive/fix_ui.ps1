$content = Get-Content 'src\main\resources\templates\battle-arena.html' -Raw
$content = $content -replace '(?s)<div class="winner-trophy" style="font-size: 64px; margin-bottom: 15px;">[^<]+</div>\s*<div class="winner-trophy"', '<div class="winner-trophy"'
$content = $content -replace '(?s)<div style="font-size: 24px;">[^<]+</div>\s*<div style="font-size: 24px;"><i class="fas fa-medal" style="color: #FFD700;"></i></div>', '<div style="font-size: 24px;"><i class="fas fa-medal" style="color: #FFD700;"></i></div>'
$content = $content -replace '(?s)<div style="font-size: 24px;">[^<]+</div>\s*<div style="font-size: 24px;"><i class="fas fa-medal" style="color: #C0C0C0;"></i></div>', '<div style="font-size: 24px;"><i class="fas fa-medal" style="color: #C0C0C0;"></i></div>'
Set-Content -Path 'src\main\resources\templates\battle-arena.html' -Value $content
