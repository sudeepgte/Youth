$content = Get-Content 'src\main\java\com\example\demo\controller\BattleLiveWebSocketController.java' -Raw
$content = $content.Replace("@MessageMapping(""/battle/{battleId}/viewer-join"")`r`n    public int getViewerCount", "public int getViewerCount")
$content = $content.Replace("@MessageMapping(""/battle/{battleId}/viewer-join"")`n    public int getViewerCount", "public int getViewerCount")
Set-Content -Path 'src\main\java\com\example\demo\controller\BattleLiveWebSocketController.java' -Value $content
