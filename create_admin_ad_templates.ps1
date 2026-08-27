$content = Get-Content 'src\main\resources\templates\admin-dashboard.html' -Raw

# Prepare templates content
$adsListMain = @"
<main class="social-feed" style="max-width: 100%; padding-bottom: 60px; padding: 20px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <h2 style="font-size: 24px; font-weight: 800; margin: 0; color: var(--text-primary);">Advertisement Management</h2>
        <a th:href="@{/admin/advertisements/new}" class="btn" style="background: var(--brand-primary); color: white; padding: 8px 16px; border-radius: 8px; text-decoration: none; font-weight: bold;"><i class="fas fa-plus"></i> Create Ad</a>
    </div>
    
    <div th:if="${successMessage}" style="background: rgba(16,185,129,0.1); color: #10B981; padding: 12px 20px; border-radius: 8px; margin-bottom: 20px; font-weight: 600;" th:text="${successMessage}"></div>
    <div th:if="${errorMessage}" style="background: rgba(239,68,68,0.1); color: #EF4444; padding: 12px 20px; border-radius: 8px; margin-bottom: 20px; font-weight: 600;" th:text="${errorMessage}"></div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin-bottom: 25px;">
        <div class="stat-tile" style="background: var(--bg-card); padding: 15px; border-radius: 12px; border: 1px solid var(--border-color); text-align: center;">
            <div style="font-size: 14px; color: var(--text-muted); font-weight: bold;">Total Ads</div>
            <div style="font-size: 24px; font-weight: 900; color: var(--text-primary);" th:text="${totalAds}">0</div>
        </div>
        <div class="stat-tile" style="background: var(--bg-card); padding: 15px; border-radius: 12px; border: 1px solid var(--border-color); text-align: center;">
            <div style="font-size: 14px; color: #10B981; font-weight: bold;">Active</div>
            <div style="font-size: 24px; font-weight: 900; color: #10B981;" th:text="${activeAds}">0</div>
        </div>
        <div class="stat-tile" style="background: var(--bg-card); padding: 15px; border-radius: 12px; border: 1px solid var(--border-color); text-align: center;">
            <div style="font-size: 14px; color: #F59E0B; font-weight: bold;">Scheduled</div>
            <div style="font-size: 24px; font-weight: 900; color: #F59E0B;" th:text="${scheduledAds}">0</div>
        </div>
        <div class="stat-tile" style="background: var(--bg-card); padding: 15px; border-radius: 12px; border: 1px solid var(--border-color); text-align: center;">
            <div style="font-size: 14px; color: var(--brand-primary); font-weight: bold;">Clicks</div>
            <div style="font-size: 24px; font-weight: 900; color: var(--brand-primary);" th:text="${totalClicks}">0</div>
        </div>
        <div class="stat-tile" style="background: var(--bg-card); padding: 15px; border-radius: 12px; border: 1px solid var(--border-color); text-align: center;">
            <div style="font-size: 14px; color: #8B5CF6; font-weight: bold;">CTR</div>
            <div style="font-size: 24px; font-weight: 900; color: #8B5CF6;"><span th:text="${ctr}">0</span>%</div>
        </div>
    </div>

    <div style="background: var(--bg-card); border-radius: 12px; border: 1px solid var(--border-color); overflow: hidden;">
        <table style="width: 100%; border-collapse: collapse; text-align: left;">
            <thead>
                <tr style="background: rgba(0,0,0,0.02); border-bottom: 1px solid var(--border-color);">
                    <th style="padding: 15px; font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase;">Image</th>
                    <th style="padding: 15px; font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase;">Advertisement</th>
                    <th style="padding: 15px; font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase;">Placement</th>
                    <th style="padding: 15px; font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase;">Status</th>
                    <th style="padding: 15px; font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase;">Clicks / Imps</th>
                    <th style="padding: 15px; font-size: 13px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; text-align: right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="ad : ${ads}" style="border-bottom: 1px solid var(--border-color);">
                    <td style="padding: 15px;">
                        <img th:if="${ad.imageUrl}" th:src="${ad.imageUrl}" style="width: 60px; height: 40px; border-radius: 4px; object-fit: cover;">
                        <div th:unless="${ad.imageUrl}" style="width: 60px; height: 40px; border-radius: 4px; background: rgba(0,0,0,0.05); display: flex; align-items: center; justify-content: center; color: var(--text-muted);"><i class="fas fa-image"></i></div>
                    </td>
                    <td style="padding: 15px;">
                        <div style="font-weight: 800; color: var(--text-primary);" th:text="${ad.title}">Title</div>
                        <div style="font-size: 13px; color: var(--text-secondary);" th:text="${ad.organizationName}">Org</div>
                    </td>
                    <td style="padding: 15px;">
                        <div style="font-size: 13px; font-weight: 600; color: var(--brand-primary);" th:text="${ad.placement}">HERO</div>
                    </td>
                    <td style="padding: 15px;">
                        <span th:if="${ad.status.name() == 'ACTIVE'}" style="background: rgba(16,185,129,0.1); color: #10B981; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold;">ACTIVE</span>
                        <span th:if="${ad.status.name() == 'SCHEDULED'}" style="background: rgba(245,158,11,0.1); color: #F59E0B; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold;">SCHEDULED</span>
                        <span th:if="${ad.status.name() == 'PAUSED'}" style="background: rgba(107,114,128,0.1); color: #6B7280; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold;">PAUSED</span>
                        <span th:if="${ad.status.name() == 'EXPIRED'}" style="background: rgba(239,68,68,0.1); color: #EF4444; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold;">EXPIRED</span>
                        <span th:if="${ad.status.name() == 'DRAFT'}" style="background: rgba(0,0,0,0.05); color: var(--text-secondary); padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold;">DRAFT</span>
                    </td>
                    <td style="padding: 15px; font-size: 14px; font-weight: 600; color: var(--text-primary);">
                        <span th:text="${ad.clickCount}">0</span> / <span th:text="${ad.impressionCount}">0</span>
                    </td>
                    <td style="padding: 15px; text-align: right; display: flex; gap: 8px; justify-content: flex-end;">
                        <a th:href="@{'/admin/advertisements/' + ${ad.id}}" class="btn" style="padding: 6px 12px; background: rgba(0,0,0,0.05); border-radius: 6px; color: var(--text-primary); text-decoration: none; font-size: 13px; font-weight: 600;"><i class="fas fa-chart-bar"></i></a>
                        <a th:href="@{'/admin/advertisements/' + ${ad.id} + '/edit'}" class="btn" style="padding: 6px 12px; background: rgba(14,165,233,0.1); border-radius: 6px; color: #0ea5e9; text-decoration: none; font-size: 13px; font-weight: 600;"><i class="fas fa-edit"></i></a>
                        <form th:action="@{'/admin/advertisements/' + ${ad.id} + '/delete'}" method="post" style="margin: 0;" onsubmit="return confirm('Are you sure you want to delete this ad?');">
                            <button type="submit" class="btn" style="padding: 6px 12px; background: rgba(239,68,68,0.1); border-radius: 6px; color: #ef4444; border: none; cursor: pointer; font-size: 13px; font-weight: 600;"><i class="fas fa-trash"></i></button>
                        </form>
                    </td>
                </tr>
                <tr th:if="${ads.isEmpty()}">
                    <td colspan="6" style="padding: 40px; text-align: center; color: var(--text-muted);">
                        <i class="fas fa-ad" style="font-size: 48px; margin-bottom: 15px; opacity: 0.2;"></i>
                        <div style="font-size: 16px; font-weight: 600;">No advertisements found.</div>
                        <div style="font-size: 14px; margin-top: 5px;">Create your first ad to start monetizing.</div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</main>
"@

$adsFormMain = @"
<main class="social-feed" style="max-width: 100%; padding-bottom: 60px; padding: 20px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <h2 style="font-size: 24px; font-weight: 800; margin: 0; color: var(--text-primary);" th:text="${ad.id == null ? 'Create Advertisement' : 'Edit Advertisement'}">Create Ad</h2>
        <a th:href="@{/admin/advertisements}" class="btn" style="background: rgba(0,0,0,0.05); color: var(--text-primary); padding: 8px 16px; border-radius: 8px; text-decoration: none; font-weight: bold;"><i class="fas fa-arrow-left"></i> Back</a>
    </div>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 30px;">
        <!-- Form -->
        <div style="background: var(--bg-card); padding: 25px; border-radius: 16px; border: 1px solid var(--border-color);">
            <form th:action="@{/admin/advertisements}" method="post" th:object="${ad}">
                <input type="hidden" th:field="*{id}">
                
                <div style="margin-bottom: 20px;">
                    <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Ad Title</label>
                    <input type="text" th:field="*{title}" id="prev-title-input" required style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                </div>
                
                <div style="margin-bottom: 20px;">
                    <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Organization Name</label>
                    <input type="text" th:field="*{organizationName}" id="prev-org-input" required style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <div>
                        <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Ad Type</label>
                        <select th:field="*{adType}" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                            <option th:each="type : ${adTypes}" th:value="${type}" th:text="${type}">Type</option>
                        </select>
                    </div>
                    <div>
                        <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Placement</label>
                        <select th:field="*{placement}" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                            <option th:each="pl : ${placements}" th:value="${pl}" th:text="${pl}">Pl</option>
                        </select>
                    </div>
                </div>

                <div style="margin-bottom: 20px;">
                    <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Description</label>
                    <textarea th:field="*{description}" id="prev-desc-input" required rows="3" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit; resize: vertical;"></textarea>
                </div>

                <div style="margin-bottom: 20px;">
                    <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Image URL (16:9 Recommended)</label>
                    <input type="url" th:field="*{imageUrl}" id="prev-img-input" required style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <div>
                        <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">CTA Button Text</label>
                        <input type="text" th:field="*{ctaText}" id="prev-cta-input" required style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                    </div>
                    <div>
                        <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">CTA URL</label>
                        <input type="url" th:field="*{ctaUrl}" required style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <div>
                        <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Target Audience</label>
                        <select th:field="*{targetAudience}" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                            <option th:each="ta : ${audiences}" th:value="${ta}" th:text="${ta}">TA</option>
                        </select>
                    </div>
                    <div>
                        <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Priority (Higher = First)</label>
                        <input type="number" th:field="*{priority}" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                    </div>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <div>
                        <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Start Date & Time</label>
                        <input type="datetime-local" name="startDateStr" th:value="${ad.startDateTime != null ? #temporals.format(ad.startDateTime, 'yyyy-MM-dd''T''HH:mm') : ''}" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                    </div>
                    <div>
                        <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">End Date & Time</label>
                        <input type="datetime-local" name="endDateStr" th:value="${ad.endDateTime != null ? #temporals.format(ad.endDateTime, 'yyyy-MM-dd''T''HH:mm') : ''}" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                    </div>
                </div>

                <div style="margin-bottom: 30px;">
                    <label style="display: block; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">Initial Status</label>
                    <select th:field="*{status}" style="width: 100%; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-family: inherit;">
                        <option th:each="st : ${statuses}" th:value="${st}" th:text="${st}">St</option>
                    </select>
                    <p style="font-size: 12px; color: var(--text-secondary); margin-top: 8px;">Status will automatically change to ACTIVE / EXPIRED based on dates.</p>
                </div>

                <button type="submit" class="btn" style="width: 100%; padding: 14px; background: var(--brand-primary); color: white; border: none; border-radius: 8px; font-size: 16px; font-weight: bold; cursor: pointer;">Save Advertisement</button>
            </form>
        </div>

        <!-- Live Preview -->
        <div>
            <div style="position: sticky; top: 20px;">
                <h3 style="font-size: 16px; font-weight: 800; color: var(--text-muted); margin-bottom: 15px; text-transform: uppercase;">Live Preview</h3>
                
                <!-- Advertisement Card Template -->
                <div style="background: var(--bg-card); border-radius: 20px; overflow: hidden; border: 1px solid var(--border-color); box-shadow: 0 10px 25px rgba(0,0,0,0.05); position: relative; margin-bottom: 20px;">
                    <!-- Sponsored Badge -->
                    <div style="position: absolute; top: 15px; right: 15px; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); color: white; font-size: 10px; font-weight: 800; padding: 4px 10px; border-radius: 20px; z-index: 2; letter-spacing: 1px;">SPONSORED</div>
                    
                    <!-- Banner -->
                    <div style="width: 100%; aspect-ratio: 16/9; background: #f1f5f9; position: relative;">
                        <img id="prev-img-display" src="https://via.placeholder.com/800x450?text=Advertisement+Banner" style="width: 100%; height: 100%; object-fit: cover; position: absolute; top: 0; left: 0;">
                    </div>
                    
                    <div style="padding: 25px;">
                        <div id="prev-org-display" style="font-size: 12px; font-weight: 800; color: var(--brand-primary); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px;">Organization Name</div>
                        <h3 id="prev-title-display" style="font-size: 22px; font-weight: 900; color: var(--text-primary); margin: 0 0 12px 0; line-height: 1.3;">Advertisement Title</h3>
                        <p id="prev-desc-display" style="font-size: 15px; color: var(--text-secondary); line-height: 1.6; margin: 0 0 20px 0;">This is where your compelling advertisement description will appear. It supports multiple lines of text.</p>
                        
                        <div style="display: flex; gap: 10px; align-items: center;">
                            <a href="#" id="prev-cta-display" style="flex: 1; text-align: center; background: linear-gradient(135deg, #2563eb, #0ea5e9); color: white; padding: 12px 20px; border-radius: 12px; font-weight: 800; text-decoration: none; box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);">Register Now <i class="fas fa-arrow-right" style="margin-left: 6px;"></i></a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<script>
    // Live Preview Logic
    function setupLivePreview(inputId, displayId, defaultVal) {
        const input = document.getElementById(inputId);
        const display = document.getElementById(displayId);
        
        const update = () => {
            if(input.tagName === 'INPUT' && input.type === 'url' && inputId === 'prev-img-input') {
                display.src = input.value || defaultVal;
            } else {
                display.textContent = input.value || defaultVal;
            }
        };
        
        input.addEventListener('input', update);
        update(); // init
    }
    
    document.addEventListener('DOMContentLoaded', () => {
        setupLivePreview('prev-title-input', 'prev-title-display', 'Advertisement Title');
        setupLivePreview('prev-org-input', 'prev-org-display', 'Organization Name');
        setupLivePreview('prev-desc-input', 'prev-desc-display', 'This is where your compelling advertisement description will appear. It supports multiple lines of text.');
        setupLivePreview('prev-cta-input', 'prev-cta-display', 'Register Now');
        setupLivePreview('prev-img-input', 'prev-img-display', 'https://via.placeholder.com/800x450?text=Advertisement+Banner');
    });
</script>
"@

$adsDetailsMain = @"
<main class="social-feed" style="max-width: 100%; padding-bottom: 60px; padding: 20px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <h2 style="font-size: 24px; font-weight: 800; margin: 0; color: var(--text-primary);">Advertisement Analytics</h2>
        <a th:href="@{/admin/advertisements}" class="btn" style="background: rgba(0,0,0,0.05); color: var(--text-primary); padding: 8px 16px; border-radius: 8px; text-decoration: none; font-weight: bold;"><i class="fas fa-arrow-left"></i> Back</a>
    </div>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 30px;">
        <div>
            <!-- Stats -->
            <div style="background: var(--bg-card); padding: 25px; border-radius: 16px; border: 1px solid var(--border-color); margin-bottom: 20px;">
                <h3 style="font-size: 16px; font-weight: 800; color: var(--text-muted); margin-bottom: 20px; text-transform: uppercase; border-bottom: 1px solid var(--border-color); padding-bottom: 10px;">Performance</h3>
                
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                    <div style="background: rgba(14,165,233,0.1); padding: 20px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 13px; color: #0ea5e9; font-weight: 800; text-transform: uppercase;">Impressions</div>
                        <div style="font-size: 32px; font-weight: 900; color: #0ea5e9;" th:text="${ad.impressionCount}">0</div>
                    </div>
                    <div style="background: rgba(16,185,129,0.1); padding: 20px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 13px; color: #10B981; font-weight: 800; text-transform: uppercase;">Clicks</div>
                        <div style="font-size: 32px; font-weight: 900; color: #10B981;" th:text="${ad.clickCount}">0</div>
                    </div>
                </div>
                
                <div style="background: rgba(139,92,246,0.1); padding: 20px; border-radius: 12px; text-align: center; margin-bottom: 20px;">
                    <div style="font-size: 13px; color: #8B5CF6; font-weight: 800; text-transform: uppercase;">Click-Through Rate (CTR)</div>
                    <div style="font-size: 32px; font-weight: 900; color: #8B5CF6;"><span th:text="${ctr}">0.0</span>%</div>
                </div>
                
                <form th:action="@{'/admin/advertisements/' + ${ad.id} + '/status'}" method="post" style="display: flex; gap: 10px;">
                    <select name="status" style="flex: 1; padding: 12px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-body); color: var(--text-primary); font-weight: bold;">
                        <option value="ACTIVE" th:selected="${ad.status.name() == 'ACTIVE'}">ACTIVE</option>
                        <option value="PAUSED" th:selected="${ad.status.name() == 'PAUSED'}">PAUSED</option>
                    </select>
                    <button type="submit" class="btn" style="padding: 12px 20px; background: var(--text-primary); color: var(--bg-card); border: none; border-radius: 8px; font-weight: bold; cursor: pointer;">Update Status</button>
                </form>
            </div>
            
            <!-- Details -->
            <div style="background: var(--bg-card); padding: 25px; border-radius: 16px; border: 1px solid var(--border-color);">
                <h3 style="font-size: 16px; font-weight: 800; color: var(--text-muted); margin-bottom: 20px; text-transform: uppercase; border-bottom: 1px solid var(--border-color); padding-bottom: 10px;">Details</h3>
                
                <div style="display: grid; grid-template-columns: 100px 1fr; gap: 15px; margin-bottom: 10px; font-size: 14px;">
                    <div style="color: var(--text-muted); font-weight: 600;">Status</div>
                    <div style="font-weight: 800; color: var(--text-primary);" th:text="${ad.status}">ACTIVE</div>
                    
                    <div style="color: var(--text-muted); font-weight: 600;">Placement</div>
                    <div style="font-weight: 800;" th:text="${ad.placement}">HERO</div>
                    
                    <div style="color: var(--text-muted); font-weight: 600;">Start Date</div>
                    <div style="font-weight: 800;" th:text="${ad.startDateTime != null ? #temporals.format(ad.startDateTime, 'dd MMM yyyy, HH:mm') : 'Immediate'}">--</div>
                    
                    <div style="color: var(--text-muted); font-weight: 600;">End Date</div>
                    <div style="font-weight: 800;" th:text="${ad.endDateTime != null ? #temporals.format(ad.endDateTime, 'dd MMM yyyy, HH:mm') : 'Never'}">--</div>
                    
                    <div style="color: var(--text-muted); font-weight: 600;">Priority</div>
                    <div style="font-weight: 800;" th:text="${ad.priority}">0</div>
                </div>
                
                <a th:href="@{'/admin/advertisements/' + ${ad.id} + '/edit'}" class="btn" style="display: block; text-align: center; margin-top: 20px; width: 100%; padding: 12px; background: rgba(14,165,233,0.1); color: #0ea5e9; border: none; border-radius: 8px; font-weight: bold; text-decoration: none;">Edit Details</a>
            </div>
        </div>
        
        <!-- Live Preview Replica -->
        <div>
            <div style="position: sticky; top: 20px;">
                <h3 style="font-size: 16px; font-weight: 800; color: var(--text-muted); margin-bottom: 15px; text-transform: uppercase;">Actual Preview</h3>
                
                <div style="background: var(--bg-card); border-radius: 20px; overflow: hidden; border: 1px solid var(--border-color); box-shadow: 0 10px 25px rgba(0,0,0,0.05); position: relative;">
                    <div style="position: absolute; top: 15px; right: 15px; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); color: white; font-size: 10px; font-weight: 800; padding: 4px 10px; border-radius: 20px; z-index: 2; letter-spacing: 1px;">SPONSORED</div>
                    
                    <div style="width: 100%; aspect-ratio: 16/9; background: #f1f5f9; position: relative;">
                        <img th:src="${ad.imageUrl}" style="width: 100%; height: 100%; object-fit: cover; position: absolute; top: 0; left: 0;">
                    </div>
                    
                    <div style="padding: 25px;">
                        <div style="font-size: 12px; font-weight: 800; color: var(--brand-primary); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px;" th:text="${ad.organizationName}">Organization Name</div>
                        <h3 style="font-size: 22px; font-weight: 900; color: var(--text-primary); margin: 0 0 12px 0; line-height: 1.3;" th:text="${ad.title}">Advertisement Title</h3>
                        <p style="font-size: 15px; color: var(--text-secondary); line-height: 1.6; margin: 0 0 20px 0;" th:text="${ad.description}">Description</p>
                        
                        <div style="display: flex; gap: 10px; align-items: center;">
                            <a th:href="${ad.ctaUrl}" target="_blank" style="flex: 1; text-align: center; background: linear-gradient(135deg, #2563eb, #0ea5e9); color: white; padding: 12px 20px; border-radius: 12px; font-weight: 800; text-decoration: none; box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);"><span th:text="${ad.ctaText}">Register Now</span> <i class="fas fa-arrow-right" style="margin-left: 6px;"></i></a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>
"@

$newAdsList = $content -replace '(?s)<main class="social-feed".*?</main>', $adsListMain
Set-Content -Path 'src\main\resources\templates\admin-advertisements.html' -Value $newAdsList

$newAdsForm = $content -replace '(?s)<main class="social-feed".*?</main>', $adsFormMain
Set-Content -Path 'src\main\resources\templates\admin-advertisement-form.html' -Value $newAdsForm

$newAdsDetails = $content -replace '(?s)<main class="social-feed".*?</main>', $adsDetailsMain
Set-Content -Path 'src\main\resources\templates\admin-advertisement-details.html' -Value $newAdsDetails
