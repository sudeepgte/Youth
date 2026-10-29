$c = Get-Content 'src\main\java\com\example\demo\MainController.java' -Raw
$c = $c -replace 'totalAdminCommission \+= entryFee \* 0\.07 \* joinsCount;', "double pct = b.getAdminCommissionPct() != null ? b.getAdminCommissionPct() : 7.0;`r`n            totalAdminCommission += entryFee * (pct / 100.0) * joinsCount;"
Set-Content 'src\main\java\com\example\demo\MainController.java' -Value $c
