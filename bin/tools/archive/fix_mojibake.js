const fs = require('fs');
const path = require('path');

const replacements = {
    'ðŸ—³ï¸ ': '&#128499;',
    'â€œ': '&ldquo;',
    'â€ ': '&rdquo;',
    'â€º': '&rsaquo;',
    'â€”': '&mdash;',
    'â€¢': '&bull;',
    'Ã¢â‚¬Â¢': '&bull;',
    'Ãƒâ€”': '&times;',
    'â†’': '&rarr;',
    'âœ•': '&times;',
    'â ¤ï¸ ': '&#10084;&#65039;',
    'ðŸ˜‚': '&#128514;',
    'ðŸ”¥': '&#128293;',
    'ðŸ˜ ': '&#128525;',
    'ðŸ‘ ': '&#128079;',
    'ðŸ™Œ': '&#128588;',
    'ðŸ˜¢': '&#128546;',
    'ðŸ˜®': '&#128558;',
    'ðŸŽ‰': '&#127881;',
    'ðŸ’¯': '&#128175;',
    'ðŸ˜€': '&#128512;',
    'ðŸ ±': '&#128049;',
    'ðŸ •': '&#127829;',
    'âš½': '&#9917;',
    'âœ‰ï¸ ': '&#9993;&#65039;',
    'â”€': '-'
};

function processDir(dir) {
    fs.readdirSync(dir).forEach(file => {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            processDir(fullPath);
        } else if (fullPath.endsWith('.html')) {
            let content = fs.readFileSync(fullPath, 'utf8');
            let modified = false;
            for (const [key, value] of Object.entries(replacements)) {
                if (content.includes(key)) {
                    content = content.split(key).join(value);
                    modified = true;
                }
            }
            if (modified) {
                fs.writeFileSync(fullPath, content, 'utf8');
                console.log('Fixed ' + file);
            }
        }
    });
}

processDir(path.join(__dirname, 'src', 'main', 'resources', 'templates'));
