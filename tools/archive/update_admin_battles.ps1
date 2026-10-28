$path = 'src\main\resources\templates\admin-battles.html'
$c = Get-Content $path -Raw

# Replace Header
$oldHeader = '<th>Admin Commission (7%)</th>'
$newHeader = "<th>Admin Commission</th>`r`n                                  <th>Payout Action</th>"
$c = $c.Replace($oldHeader, $newHeader)

# Replace Column Content
$oldCol = '<td style="font-weight: 800; color: #10b981;" 
                                      th:text="${''₹'' + #numbers.formatDecimal((b.entryFee != null ? b.entryFee : 0.0) * 0.07 * joinsCount, 1, 2)}">
                                      ₹10.00
                                  </td>'
$newCol = '<td style="padding: 10px;">
                                    <form th:action="@{/admin/battles/{id}/commission(id=${b.id})}" method="POST" style="display:flex; gap:5px; align-items:center;">
                                        <input type="number" step="0.1" name="adminCommissionPct" th:value="${b.adminCommissionPct != null ? b.adminCommissionPct : 7.0}" style="width: 60px; padding: 4px; border-radius: 4px; border: 1px solid #ccc;"> %
                                        <button type="submit" style="padding: 4px 8px; background: #3b82f6; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 11px;">Save</button>
                                    </form>
                                    <div style="font-weight: 800; color: #10b981; margin-top: 8px;" 
                                        th:text="${''₹'' + #numbers.formatDecimal((b.entryFee != null ? b.entryFee : 0.0) * (b.adminCommissionPct != null ? b.adminCommissionPct / 100.0 : 0.07) * (b.participants != null ? b.participants.size() : 0), 1, 2)}">
                                        ₹0.00
                                    </div>
                                </td>
                                <td>
                                    <div th:if="${''COMPLETED''.equals(b.status)}">
                                        <div th:if="${b.payoutReleased == true}" style="color: #10b981; font-weight: bold;"><i class="fas fa-check-circle"></i> Released</div>
                                        <form th:if="${b.payoutReleased == null or !b.payoutReleased}" th:action="@{/admin/battles/{id}/release(id=${b.id})}" method="POST">
                                            <button type="submit" style="padding: 6px 12px; background: #8b5cf6; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold;">Release Payout</button>
                                        </form>
                                    </div>
                                    <div th:unless="${''COMPLETED''.equals(b.status)}" style="color: #64748b; font-size: 11px;">
                                        Waiting
                                    </div>
                                </td>'
$c = $c.Replace($oldCol, $newCol)

Set-Content -Path $path -Value $c
