const fs = require('fs');

const homeHtmlPath = 'c:/Users/priya/Desktop/youth/Youth/src/main/resources/templates/home.html';
let homeHtml = fs.readFileSync(homeHtmlPath, 'utf-8');

const startTag = '        <!-- Leaflet CSS -->';
const endTag = '    </script>';

const startIndex = homeHtml.indexOf(startTag);
if (startIndex !== -1) {
    let endIndex = homeHtml.indexOf(endTag, startIndex);
    if (endIndex !== -1) {
        endIndex += endTag.length;
        
        const googleMapsCode = `
        <!-- Google Maps API -->
        <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyCr_gUF2YzV16dICNphMfnkyjBFurYLKaM&libraries=visualization"></script>

    <script>
    (function() {
        var heatmapMap = null;
        var heatLayer = null;
        var markers = [];
        var infoWindow = null;
        var currentFilter = 'all';
        var contextPath = document.querySelector('meta[name="ctx"]')
            ? document.querySelector('meta[name="ctx"]').content
            : (window.location.pathname.substring(0, window.location.pathname.indexOf('/home')) || '');

        function getApiBase() {
            return contextPath + '/api/heatmap/events';
        }

        window.initGoogleMap = function() {
            if (heatmapMap) return;
            heatmapMap = new google.maps.Map(document.getElementById('heatmapMapContainer'), {
                zoom: 5,
                center: { lat: 20.5937, lng: 78.9629 }, // India center
                mapTypeId: 'roadmap',
                scrollwheel: false,
                streetViewControl: false,
                mapTypeControl: false,
                fullscreenControl: true
            });
            infoWindow = new google.maps.InfoWindow();
        }

        function loadHeatmapData(filter, showLoading) {
            var loadingEl = document.getElementById('heatmapLoading');
            if (showLoading !== false && loadingEl) loadingEl.style.display = 'flex';

            fetch(getApiBase() + '?filter=' + encodeURIComponent(filter))
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    updateStats(data.stats);
                    updateMap(data);
                    if (loadingEl) loadingEl.style.display = 'none';
                })
                .catch(function(err) {
                    console.error('Heatmap fetch error:', err);
                    if (loadingEl) loadingEl.style.display = 'none';
                });
        }

        function updateStats(stats) {
            var el;
            el = document.getElementById('hmStatLive'); if (el) el.textContent = stats.live || 0;
            el = document.getElementById('hmStatToday'); if (el) el.textContent = stats.today || 0;
            el = document.getElementById('hmStatTomorrow'); if (el) el.textContent = stats.tomorrow || 0;
            el = document.getElementById('hmStatWeek'); if (el) el.textContent = stats.week || 0;
            el = document.getElementById('hmStatMonth'); if (el) el.textContent = stats.month || 0;
        }

        function updateMap(data) {
            if (!heatmapMap) return;

            // Clear existing heat layer
            if (heatLayer) {
                heatLayer.setMap(null);
                heatLayer = null;
            }
            
            // Clear existing markers
            markers.forEach(function(m) { m.setMap(null); });
            markers = [];

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

            // Add heat layer
            var heatmapData = data.heatPoints.map(function(p) {
                return { location: new google.maps.LatLng(p[0], p[1]), weight: p[2] };
            });

            heatLayer = new google.maps.visualization.HeatmapLayer({
                data: heatmapData,
                radius: 35,
                maxIntensity: Math.max.apply(null, data.heatPoints.map(function(p) { return p[2]; })) || 1,
                gradient: [
                    'rgba(0, 255, 255, 0)',
                    'rgba(0, 255, 255, 1)',
                    'rgba(0, 191, 255, 1)',
                    'rgba(0, 127, 255, 1)',
                    'rgba(0, 63, 255, 1)',
                    'rgba(0, 0, 255, 1)',
                    'rgba(0, 0, 223, 1)',
                    'rgba(0, 0, 191, 1)',
                    'rgba(0, 0, 159, 1)',
                    'rgba(0, 0, 127, 1)',
                    'rgba(63, 0, 91, 1)',
                    'rgba(127, 0, 63, 1)',
                    'rgba(191, 0, 31, 1)',
                    'rgba(255, 0, 0, 1)'
                ]
            });
            heatLayer.setMap(heatmapMap);

            // Add markers
            var bounds = new google.maps.LatLngBounds();
            data.markers.forEach(function(m) {
                var pos = new google.maps.LatLng(m.lat, m.lng);
                bounds.extend(pos);

                var marker = new google.maps.Marker({
                    position: pos,
                    map: heatmapMap,
                    title: m.location,
                    label: {
                        text: String(m.total),
                        color: 'white',
                        fontWeight: 'bold'
                    }
                });

                var popupHtml = '<div class="heatmap-popup" style="padding: 5px; color: black;">' +
                    '<h4 style="margin: 0;"><i class="fa-solid fa-location-dot"></i> ' + escapeHtml(m.location) + '</h4>' +
                    '</div>';

                marker.addListener('click', function() {
                    infoWindow.setContent(popupHtml);
                    infoWindow.open(heatmapMap, marker);
                    heatmapMap.panTo(pos);
                    heatmapMap.setZoom(17);
                });

                markers.push(marker);
            });

            // Fit bounds if markers exist
            if (markers.length > 0) {
                if (markers.length === 1) {
                    heatmapMap.setCenter(markers[0].getPosition());
                    heatmapMap.setZoom(12);
                } else {
                    heatmapMap.fitBounds(bounds);
                }
            }
        }

        function escapeHtml(text) {
            var div = document.createElement('div');
            div.appendChild(document.createTextNode(text));
            return div.innerHTML;
        }

        // Filter button handlers
        var filtersContainer = document.getElementById('heatmapFilters');
        if (filtersContainer) {
            filtersContainer.addEventListener('click', function(e) {
                var btn = e.target.closest('.heatmap-filter-btn');
                if (!btn) return;
                filtersContainer.querySelectorAll('.heatmap-filter-btn').forEach(function(b) { b.classList.remove('active'); });
                btn.classList.add('active');
                currentFilter = btn.getAttribute('data-filter');
                loadHeatmapData(currentFilter);
            });
        }

        // IntersectionObserver: lazy-init map when section becomes visible
        var section = document.getElementById('event-heatmap');
        if (section && 'IntersectionObserver' in window) {
            var observer = new IntersectionObserver(function(entries) {
                entries.forEach(function(entry) {
                    if (entry.isIntersecting) {
                        initGoogleMap();
                        loadHeatmapData(currentFilter);
                        setInterval(function() { loadHeatmapData(currentFilter, false); }, 30000);
                        observer.unobserve(section);
                    }
                });
            }, { rootMargin: '200px' });
            observer.observe(section);
        } else if (section) {
            // Fallback
            initGoogleMap();
            loadHeatmapData(currentFilter);
            setInterval(function() { loadHeatmapData(currentFilter, false); }, 30000);
        }
    })();
    </script>`;
        
        homeHtml = homeHtml.substring(0, startIndex) + googleMapsCode + homeHtml.substring(endIndex);
        fs.writeFileSync(homeHtmlPath, homeHtml, 'utf-8');
        console.log('Successfully updated heatmap to use Google Maps API.');
    } else {
        console.log('End tag not found.');
    }
} else {
    console.log('Start tag not found.');
}
