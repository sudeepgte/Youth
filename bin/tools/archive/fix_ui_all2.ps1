$content = Get-Content 'src\main\resources\templates\battle-arena.html' -Raw
$content = $content.Replace(" successfully! dY-`,?", " successfully!")
$content = $content.Replace(" uploaded! dYZ%", " uploaded!")
$content = $content.Replace(" recorded! dY-3,?", " recorded!")
$content = $content.Replace("dY`"'' Go Live", "<i class=`"fas fa-video`"></i> Go Live")
$content = $content.Replace("dY`?,? Watch Live", "<i class=`"fas fa-eye`"></i> Watch Live")
$content = $content.Replace("<h2>dYs Join Battle</h2>", "<h2><i class=`"fas fa-user-plus`"></i> Join Battle</h2>")
Set-Content -Path 'src\main\resources\templates\battle-arena.html' -Value $content
