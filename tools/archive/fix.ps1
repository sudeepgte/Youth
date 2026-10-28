$content = Get-Content 'src\main\resources\templates\battle-live.html' -Raw
$content = $content -replace 'width: \{ ideal: 640, max: 1280 \},', ''
$content = $content -replace 'height: \{ ideal: 480, max: 720 \},', ''
$content = $content -replace 'frameRate: \{ ideal: 20, max: 24 \}', 'facingMode: "user"'

$content = $content -replace 'function toggleCamera\(\) \{', 'async function toggleCamera() {'
$content = $content -replace 'if \(!localStream\) \{\s*alert\("Camera not available or access denied."\);\s*return;\s*\}', 'if (!localStream) { await startLocalMedia(); if (!localStream) { alert("Camera not available or access denied."); return; } }'

$content = $content -replace 'function toggleMic\(\) \{', 'async function toggleMic() {'
$content = $content -replace 'if \(!localStream\) \{\s*alert\("Microphone not available or access denied."\);\s*return;\s*\}', 'if (!localStream) { await startLocalMedia(); if (!localStream) { alert("Microphone not available or access denied."); return; } }'

Set-Content -Path 'src\main\resources\templates\battle-live.html' -Value $content
