const fs = require('fs');
const diffText = fs.readFileSync('c:/Users/priya/Desktop/youth/Youth/diff_utf8.txt', 'utf-8');

const lines = diffText.split('\n');
let inHeatmap = false;
let heatmapLines = [];

for (const line of lines) {
    if (line.startsWith('-    <!-- ==================== LIVE EVENT HEATMAP ==================== -->')) {
        inHeatmap = true;
    }
    if (inHeatmap) {
        if (line.startsWith('-')) {
            heatmapLines.push(line.substring(1)); // remove '-'
        }
        if (line === '-    </script>') {
            break;
        }
    }
}

let heatmapCode = heatmapLines.join('\n');

// Add real-time polling logic
heatmapCode = heatmapCode.replace(
    'function loadHeatmapData(filter) {\n            var loadingEl = document.getElementById(\'heatmapLoading\');\n            if (loadingEl) loadingEl.style.display = \'flex\';',
    'function loadHeatmapData(filter, showLoading) {\n            var loadingEl = document.getElementById(\'heatmapLoading\');\n            if (showLoading !== false && loadingEl) loadingEl.style.display = \'flex\';'
);

// We should replace both occurrences of loadHeatmapData(currentFilter) where it lacks the false arg
heatmapCode = heatmapCode.replace(
    'initMap();\n                        loadHeatmapData(currentFilter);\n                        observer.unobserve(section);',
    'initMap();\n                        loadHeatmapData(currentFilter);\n                        setInterval(function() { loadHeatmapData(currentFilter, false); }, 10000);\n                        observer.unobserve(section);'
);

heatmapCode = heatmapCode.replace(
    'initMap();\n            loadHeatmapData(currentFilter);',
    'initMap();\n            loadHeatmapData(currentFilter);\n            setInterval(function() { loadHeatmapData(currentFilter, false); }, 10000);'
);

const homeHtmlPath = 'c:/Users/priya/Desktop/youth/Youth/src/main/resources/templates/home.html';
let homeHtml = fs.readFileSync(homeHtmlPath, 'utf-8');

const target = '    <!-- Footer -->\n\n    <footer th:replace="~{fragments/template :: footer}"></footer>';

if (!homeHtml.includes('id="event-heatmap"')) {
    homeHtml = homeHtml.replace(target, heatmapCode + '\n\n' + target);
    fs.writeFileSync(homeHtmlPath, homeHtml, 'utf-8');
    console.log('Successfully restored heatmap and added 10s real-time polling.');
} else {
    console.log('Heatmap is already in home.html');
}
