$path = 'src\main\java\com\example\demo\service\BattleTimerService.java'
$c = Get-Content $path -Raw
$c = $c -replace '`r`n', "`r`n"
Set-Content -Path $path -Value $c
