$path = 'src\main\resources\templates\battle-arena.html'
$c = Get-Content $path -Raw

# 1. Add category-option
$oldOption = '<div class="category-option" onclick="selectBattleCategory(this, ''Comedy'')"><i class="fas fa-grin-tears"></i> Comedy</div>'
$newOption = $oldOption + "`r`n                            <div class=`"category-option`" onclick=`"selectBattleCategory(this, 'Story Teller')`"><i class=`"fas fa-book-reader`"></i> Story Teller</div>"
$c = $c.Replace($oldOption, $newOption)

# 2. Add to <option value="Comedy"> if it exists, or just append it after Writing
$oldWritingOption = '<option value="Writing">Writing</option>'
$newWritingOption = $oldWritingOption + "`r`n                        <option value=`"Story Teller`">Story Teller</option>"
$c = $c.Replace($oldWritingOption, $newWritingOption)

# 3. Add to the other list (Video) if needed. Let's just find Video Editing
$oldVideoOption = '<option value="Video Editing">Video Editing</option>'
$newVideoOption = $oldVideoOption + "`r`n                        <option value=`"Story Teller`">Story Teller</option>"
$c = $c.Replace($oldVideoOption, $newVideoOption)

# 4. Filter dropdowns might not have all, let's just use regex to add Story Teller after Comedy option if it exists
$c = $c -replace '(<option value="Comedy">Comedy</option>)', "`$1`r`n                        <option value=`"Story Teller`">Story Teller</option>"

Set-Content -Path $path -Value $c
