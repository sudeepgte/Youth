$content = Get-Content 'src\main\resources\templates\admin-dashboard.html' -Raw
$newLink = @"
            <a th:href="@{/admin/advertisements}" class="sidebar-link"><i class="fas fa-ad" style="color: inherit;"></i> Manage Ads</a>
"@
$content = $content -replace '<a th:href="@\{/events/admin/manage\}" class="sidebar-link"><i class="fas fa-calendar-alt"', "$newLink`r`n            <a th:href=`"@{/events/admin/manage}`" class=`"sidebar-link`"><i class=`"fas fa-calendar-alt`""
Set-Content -Path 'src\main\resources\templates\admin-dashboard.html' -Value $content
