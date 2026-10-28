$path = 'src\main\java\com\example\demo\service\BattleTimerService.java'
$c = Get-Content $path -Raw
$c = $c.Replace("userRepository.save(p1);``r``n            userRepository.save(p2);", "userRepository.save(p1);`r`n            userRepository.save(p2);")
Set-Content -Path $path -Value $c
