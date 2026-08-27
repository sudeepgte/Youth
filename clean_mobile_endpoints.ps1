$lines = Get-Content 'src\main\java\com\example\demo\controller\MobileApiController.java'
$output = @()
$skip = $false

foreach ($line in $lines) {
    if ($line -match '@GetMapping\("/music/tracks"\)') {
        $skip = $true
        # Remove a few preceding lines if they were just comments
        if ($output[-1] -match '//') { $output = $output[0..($output.Length-2)] }
        if ($output[-1] -match '//') { $output = $output[0..($output.Length-2)] }
        if ($output[-1] -match '^\s*$') { $output = $output[0..($output.Length-2)] }
    }
    if ($skip -and $line -match '@GetMapping\("/stories"\)') {
        $skip = $false
    }
    
    if (-not $skip) {
        $output += $line
    }
}

Set-Content -Path 'src\main\java\com\example\demo\controller\MobileApiController.java' -Value $output
