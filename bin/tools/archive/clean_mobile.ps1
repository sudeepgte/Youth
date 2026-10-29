$content = Get-Content 'src\main\java\com\example\demo\controller\MobileApiController.java' -Raw
$content = $content -replace '(?m)^import com\.example\.demo\.music\..*?\r?\n', ''
$content = $content -replace '(?m)^\s*@Autowired\s+private\s+(TrackRepository|TrackLikeRepository|MusicRoomRepository|MusicRoomVoteRepository|MusicService)\s+\w+;\r?\n', ''
$content = $content -replace '(?s)\s*// --- Music ---.*?// --- Battles ---', "`r`n`r`n    // --- Battles ---"
Set-Content -Path 'src\main\java\com\example\demo\controller\MobileApiController.java' -Value $content
