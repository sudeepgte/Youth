import os

path = 'src/main/resources/templates/tournament-view.html'
with open(path, 'r') as f:
    content = f.read()

old_content = '''                    <div th:if="${isRegistered}" style="background: rgba(16, 185, 129, 0.1); color: #059669; padding: 12px 20px; border-radius: 12px; font-weight: 800; display: inline-flex; align-items: center; gap: 8px;">
                        <i class="fas fa-check-circle"></i> You are registered!
                    </div>'''

new_content = '''                    <div th:if="${isRegistered}" style="background: rgba(16, 185, 129, 0.1); color: #059669; padding: 12px 20px; border-radius: 12px; font-weight: 800; display: inline-flex; align-items: center; gap: 8px; margin-bottom: 15px;">
                        <i class="fas fa-check-circle"></i> You are registered!
                    </div>
                    
                    <div th:if="${isRegistered and tournament.mode == 'OFFLINE' and myParticipant != null}" style="background: white; border: 2px dashed #cbd5e1; border-radius: 15px; padding: 20px; text-align: center; max-width: 300px;">
                        <h4 style="margin: 0 0 10px 0; color: #334155;">🎫 OFFLINE BATTLE PASS</h4>
                        <div style="background: #f1f5f9; padding: 15px; border-radius: 10px; margin-bottom: 15px;">
                            <img th:src="@{'https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=' + ${myParticipant.qrPassCode}}" alt="QR Code" style="width: 150px; height: 150px; border-radius: 10px;">
                        </div>
                        <p style="margin: 0 0 5px 0; font-weight: 700; color: #0f172a;" th:text="${myParticipant.user.username}"></p>
                        <p style="margin: 0; font-size: 13px; color: #64748b;" th:text="'Participant ID: ' + ${myParticipant.participantNumber}"></p>
                        <div style="margin-top: 15px;">
                            <span th:if="${myParticipant.checkedIn}" style="background: #d1fae5; color: #059669; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 800;"><i class="fas fa-check-double"></i> Checked In</span>
                            <span th:if="${!myParticipant.checkedIn}" style="background: #f1f5f9; color: #64748b; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 800;">Pending Check-in</span>
                        </div>
                    </div>'''

content = content.replace(old_content, new_content)

with open(path, 'w') as f:
    f.write(content)
