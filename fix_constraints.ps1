$content = Get-Content 'src\main\resources\templates\battle-live.html' -Raw
$content = $content.Replace("facingMode: `"user`"", "")
Set-Content -Path 'src\main\resources\templates\battle-live.html' -Value $content
