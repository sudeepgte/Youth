$c = Get-Content 'src\main\java\com\example\demo\controller\BattleLiveWebSocketController.java' -Raw
$c = $c -replace 'public void handleViewerJoin', "public int getViewerCount(Long battleId) { Set<Long> viewers = battleViewers.get(battleId); return viewers != null ? viewers.size() : 0; }`r`n`r`n    @MessageMapping(""/battle/{battleId}/viewer-join"")`r`n    public void handleViewerJoin"
Set-Content -Path 'src\main\java\com\example\demo\controller\BattleLiveWebSocketController.java' -Value $c

$bc = Get-Content 'src\main\java\com\example\demo\controller\BattleController.java' -Raw
$bc = $bc -replace 'public class BattleController \{', "public class BattleController {`r`n`r`n    @Autowired private BattleLiveWebSocketController battleLiveWebSocketController;"
$bc = $bc -replace 'model\.addAttribute\("giftCount", giftCount\);', "model.addAttribute(""giftCount"", giftCount);`r`n        model.addAttribute(""viewerCount"", battleLiveWebSocketController.getViewerCount(battle.getId()));"
Set-Content -Path 'src\main\java\com\example\demo\controller\BattleController.java' -Value $bc
