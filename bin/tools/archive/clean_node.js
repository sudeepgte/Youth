const fs = require('fs');
let content = fs.readFileSync('src/main/resources/templates/battle-arena.html', 'utf-8');

// 1. Null Checks
content = content.replace(/\$\{b\.participants\.size\(\)\}/g, '${b.participants != null ? b.participants.size() : 0}');

content = content.replace(/th:src="\$\{b\.creator\.profilePhotoUrl != null and !b\.creator\.profilePhotoUrl\.isEmpty\(\)\} \? \$\{b\.creator\.profilePhotoUrl\} : 'https:\/\/ui-avatars\.com\/api\/\?name=' \+ \$\{b\.creator\.username\} \+ '&background=F84464&color=fff&size=50'"/g,
    'th:src="${b.creator != null and b.creator.profilePhotoUrl != null and !b.creator.profilePhotoUrl.isEmpty()} ? ${b.creator.profilePhotoUrl} : \'https://ui-avatars.com/api/?name=\' + (${b.creator != null} ? ${b.creator.username} : \'System\') + \'&background=F84464&color=fff&size=50\'"');

content = content.replace(/th:text="\$\{b\.creator\.username\}"/g,
    'th:text="${b.creator != null ? b.creator.username : \'System\'}"');

content = content.replace(/\$\{p\.user\.id == battle\.creator\.id\}/g,
    '${battle.creator != null and p.user.id == battle.creator.id}');

// 2. Delete the huge blocks of corruption.
// In UTF-8, the \xEF\xBF\xBD character is the replacement character.
content = content.replace(/<!--\s*A\uFFFD[\s\S]*?(?=<!-- BATTLES LIST VIEW)/g, '');
content = content.replace(/(Your vote has been recorded!)\s*A\uFFFD[\s\S]*?(?=<\/div>)/g, '$1');

// Match A followed by replacement chars
content = content.replace(/A\uFFFD[\uFFFD,A\?\+T\_a-zA-Z\s]+/g, '');

fs.writeFileSync('src/main/resources/templates/battle-arena.html', content, 'utf-8');
console.log("Cleanup done");
