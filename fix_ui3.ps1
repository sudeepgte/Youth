$content = Get-Content 'src\main\resources\templates\battle-arena.html' -Raw
$content = $content -replace '(?s)<div style="font-size: 24px;">[^<]+</div>\s*<div style="font-size: 24px;"><i class="fas fa-medal" style="color: #CD7F32;"></i></div>', '<div style="font-size: 24px;"><i class="fas fa-medal" style="color: #CD7F32;"></i></div>'
Set-Content -Path 'src\main\resources\templates\battle-arena.html' -Value $content
