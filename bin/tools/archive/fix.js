const fs = require('fs');
const paths = ['src/main/resources/templates/battle-live.html', 'target/classes/templates/battle-live.html'];

paths.forEach(p => {
  if (fs.existsSync(p)) {
    let c = fs.readFileSync(p, 'utf8');
    c = c.replace(/var hearts = \['[^\]]+\];/g, "var hearts = ['❤️', '💙', '💜', '💛', '💚'];");
    c = c.replace(/var giftEmojis = \{[^}]+};/g, "var giftEmojis = { ROSE: '🌹', FIRE: '🔥', GIFT_BOX: '🎁', DIAMOND: '💎', CROWN: '👑' };");
    c = c.replace(/<span class="gift-emoji">[^<]+<\/span>\s*<span class="gift-name">Rose<\/span>/g, '<span class="gift-emoji">🌹</span>\n            <span class="gift-name">Rose</span>');
    c = c.replace(/<span class="gift-emoji">[^<]+<\/span>\s*<span class="gift-name">Fire<\/span>/g, '<span class="gift-emoji">🔥</span>\n            <span class="gift-name">Fire</span>');
    c = c.replace(/<span class="gift-emoji">[^<]+<\/span>\s*<span class="gift-name">Gift Box<\/span>/g, '<span class="gift-emoji">🎁</span>\n            <span class="gift-name">Gift Box</span>');
    c = c.replace(/<span class="gift-emoji">[^<]+<\/span>\s*<span class="gift-name">Diamond<\/span>/g, '<span class="gift-emoji">💎</span>\n            <span class="gift-name">Diamond</span>');
    c = c.replace(/<span class="gift-emoji">[^<]+<\/span>\s*<span class="gift-name">Crown<\/span>/g, '<span class="gift-emoji">👑</span>\n            <span class="gift-name">Crown</span>');
    fs.writeFileSync(p, c, 'utf8');
    console.log('Fixed', p);
  }
});
