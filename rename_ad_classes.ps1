$path = 'src\main\resources\templates\home.html'
$c = Get-Content $path -Raw
$c = $c.Replace('ad-carousel', 'promo-carousel')
$c = $c.Replace('ad-slide', 'promo-slide')
$c = $c.Replace('ad-dot', 'promo-dot')
$c = $c.Replace('ad-cta-btn', 'promo-cta-btn')
Set-Content -Path $path -Value $c
