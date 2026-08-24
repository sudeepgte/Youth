import re

with open('src/main/resources/templates/battle-arena.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove all HTML comments that contain strange repeated characters
content = re.sub(r'<!--[ \n]*A[,A\?\+T\.\_a-zA-Z\s]+-->', '', content)
content = re.sub(r'<!--\s*(A.*?|A\?\?.*?|A,A.*?)\s*-->', '', content, flags=re.DOTALL)

# Also remove inline garbage text if it's sitting as a direct text node
# Looking for 'A' followed by  or ? or , repeated
content = re.sub(r'A[\?\,\+T]{2,}\w*', '', content)

with open('src/main/resources/templates/battle-arena.html', 'w', encoding='utf-8') as f:
    f.write(content)
