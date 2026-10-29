$content = Get-Content 'src\main\resources\templates\battle-live.html' -Raw
$content = $content.Replace("constraints.video = {`r`n                `r`n                `r`n                `r`n            };", "constraints.video = true;")
$content = $content.Replace("constraints.video = {`n                `n                `n                `n            };", "constraints.video = true;")
Set-Content -Path 'src\main\resources\templates\battle-live.html' -Value $content
