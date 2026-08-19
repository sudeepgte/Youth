const fs = require('fs');
const path = require('path');

function walk(dir) {
    fs.readdirSync(dir).forEach(f => {
        let p = path.join(dir, f);
        if (fs.statSync(p).isDirectory()) {
            walk(p);
        } else if (p.endsWith('.html')) {
            let content = fs.readFileSync(p, 'utf8');
            let modified = false;
            // The string is: loading=" lazy\
            // We want to replace it with: loading="lazy"
            
            if (content.includes('loading=" lazy\\')) {
                console.log('Fixing ' + p);
                content = content.split('loading=" lazy\\').join('loading="lazy"');
                modified = true;
            }
            if (content.includes('loading=" lazy"')) {
                console.log('Fixing ' + p);
                content = content.split('loading=" lazy"').join('loading="lazy"');
                modified = true;
            }
            
            if (modified) {
                fs.writeFileSync(p, content, 'utf8');
            }
        }
    });
}

walk('src/main/resources/templates');
console.log('Done.');
