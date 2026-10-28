$content = Get-Content 'src\main\resources\templates\battle-arena.html' -Raw
$content = $content -replace ' successfully! dY-`,\?', ' successfully!'
$content = $content -replace ' uploaded! dYZ%', ' uploaded!'
$content = $content -replace ' recorded! dY-3,\?', ' recorded!'
$content = $content -replace 'dY"'' Go Live', '<i class="fas fa-video"></i> Go Live'
$content = $content -replace 'dY`\?,\? Watch Live', '<i class="fas fa-eye"></i> Watch Live'
$content = $content -replace '<h2>dYs Join Battle</h2>', '<h2><i class="fas fa-user-plus"></i> Join Battle</h2>'
Set-Content -Path 'src\main\resources\templates\battle-arena.html' -Value $content
