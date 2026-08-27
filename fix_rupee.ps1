$path = 'src\main\resources\templates\admin-battles.html'
$c = Get-Content $path -Raw -Encoding UTF8
$c = $c.Replace("td th:text=`"`${'?' + b.entryFee}`">?0.00</td", "td th:text=`"`${'&#8377;' + b.entryFee}`">&#8377;0.00</td")
Set-Content -Path $path -Value $c -Encoding UTF8
