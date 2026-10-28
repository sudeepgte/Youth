$content = Get-Content 'src\main\java\com\example\demo\MainController.java' -Raw

$imports = @"
import com.example.demo.model.AdPlacement;
import com.example.demo.service.AdvertisementService;
"@
$content = $content -replace 'import com\.example\.demo\.service\.FeedAlgorithmService;', "import com.example.demo.service.FeedAlgorithmService;`r`n$imports"

$autowired = @"
    @Autowired
    private AdvertisementService advertisementService;
"@
$content = $content -replace 'private FeedAlgorithmService feedAlgorithmService;', "private FeedAlgorithmService feedAlgorithmService;`r`n$autowired"

$homeLogic = @"
        model.addAttribute("adsHero", advertisementService.getValidAdsForPlacement(AdPlacement.HERO));
        model.addAttribute("adsFeatured", advertisementService.getValidAdsForPlacement(AdPlacement.FEATURED));
        model.addAttribute("adsBetween", advertisementService.getValidAdsForPlacement(AdPlacement.BETWEEN_SECTIONS));
        model.addAttribute("adsBottom", advertisementService.getValidAdsForPlacement(AdPlacement.BOTTOM_CTA));
"@
$content = $content -replace 'return "home";', "$homeLogic`r`n        return `"home`";"

Set-Content -Path 'src\main\java\com\example\demo\MainController.java' -Value $content
