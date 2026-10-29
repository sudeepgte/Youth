$content = Get-Content 'src\main\resources\templates\home.html' -Raw

$carouselFragment = @"
    <!-- ADVERTISEMENT FRAGMENT START -->
    <th:block th:fragment="adCarousel(adsList, placementId)">
        <div th:if="`${adsList != null and !adsList.isEmpty()}`" class="ad-carousel-container" th:data-placement="`${placementId}`" style="max-width: 1000px; margin: 40px auto; position: relative; overflow: hidden; border-radius: 20px; box-shadow: 0 15px 35px rgba(0,0,0,0.05); border: 1px solid var(--border-color); background: var(--bg-card);">
            <!-- Sponsored Badge -->
            <div style="position: absolute; top: 15px; right: 15px; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); color: white; font-size: 10px; font-weight: 800; padding: 4px 10px; border-radius: 20px; z-index: 10; letter-spacing: 1px;">SPONSORED</div>
            
            <div class="ad-carousel-track" style="display: flex; transition: transform 0.5s ease-in-out; width: 100%;">
                <div th:each="ad, stat : `${adsList}`" class="ad-slide" th:data-ad-id="`${ad.id}`" style="min-width: 100%; display: flex; flex-direction: row; flex-wrap: wrap;">
                    <!-- Banner -->
                    <div style="flex: 1 1 50%; min-width: 300px; aspect-ratio: 16/9; background: #f1f5f9; position: relative; overflow: hidden;">
                        <img th:src="`${ad.imageUrl}`" style="width: 100%; height: 100%; object-fit: cover; position: absolute; top: 0; left: 0;">
                    </div>
                    
                    <div style="flex: 1 1 50%; min-width: 300px; padding: 40px; display: flex; flex-direction: column; justify-content: center;">
                        <div style="font-size: 13px; font-weight: 800; color: var(--brand-primary); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px;" th:text="`${ad.organizationName}`">Organization Name</div>
                        <h3 style="font-size: 28px; font-weight: 900; color: var(--text-primary); margin: 0 0 15px 0; line-height: 1.3;" th:text="`${ad.title}`">Advertisement Title</h3>
                        <p style="font-size: 16px; color: var(--text-secondary); line-height: 1.6; margin: 0 0 30px 0; flex: 1;" th:text="`${ad.description}`">Description</p>
                        
                        <div style="display: flex; gap: 15px; align-items: center; margin-top: auto;">
                            <a th:href="@{'/api/advertisements/' + `${ad.id}` + '/click'}" target="_blank" class="ad-cta-btn" style="flex: 1; text-align: center; background: linear-gradient(135deg, #2563eb, #0ea5e9); color: white; padding: 14px 24px; border-radius: 12px; font-weight: 800; font-size: 16px; text-decoration: none; box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3); transition: transform 0.2s;"><span th:text="`${ad.ctaText}`">Register Now</span> <i class="fas fa-arrow-right" style="margin-left: 6px;"></i></a>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Dots -->
            <div th:if="`${adsList.size() > 1}`" class="ad-carousel-dots" style="position: absolute; bottom: 20px; left: 0; right: 0; display: flex; justify-content: center; gap: 8px; z-index: 10;">
                <div th:each="ad, stat : `${adsList}`" th:class="`${stat.index == 0 ? 'ad-dot active' : 'ad-dot'}`" th:data-index="`${stat.index}`" style="width: 8px; height: 8px; border-radius: 50%; background: rgba(0,0,0,0.2); cursor: pointer; transition: background 0.3s;"></div>
            </div>
        </div>
    </th:block>
    <!-- ADVERTISEMENT FRAGMENT END -->
"@

$carouselFragment = $carouselFragment.Replace('`$', '$')

$jsScript = @"
    <!-- ADVERTISEMENT SCRIPTS START -->
    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const impressionTracker = new IntersectionObserver((entries, observer) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const adSlide = entry.target;
                        const adId = adSlide.getAttribute('data-ad-id');
                        if (adId && !adSlide.dataset.impressionRecorded) {
                            adSlide.dataset.impressionRecorded = 'true';
                            fetch('/api/advertisements/' + adId + '/impression', { method: 'POST' }).catch(e => console.error(e));
                        }
                    }
                });
            }, { threshold: 0.5 });

            document.querySelectorAll('.ad-carousel-container').forEach(container => {
                const track = container.querySelector('.ad-carousel-track');
                const slides = container.querySelectorAll('.ad-slide');
                const dots = container.querySelectorAll('.ad-dot');
                if (!track || slides.length === 0) return;

                let currentIndex = 0;
                const slideCount = slides.length;

                impressionTracker.observe(slides[0]);

                if (slideCount > 1) {
                    const updateCarousel = (index) => {
                        track.style.transform = 'translateX(-' + (index * 100) + '%)';
                        dots.forEach((dot, i) => {
                            dot.style.background = i === index ? 'var(--brand-primary)' : 'rgba(0,0,0,0.2)';
                        });
                        impressionTracker.observe(slides[index]);
                    };

                    dots.forEach((dot, i) => {
                        dot.addEventListener('click', () => {
                            currentIndex = i;
                            updateCarousel(currentIndex);
                        });
                    });

                    let autoRotate = setInterval(() => {
                        currentIndex = (currentIndex + 1) % slideCount;
                        updateCarousel(currentIndex);
                    }, 6000);

                    container.addEventListener('mouseenter', () => clearInterval(autoRotate));
                    container.addEventListener('mouseleave', () => {
                        autoRotate = setInterval(() => {
                            currentIndex = (currentIndex + 1) % slideCount;
                            updateCarousel(currentIndex);
                        }, 6000);
                    });
                }
            });
        });
    </script>
    <!-- ADVERTISEMENT SCRIPTS END -->
"@

$content = $content -replace '</body>', "`n$carouselFragment`n$jsScript`n</body>"

# For placements, we'll replace specific strings.
# HERO placement
$content = $content -replace '<section class="container">', "`n    <div th:replace=`"~{this :: adCarousel(`$adsHero, 'HERO')}`"></div>`n    <section class=`"container`">"

# FEATURED placement
$content = $content -replace '<section class="container" id="featured-events">', "`n    <div th:replace=`"~{this :: adCarousel(`$adsFeatured, 'FEATURED')}`"></div>`n    <section class=`"container`" id=`"featured-events`">"

# BETWEEN_SECTIONS
$content = $content -replace '<section class="container" id="organizers"', "`n    <div th:replace=`"~{this :: adCarousel(`$adsBetween, 'BETWEEN_SECTIONS')}`"></div>`n    <section class=`"container`" id=`"organizers`""

# BOTTOM_CTA
$content = $content -replace '<footer', "`n    <div th:replace=`"~{this :: adCarousel(`$adsBottom, 'BOTTOM_CTA')}`"></div>`n    <footer"

Set-Content -Path 'src\main\resources\templates\home.html' -Value $content
