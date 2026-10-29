$content = Get-Content 'src\main\resources\templates\home.html' -Raw
$content = $content.Replace("`$adsHero", "`${adsHero}")
$content = $content.Replace("`$adsFeatured", "`${adsFeatured}")
$content = $content.Replace("`$adsBetween", "`${adsBetween}")
$content = $content.Replace("`$adsBottom", "`${adsBottom}")
Set-Content -Path 'src\main\resources\templates\home.html' -Value $content
