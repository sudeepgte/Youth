const fs = require('fs');
let content = fs.readFileSync('src/main/resources/templates/battle-arena.html', 'utf-8');

// Replace participants.size() with a safe null check
content = content.replace(/\$\{b\.participants\.size\(\)\}/g, '${b.participants != null ? b.participants.size() : 0}');

// Replace b.creator with safe null checks
content = content.replace(/th:src="\$\{b\.creator\.profilePhotoUrl != null and !b\.creator\.profilePhotoUrl\.isEmpty\(\)\} \? \$\{b\.creator\.profilePhotoUrl\} : 'https:\/\/ui-avatars\.com\/api\/\?name=' \+ \$\{b\.creator\.username\} \+ '&background=F84464&color=fff&size=50'"/g,
    'th:src="${b.creator != null and b.creator.profilePhotoUrl != null and !b.creator.profilePhotoUrl.isEmpty()} ? ${b.creator.profilePhotoUrl} : \'https://ui-avatars.com/api/?name=\' + (${b.creator != null} ? ${b.creator.username} : \'System\') + \'&background=F84464&color=fff&size=50\'"');

content = content.replace(/th:text="\$\{b\.creator\.username\}"/g,
    'th:text="${b.creator != null ? b.creator.username : \'System\'}"');

content = content.replace(/\$\{p\.user\.id == battle\.creator\.id\}/g,
    '${battle.creator != null and p.user.id == battle.creator.id}');

fs.writeFileSync('src/main/resources/templates/battle-arena.html', content, 'utf-8');
