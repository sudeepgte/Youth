$path = 'src\main\resources\templates\battle-arena.html'
$c = Get-Content $path -Raw

# 1. Label
$c = $c -replace "(<span th:if=`"\`$\{battle\.category == 'Comedy'\}`"><i class=`"fas fa-grin-tears`" style=`"color:#FBBF24;`"></i> Upload Comedy Video</span>)", "`$1`r`n                                <span th:if=`"`${battle.category == 'Story Teller'}`"><i class=`"fas fa-book-reader`" style=`"color:#8B5CF6;`"></i> Upload Story Video / Audio / Doc</span>"

# 2. Upload Zone Icon
$c = $c -replace "(<span th:if=`"\`$\{battle\.category == 'Comedy'\}`"><i class=`"fas fa-grin-tears`" style=`"color:#FBBF24;`"></i></span>)", "`$1`r`n                                    <span th:if=`"`${battle.category == 'Story Teller'}`"><i class=`"fas fa-book-reader`" style=`"color:#8B5CF6;`"></i></span>"

# 3. Subtext
$c = $c -replace "(<span th:if=`"\`$\{battle\.category == 'Comedy'\}`">Accepts: \.mp4, \.mov, \.webm</span>)", "`$1`r`n                                    <span th:if=`"`${battle.category == 'Story Teller'}`">Accepts: .mp4, .mp3, .pdf, .txt</span>"

# 4. Filter dropdowns if I missed any? I got both earlier.

Set-Content -Path $path -Value $c
