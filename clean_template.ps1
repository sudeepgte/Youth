$lines = Get-Content 'src\main\resources\templates\fragments\template.html'
$output = @()
$skip = $false

foreach ($line in $lines) {
    if ($line -match '<header th:fragment="musicNavbar') {
        $skip = $true
    }
    if ($skip -and $line -match '</header>') {
        $skip = $false
        continue
    }
    
    if (-not $skip) {
        $output += $line
    }
}

Set-Content -Path 'src\main\resources\templates\fragments\template.html' -Value $output
