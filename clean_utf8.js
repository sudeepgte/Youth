const fs = require('fs');
let content = fs.readFileSync('src/main/resources/templates/battle-arena.html', 'utf-8');

// The replacement character is \uFFFD.
// Let's look for any HTML comment that contains \uFFFD and remove the entire comment.
content = content.replace(/<!--[\s\S]*?-->/g, match => {
    if (match.includes('\uFFFD') || match.includes('A')) {
        return '';
    }
    return match;
});

// There is also some stray garbage right before <div class="battle-detail"> or hero
// We'll strip any text between > and < that contains A or \uFFFD
content = content.replace(/>([^<]*\uFFFD[^<]*)</g, '><');
content = content.replace(/>([^<]*A[^<]*)</g, '><');

fs.writeFileSync('src/main/resources/templates/battle-arena.html', content, 'utf-8');
