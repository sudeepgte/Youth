import io

file_path = "src/main/resources/templates/battle-arena.html"
with io.open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Try to remove weird sequences, or just specific lines.
# Easiest way: remove lines containing font-size: 64px if they don't have fa-trophy
lines = content.split("\n")
new_lines = []
for line in lines:
    if 'ðŸ †' in line or 'ðŸ¥‡' in line or 'ðŸ¥ˆ' in line or 'ðŸ¥‰' in line:
        pass # Skip these lines completely
    else:
        # replace others
        line = line.replace('ðŸ—‘ï¸', '')
        line = line.replace('ðŸŽ‰', '')
        line = line.replace('ðŸ—³ï¸', '')
        line = line.replace('ðŸ”´', '<i class="fas fa-circle" style="color:red"></i>')
        line = line.replace('ðŸ‘ ï¸', '<i class="fas fa-eye"></i>')
        line = line.replace('ðŸ¤¼', '<i class="fas fa-fist-raised"></i>')
        # Also catch the weird encoding versions of them if read differently
        new_lines.append(line)

with io.open(file_path, "w", encoding="utf-8") as f:
    f.write("\n".join(new_lines))
