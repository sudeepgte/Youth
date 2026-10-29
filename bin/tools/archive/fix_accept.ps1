$path = 'src\main\resources\templates\battle-arena.html'
$c = Get-Content $path -Raw
$c = $c.Replace("'application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain,.pdf,.doc,.docx,.txt'", "'application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain,.pdf,.doc,.docx,.txt,video/mp4,audio/mpeg,audio/wav,.mp4,.mp3'")
Set-Content -Path $path -Value $c
