const fs = require('fs');

const homeHtmlPath = 'c:/Users/priya/Desktop/youth/Youth/src/main/resources/templates/home.html';
let homeHtml = fs.readFileSync(homeHtmlPath, 'utf-8');

// Find the start and end of the heatmap section
const startTag = '    <!-- ==================== LIVE EVENT HEATMAP ==================== -->';
const endTag = '    </script>';

const startIndex = homeHtml.indexOf(startTag);
if (startIndex !== -1) {
    let endIndex = homeHtml.indexOf(endTag, startIndex);
    if (endIndex !== -1) {
        endIndex += endTag.length;
        
        let heatmapSection = homeHtml.substring(startIndex, endIndex);
        
        // 1. Initially hide the loading spinner to prevent it getting stuck if JS fails
        heatmapSection = heatmapSection.replace(
            '<div class="heatmap-loading" id="heatmapLoading">',
            '<div class="heatmap-loading" id="heatmapLoading" style="display: none;">'
        );
        
        // 2. Add an "Empty Data" overlay handling in updateMap
        const updateMapTarget = 'if (!data.heatPoints || data.heatPoints.length === 0) {\n                return;\n            }';
        const updateMapReplacement = `
            var emptyMsg = document.getElementById('heatmapEmptyMsg');
            if (!emptyMsg) {
                emptyMsg = document.createElement('div');
                emptyMsg.id = 'heatmapEmptyMsg';
                emptyMsg.style.position = 'absolute';
                emptyMsg.style.top = '50%';
                emptyMsg.style.left = '50%';
                emptyMsg.style.transform = 'translate(-50%, -50%)';
                emptyMsg.style.background = 'rgba(255,255,255,0.95)';
                emptyMsg.style.padding = '12px 24px';
                emptyMsg.style.borderRadius = '24px';
                emptyMsg.style.boxShadow = '0 10px 25px rgba(0,0,0,0.1)';
                emptyMsg.style.fontWeight = '700';
                emptyMsg.style.color = '#0f172a';
                emptyMsg.style.zIndex = '1000';
                emptyMsg.style.pointerEvents = 'none';
                emptyMsg.innerHTML = '<i class="fa-solid fa-circle-info" style="color:#3b82f6;margin-right:8px;"></i> No active events with coordinates found.';
                document.getElementById('heatmapMapContainer').parentElement.appendChild(emptyMsg);
            }
            
            if (!data.heatPoints || data.heatPoints.length === 0) {
                emptyMsg.style.display = 'block';
                return;
            } else {
                emptyMsg.style.display = 'none';
            }
        `;
        heatmapSection = heatmapSection.replace(updateMapTarget, updateMapReplacement);

        // 3. Relax the polling interval to 30 seconds to allow users to read popups
        heatmapSection = heatmapSection.replace(/10000/g, '30000');

        homeHtml = homeHtml.substring(0, startIndex) + heatmapSection + homeHtml.substring(endIndex);
        fs.writeFileSync(homeHtmlPath, homeHtml, 'utf-8');
        console.log('Heatmap updated with empty state overlay and relaxed polling.');
    }
} else {
    console.log('Heatmap section not found.');
}
