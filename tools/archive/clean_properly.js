const fs = require('fs');
let content = fs.readFileSync('src/main/resources/templates/battle-arena.html', 'utf-8');

// 1. Remove the huge garbage comment above BATTLES LIST VIEW
content = content.replace(/<!--\s*A\uFFFD[\s\S]*?(?=<!-- BATTLES LIST VIEW)/g, '');

// 2. Remove the garbage appended to "Your vote has been recorded!"
content = content.replace(/(Your vote has been recorded!)\s*A\uFFFD[\s\S]*?(?=<\/div>)/g, '$1');

// 3. Remove garbage around "Hero Banner"
content = content.replace(/\/\*\s*A\uFFFD[\s\S]*?Hero Banner[\s\S]*?\*\//g, '/* Hero Banner */');
content = content.replace(/\/\*\s*A\uFFFD[\s\S]*?Responsive[\s\S]*?\*\//g, '/* Responsive */');

// 4. Remove garbage in JS comments (e.g. "// A... Local File Uploading ...")
content = content.replace(/\/\/\s*A\uFFFD[\s\S]*?Local File Uploading[\s\S]*?(?=\r?\n\s*async)/g, '// Local File Uploading\n');
content = content.replace(/\/\/\s*A\uFFFD[\s\S]*?Modals[\s\S]*?(?=\r?\n\s*function openCreateBattle)/g, '// Modals\n');

// 5. Any remaining HTML comment with A\uFFFD
content = content.replace(/<!--\s*A\uFFFD[\s\S]*?-->/g, '');

// 6. Safe null checks
content = content.replace(/\$\{b\.participants\.size\(\)\}/g, '${b.participants != null ? b.participants.size() : 0}');

content = content.replace(/th:src="\$\{b\.creator\.profilePhotoUrl != null and !b\.creator\.profilePhotoUrl\.isEmpty\(\)\} \? \$\{b\.creator\.profilePhotoUrl\} : 'https:\/\/ui-avatars\.com\/api\/\?name=' \+ \$\{b\.creator\.username\} \+ '&background=F84464&color=fff&size=50'"/g,
    'th:src="${b.creator != null and b.creator.profilePhotoUrl != null and !b.creator.profilePhotoUrl.isEmpty()} ? ${b.creator.profilePhotoUrl} : \'https://ui-avatars.com/api/?name=\' + (${b.creator != null} ? ${b.creator.username} : \'System\') + \'&background=F84464&color=fff&size=50\'"');

content = content.replace(/th:text="\$\{b\.creator\.username\}"/g,
    'th:text="${b.creator != null ? b.creator.username : \'System\'}"');

content = content.replace(/\$\{p\.user\.id == battle\.creator\.id\}/g,
    '${battle.creator != null and p.user.id == battle.creator.id}');

fs.writeFileSync('src/main/resources/templates/battle-arena.html', content, 'utf-8');
console.log("Cleanup done!");
