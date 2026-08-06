import re

with open('c:/Users/priya/Desktop/youth/Youth/diff_utf8.txt', 'r', encoding='utf-8') as f:
    diff_text = f.read()

# Extract lines that start with '-' and are within the heatmap section
heatmap_lines = []
in_heatmap = False
for line in diff_text.split('\n'):
    if line.startswith('-    <!-- ==================== LIVE EVENT HEATMAP ==================== -->'):
        in_heatmap = True
    if in_heatmap:
        if line.startswith('-'):
            heatmap_lines.append(line[1:]) # remove the '-' prefix
        if line == '-    </script>':
            # End of heatmap section
            break

heatmap_code = '\n'.join(heatmap_lines)

# Now modify heatmap_code to add real-time polling
heatmap_code = heatmap_code.replace(
    'function loadHeatmapData(filter) {\n            var loadingEl = document.getElementById(\'heatmapLoading\');\n            if (loadingEl) loadingEl.style.display = \'flex\';',
    'function loadHeatmapData(filter, showLoading) {\n            var loadingEl = document.getElementById(\'heatmapLoading\');\n            if (showLoading !== false && loadingEl) loadingEl.style.display = \'flex\';'
)

heatmap_code = heatmap_code.replace(
    'initMap();\n                        loadHeatmapData(currentFilter);\n                        observer.unobserve(section);',
    'initMap();\n                        loadHeatmapData(currentFilter);\n                        setInterval(function() { loadHeatmapData(currentFilter, false); }, 10000);\n                        observer.unobserve(section);'
)

heatmap_code = heatmap_code.replace(
    'initMap();\n            loadHeatmapData(currentFilter);',
    'initMap();\n            loadHeatmapData(currentFilter);\n            setInterval(function() { loadHeatmapData(currentFilter, false); }, 10000);'
)

# Read current home.html
with open('c:/Users/priya/Desktop/youth/Youth/src/main/resources/templates/home.html', 'r', encoding='utf-8') as f:
    home_html = f.read()

# Insert before footer
target = '    <!-- Footer -->\n\n    <footer th:replace="~{fragments/template :: footer}"></footer>'
new_home_html = home_html.replace(target, heatmap_code + '\n\n' + target)

with open('c:/Users/priya/Desktop/youth/Youth/src/main/resources/templates/home.html', 'w', encoding='utf-8') as f:
    f.write(new_home_html)

print('Successfully restored heatmap and added 10s real-time polling.')
