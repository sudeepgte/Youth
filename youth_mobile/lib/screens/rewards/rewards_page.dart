import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../config/api_config.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import '../events/ticket_page.dart';
import 'reward_redeem_page.dart';

class RewardsPage extends StatefulWidget {
  const RewardsPage({super.key});

  @override
  State<RewardsPage> createState() => _RewardsPageState();
}

class _RewardsPageState extends State<RewardsPage> {
  List<Map<String, dynamic>> _rewards = [];
  bool _loading = true;
  String? _error;
  final Set<String> _busy = {};
  final _codeCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await AppApi.rewards();
      if (!mounted) return;
      setState(() {
        _rewards = list;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppTheme.extractError(e);
        _loading = false;
      });
    }
  }

  String _partnerRedeemUrl(String code) => ApiConfig.mediaUrl('/rewards/redeem/$code');

  Future<void> _redeem(String rewardCode) async {
    setState(() => _busy.add(rewardCode));
    try {
      await AppApi.redeemReward(rewardCode);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Reward redeemed');
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _busy.remove(rewardCode));
    }
  }

  Future<void> _reveal(int registrationId) async {
    final key = 'reveal-$registrationId';
    setState(() => _busy.add(key));
    try {
      await AppApi.revealReward(registrationId);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Reward revealed');
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _busy.remove(key));
    }
  }

  Future<void> _openPartnerScan() async {
    final code = _codeCtrl.text.trim();
    if (code.isEmpty) {
      AppTheme.showError(context, 'Enter a reward code');
      return;
    }
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => RewardRedeemPage(rewardCode: code)),
    );
    if (mounted) _load();
  }

  void _showDetail(Map<String, dynamic> r) {
    final code = r['rewardCode']?.toString() ?? '';
    final status = r['status']?.toString() ?? '';
    final redeemUrl = _partnerRedeemUrl(code);
    final stall = r['redeemStallNumber']?.toString();
    final store = r['storeName']?.toString();
    final address = r['storeAddress']?.toString();
    final contact = r['storeContact']?.toString();
    final coupon = r['couponCode']?.toString();
    final delivery = r['deliveryMethod']?.toString() ?? r['deliveryType']?.toString();
    final eta = r['estimatedDelivery']?.toString();
    final terms = r['terms']?.toString();
    final logo = r['sponsorLogoUrl']?.toString();

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) {
        final h = MediaQuery.sizeOf(ctx).height * 0.85;
        return SizedBox(
          height: h,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
            children: [
              Center(
                child: Container(width: 40, height: 4, decoration: BoxDecoration(color: Colors.black12, borderRadius: BorderRadius.circular(4))),
              ),
              const SizedBox(height: 16),
              if (logo != null && logo.isNotEmpty)
                Center(
                  child: Image.network(ApiConfig.mediaUrl(logo), height: 48, errorBuilder: (_, _, _) => const SizedBox.shrink()),
                ),
              Text(
                r['offerTitle']?.toString() ?? 'Reward',
                style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 22),
              ),
              if ((r['partnerName']?.toString() ?? '').isNotEmpty)
                Text(r['partnerName'].toString(), style: GoogleFonts.inter(color: Colors.black54)),
              const SizedBox(height: 8),
              Text('Status: $status', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: AppTheme.primary)),
              if ((r['offerDescription']?.toString() ?? '').isNotEmpty) ...[
                const SizedBox(height: 12),
                Text(r['offerDescription'].toString(), style: GoogleFonts.inter(height: 1.4)),
              ],
              if (stall != null && stall.isNotEmpty) ...[
                const SizedBox(height: 12),
                _infoRow(Icons.storefront, 'Stall', stall),
              ],
              if (store != null && store.isNotEmpty) ...[
                const SizedBox(height: 8),
                _infoRow(Icons.store, 'Store', store),
              ],
              if (address != null && address.isNotEmpty) ...[
                const SizedBox(height: 8),
                _infoRow(Icons.place_outlined, 'Address', address),
                TextButton.icon(
                  onPressed: () => launchUrl(
                    Uri.parse('https://www.google.com/maps/search/?api=1&query=${Uri.encodeComponent(address)}'),
                    mode: LaunchMode.externalApplication,
                  ),
                  icon: const Icon(Icons.map_outlined, size: 18),
                  label: const Text('Open in Maps'),
                ),
              ],
              if (contact != null && contact.isNotEmpty) ...[
                const SizedBox(height: 8),
                _infoRow(Icons.phone_outlined, 'Contact', contact),
                TextButton.icon(
                  onPressed: () => launchUrl(Uri.parse('tel:$contact')),
                  icon: const Icon(Icons.call, size: 18),
                  label: const Text('Call'),
                ),
              ],
              if (coupon != null && coupon.isNotEmpty) ...[
                const SizedBox(height: 8),
                _infoRow(Icons.confirmation_number_outlined, 'Coupon', coupon),
              ],
              if (delivery != null && delivery.isNotEmpty) ...[
                const SizedBox(height: 8),
                _infoRow(Icons.local_shipping_outlined, 'Delivery', delivery),
              ],
              if (eta != null && eta.isNotEmpty) ...[
                const SizedBox(height: 8),
                _infoRow(Icons.schedule, 'ETA', eta),
              ],
              if (terms != null && terms.isNotEmpty) ...[
                const SizedBox(height: 12),
                Text('Terms', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                Text(terms, style: GoogleFonts.inter(fontSize: 13, color: Colors.black54, height: 1.4)),
              ],
              if (code.isNotEmpty) ...[
                const SizedBox(height: 20),
                Text('Partner QR', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                const SizedBox(height: 8),
                Center(
                  child: QrImageView(
                    data: redeemUrl,
                    size: 180,
                    backgroundColor: Colors.white,
                  ),
                ),
                const SizedBox(height: 8),
                SelectableText(code, textAlign: TextAlign.center, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, letterSpacing: 1.2)),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    TextButton.icon(
                      onPressed: () {
                        Clipboard.setData(ClipboardData(text: code));
                        AppTheme.showSuccess(context, 'Code copied');
                      },
                      icon: const Icon(Icons.copy, size: 16),
                      label: const Text('Copy code'),
                    ),
                    TextButton.icon(
                      onPressed: () => Share.share('Redeem my Youthian reward\nCode: $code\n$redeemUrl'),
                      icon: const Icon(Icons.share_outlined, size: 16),
                      label: const Text('Share'),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              if (status == 'AVAILABLE' && code.isNotEmpty)
                ElevatedButton(
                  onPressed: _busy.contains(code)
                      ? null
                      : () async {
                          Navigator.pop(ctx);
                          await _redeem(code);
                        },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primary,
                    foregroundColor: Colors.white,
                    minimumSize: const Size.fromHeight(44),
                  ),
                  child: const Text('Mark redeemed'),
                ),
            ],
          ),
        );
      },
    );
  }

  Widget _infoRow(IconData icon, String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18, color: AppTheme.primary),
        const SizedBox(width: 8),
        Expanded(
          child: Text('$label: $value', style: GoogleFonts.inter(fontSize: 14)),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      drawer: const AppDrawer(active: AppDrawerItem.rewards),
      appBar: AppBar(
        title: Text('Rewards', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        leading: Builder(
          builder: (ctx) => IconButton(
            icon: const Icon(Icons.menu, color: AppTheme.textPrimary),
            onPressed: () => Scaffold.of(ctx).openDrawer(),
          ),
        ),
      ),
      body: Column(
        children: [
          Container(
            color: Colors.white,
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _codeCtrl,
                    decoration: AppTheme.dashboardInput('Partner: scan / enter code'),
                    textCapitalization: TextCapitalization.characters,
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _openPartnerScan,
                  style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
                  child: const Text('Open'),
                ),
              ],
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(child: Text(_error!, style: const TextStyle(color: Colors.red)))
                    : _rewards.isEmpty
                        ? Center(
                            child: Padding(
                              padding: const EdgeInsets.all(24),
                              child: Text(
                                'No rewards yet. Reveal secret rewards from event tickets after attendance.',
                                textAlign: TextAlign.center,
                                style: GoogleFonts.inter(color: Colors.black45),
                              ),
                            ),
                          )
                        : RefreshIndicator(
                            onRefresh: _load,
                            child: ListView.builder(
                              padding: const EdgeInsets.all(12),
                              itemCount: _rewards.length,
                              itemBuilder: (_, i) {
                                final r = _rewards[i];
                                final code = r['rewardCode']?.toString() ?? '';
                                final status = r['status']?.toString() ?? 'UNKNOWN';
                                final canRedeem = status == 'AVAILABLE' && code.isNotEmpty;
                                final regId = (r['registrationId'] as num?)?.toInt();
                                final ticketId = r['ticketId']?.toString();
                                final needsReveal = status.toUpperCase().contains('PENDING') ||
                                    status.toUpperCase().contains('UNREVEALED') ||
                                    (code.isEmpty && regId != null);

                                return Card(
                                  margin: const EdgeInsets.only(bottom: 10),
                                  child: InkWell(
                                    onTap: () => _showDetail(r),
                                    borderRadius: BorderRadius.circular(12),
                                    child: Padding(
                                      padding: const EdgeInsets.all(12),
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            r['offerTitle']?.toString() ?? r['eventTitle']?.toString() ?? 'Reward',
                                            style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 16),
                                          ),
                                          const SizedBox(height: 4),
                                          Text(
                                            [
                                              if ((r['partnerName']?.toString() ?? '').isNotEmpty) r['partnerName'],
                                              if ((r['redeemStallNumber']?.toString() ?? '').isNotEmpty)
                                                'Stall ${r['redeemStallNumber']}',
                                              if ((r['storeName']?.toString() ?? '').isNotEmpty) r['storeName'],
                                              'Status: $status',
                                              'Code: ${code.isEmpty ? '—' : code}',
                                            ].join('\n'),
                                            style: GoogleFonts.inter(fontSize: 13, color: Colors.black54, height: 1.4),
                                          ),
                                          const SizedBox(height: 10),
                                          Row(
                                            children: [
                                              if (canRedeem)
                                                ElevatedButton(
                                                  onPressed: _busy.contains(code) ? null : () => _redeem(code),
                                                  style: ElevatedButton.styleFrom(
                                                    backgroundColor: AppTheme.primary,
                                                    foregroundColor: Colors.white,
                                                  ),
                                                  child: const Text('Redeem'),
                                                ),
                                              if (needsReveal && regId != null) ...[
                                                if (canRedeem) const SizedBox(width: 8),
                                                OutlinedButton(
                                                  onPressed: _busy.contains('reveal-$regId') ? null : () => _reveal(regId),
                                                  child: const Text('Reveal'),
                                                ),
                                              ],
                                              const Spacer(),
                                              TextButton(onPressed: () => _showDetail(r), child: const Text('Details / QR')),
                                              if (ticketId != null && ticketId.isNotEmpty)
                                                TextButton(
                                                  onPressed: () => Navigator.push(
                                                    context,
                                                    MaterialPageRoute(builder: (_) => TicketPage(ticketId: ticketId)),
                                                  ),
                                                  child: const Text('Ticket'),
                                                ),
                                            ],
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                );
                              },
                            ),
                          ),
          ),
        ],
      ),
    );
  }
}
