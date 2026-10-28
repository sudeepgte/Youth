import os

path = 'src/main/resources/templates/tournaments.html'
with open(path, 'r') as f:
    content = f.read()

old_content = '''                <div style="display: flex; align-items: center; gap: 15px; margin-bottom: 15px; font-size: 13px; font-weight: 600; color: #334155;">
                    <div><i class="fas fa-users" style="color: #3b82f6;"></i> <span th:text="${t.participants.size()}">0</span> / <span th:text="${t.maxParticipants}">16</span> Players</div>
                    <div><i class="fas fa-stopwatch" style="color: #ef4444;"></i> <span th:text="${t.battleDurationMinutes}">10</span> Min Battles</div>
                </div>'''

new_content = '''                <div style="display: flex; flex-direction: column; gap: 8px; margin-bottom: 15px; font-size: 13px; font-weight: 600; color: #334155;">
                    <div style="display: flex; gap: 15px;">
                        <div><i class="fas fa-users" style="color: #3b82f6;"></i> <span th:text="${t.participants.size()}">0</span> / <span th:text="${t.maxParticipants}">16</span> Players</div>
                        <div>
                            <span th:if="${t.mode == 'ONLINE'}"><i class="fas fa-globe" style="color: #ef4444;"></i> Online</span>
                            <span th:if="${t.mode == 'OFFLINE'}"><i class="fas fa-map-marker-alt" style="color: #8b5cf6;"></i> Offline</span>
                        </div>
                    </div>
                    
                    <div th:if="${t.mode == 'OFFLINE'}" style="background: #f8fafc; padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0;">
                        <div style="margin-bottom: 5px;"><i class="fas fa-building" style="color: #64748b;"></i> <span th:text="${t.venue}">Venue Name</span></div>
                        <div><i class="fas fa-calendar" style="color: #64748b;"></i> <span th:text="${t.eventDate + ' ' + t.eventTime}">Date & Time</span></div>
                    </div>
                </div>'''

content = content.replace(old_content, new_content)

with open(path, 'w') as f:
    f.write(content)
