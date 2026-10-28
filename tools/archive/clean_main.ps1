$content = Get-Content 'src\main\java\com\example\demo\MainController.java' -Raw
$content = $content -replace '(?m)^import com\.example\.demo\.music\.room\..*?\r?\n', ''
$content = $content -replace '(?m)^\s*@Autowired\s*\r?\n\s*private MusicRoomRepository musicRoomRepository;\r?\n', ''
$content = $content -replace '(?s)\s*// Ongoing music battles.*?model\.addAttribute\("ongoingMusicBattles", ongoingBattles\);', ''
Set-Content -Path 'src\main\java\com\example\demo\MainController.java' -Value $content
