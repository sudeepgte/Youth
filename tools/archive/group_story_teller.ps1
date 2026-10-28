$path = 'src\main\resources\templates\battle-arena.html'
$c = Get-Content $path -Raw

$c = $c.Replace("battle.category == 'Writing' or battle.category == 'Poetry'", "battle.category == 'Writing' or battle.category == 'Poetry' or battle.category == 'Story Teller'")
$c = $c.Replace("Accepts: .pdf, .doc, .docx, .txt</span>", "Accepts: .pdf, .doc, .docx, .txt, .mp4, .mp3</span>")

Set-Content -Path $path -Value $c
