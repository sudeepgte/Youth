$path = 'src\main\resources\templates\battle-arena.html'
$c = Get-Content $path -Raw
$c = $c.Replace('                                <span th:if="${battle.category == ''Story Teller''}"><i class="fas fa-book-reader" style="color:#8B5CF6;"></i> Upload Story Video / Audio / Doc</span>', '')
Set-Content -Path $path -Value $c
