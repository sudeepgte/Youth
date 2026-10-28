$content = Get-Content 'src\main\java\com\example\demo\controller\AdminAdvertisementController.java' -Raw
$content = $content.Replace("advertisementRepository.countActiveAds()", "advertisementRepository.countByStatus(AdStatus.ACTIVE)")
$content = $content.Replace("advertisementRepository.countScheduledAds()", "advertisementRepository.countByStatus(AdStatus.SCHEDULED)")
$content = $content.Replace("advertisementRepository.countPausedAds()", "advertisementRepository.countByStatus(AdStatus.PAUSED)")
Set-Content -Path 'src\main\java\com\example\demo\controller\AdminAdvertisementController.java' -Value $content
