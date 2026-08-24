import re
import io

with io.open('src/main/resources/templates/battle-arena.html', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Null Checks
content = content.replace('', '')
content = content.replace('th:src="\ ? \ : \'https://ui-avatars.com/api/?name=\' + \ + \'&background=F84464&color=fff&size=50\'"', 'th:src="\ ? \ : \'https://ui-avatars.com/api/?name=\' + (\ ? \ : \'System\') + \'&background=F84464&color=fff&size=50\'"')
content = content.replace('th:text="\"', 'th:text="\"')
content = content.replace('', '')

# 2. Cleanup specific known corrupted lines that cause parsing failures
content = re.sub(r'<!--\s*A\uFFFD[\s\S]*?(?=<!-- BATTLES LIST VIEW)', '', content)
content = re.sub(r'(Your vote has been recorded!)\s*A\uFFFD[\s\S]*?(?=<\/div>)', r'\1', content)

# Remove any lines containing pure garbage A\uFFFD inside comments or HTML text
content = re.sub(r'A\uFFFD[\uFFFD,A\?\+T\_a-zA-Z\s]*', '', content)

# Remove the broken emojis in winner section
content = content.replace('<div class="winner-trophy" style="font-size: 64px; margin-bottom: 15px;">A,A??</div>', '')
content = content.replace('<div style="font-size: 24px;">A,A?</div>', '')
content = content.replace('<div style="font-size: 24px;">A,A?</div>', '')

with io.open('src/main/resources/templates/battle-arena.html', 'w', encoding='utf-8') as f:
    f.write(content)
print("Done")
