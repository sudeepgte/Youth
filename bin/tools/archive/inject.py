import codecs

html_to_inject = '''
                <!-- Result Screen (Phase 5) -->
                <div th:if="\" style="background: linear-gradient(135deg, #1f2937, #111827); border-radius: 20px; padding: 40px; text-align: center; color: white; margin-bottom: 24px; border: 2px solid #374151; box-shadow: 0 10px 30px rgba(0,0,0,0.3);">
                    <div style="font-size: 64px; margin-bottom: 10px;">🏆</div>
                    <h2 style="font-size: 28px; font-weight: 900; margin: 0 0 10px 0; color: #F84464; text-transform: uppercase;">BATTLE CONCLUDED</h2>
                    
                    <div th:if="\">
                        <h3 style="font-size: 20px; font-weight: 700; margin-bottom: 30px; color: #9CA3AF;">It's a TIE!</h3>
                        <div style="display: flex; justify-content: center; gap: 30px; align-items: center; margin-bottom: 30px;">
                            <div th:if="\">
                                <img th:src="\" style="width: 80px; height: 80px; border-radius: 50%; border: 3px solid #9CA3AF; object-fit: cover;">
                                <div style="font-weight: bold; margin-top: 10px;" th:text="\">Player 1</div>
                            </div>
                            <div style="font-weight: 900; color: #6B7280; font-size: 24px;">VS</div>
                            <div th:if="\">
                                <img th:src="\" style="width: 80px; height: 80px; border-radius: 50%; border: 3px solid #9CA3AF; object-fit: cover;">
                                <div style="font-weight: bold; margin-top: 10px;" th:text="\">Player 2</div>
                            </div>
                        </div>
                    </div>

                    <div th:if="\">
                        <h3 style="font-size: 16px; font-weight: 800; margin-bottom: 20px; color: #10B981; letter-spacing: 2px;">WINNER</h3>
                        <div th:if="\" style="margin-bottom: 30px;">
                            <img th:src="\" style="width: 100px; height: 100px; border-radius: 50%; border: 4px solid #10B981; box-shadow: 0 0 20px rgba(16, 185, 129, 0.4); object-fit: cover; margin: 0 auto;">
                            <div style="font-weight: 900; font-size: 24px; margin-top: 15px;" th:text="\">Winner Name</div>
                        </div>
                    </div>

                    <div style="background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; padding: 20px; display: inline-flex; gap: 40px; margin-bottom: 30px;">
                        <div>
                            <div style="font-size: 12px; color: #9CA3AF; font-weight: 700; margin-bottom: 5px;">PRIZE POOL</div>
                            <div style="font-size: 20px; font-weight: 900; color: #F59E0B;"><i class="fas fa-coins"></i> <span th:text="\">0.0</span></div>
                        </div>
                        <div>
                            <div style="font-size: 12px; color: #9CA3AF; font-weight: 700; margin-bottom: 5px;">XP AWARDED</div>
                            <div style="font-size: 20px; font-weight: 900; color: #8B5CF6;"><i class="fas fa-star"></i> <span th:text="\">100</span></div>
                        </div>
                    </div>

                    <div>
                        <a th:href="@{/battles}" style="background: #F84464; color: white; padding: 12px 30px; border-radius: 30px; text-decoration: none; font-weight: 800; font-size: 16px; transition: 0.2s; display: inline-block;">Rematch / Find New</a>
                    </div>
                </div>
'''

with codecs.open('src/main/resources/templates/battle-arena.html', 'r', 'utf-8') as f:
    content = f.read()

target = '<!-- BATTLES LIST VIEW (when no single battle selected) -->'
content = content.replace(target, html_to_inject + '\n\n' + '            ' + target)

with codecs.open('src/main/resources/templates/battle-arena.html', 'w', 'utf-8') as f:
    f.write(content)