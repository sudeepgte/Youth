$replacements = @{
    'ðŸ—³ï¸ ' = '&#128499;'
    'â€œ' = '&ldquo;'
    'â€ ' = '&rdquo;'
    'â€º' = '&rsaquo;'
    'â€”' = '&mdash;'
    'â€¢' = '&bull;'
    'Ã¢â‚¬Â¢' = '&bull;'
    'Ãƒâ€”' = '&times;'
    'â†’' = '&rarr;'
    'âœ•' = '&times;'
    'â ¤ï¸ ' = '&#10084;&#65039;'
    'ðŸ˜‚' = '&#128514;'
    'ðŸ”¥' = '&#128293;'
    'ðŸ˜ ' = '&#128525;'
    'ðŸ‘ ' = '&#128079;'
    'ðŸ™Œ' = '&#128588;'
    'ðŸ˜¢' = '&#128546;'
    'ðŸ˜®' = '&#128558;'
    'ðŸŽ‰' = '&#127881;'
    'ðŸ’¯' = '&#128175;'
    'ðŸ˜€' = '&#128512;'
    'ðŸ ±' = '&#128049;'
    'ðŸ •' = '&#127829;'
    'âš½' = '&#9917;'
    'âœ‰ï¸ ' = '&#9993;&#65039;'
    'â”€' = '-'
}

$files = Get-ChildItem -Path ".\src\main\resources\templates" -Recurse -Filter "*.html"

foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $modified = $false
    foreach ($key in $replacements.Keys) {
        if ($content -match [regex]::Escape($key)) {
            $content = $content -replace [regex]::Escape($key), $replacements[$key]
            $modified = $true
        }
    }
    if ($modified) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8
        Write-Host "Fixed $($file.Name)"
    }
}
