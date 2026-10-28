const fs = require('fs');
let content = fs.readFileSync('src/main/resources/templates/battle-arena.html', 'utf-8');

// Remove all HTML comments completely EXCEPT those containing standard text
content = content.replace(/<!--[\s\S]*?-->/g, match => {
    // If the comment has a lot of non-ascii or repeated A patterns, delete it
    if (match.includes('A') || match.includes('A,A') || match.includes('A?A') || match.match(/A[\?,+T]{2,}/)) {
        return '';
    }
    return match;
});

// Also fix inline garbage in Hero Banner
content = content.replace(/<h1>.*?Battle Arena<\/h1>/, '<h1>Battle Arena</h1>');
content = content.replace(/Your vote has been recorded!.*?<\/div>/s, 'Your vote has been recorded!</div>');

fs.writeFileSync('src/main/resources/templates/battle-arena.html', content, 'utf-8');
