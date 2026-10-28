$path = 'src\main\resources\templates\home.html'
$c = Get-Content $path -Raw
$c = $c.Replace('adsList != null and !adsList.isEmpty()', 'adsList != null and !#lists.isEmpty(adsList)')
Set-Content -Path $path -Value $c
