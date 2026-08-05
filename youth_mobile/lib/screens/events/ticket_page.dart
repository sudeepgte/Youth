import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../config/api_config.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class TicketPage extends StatefulWidget {
  const TicketPage({super.key, required this.ticketId});

  final String ticketId;

  @override
  State<TicketPage> createState() => _TicketPageState();
}

class _TicketPageState extends State<TicketPage> {
  Map<String, dynamic>? _ticket;
  bool _loading = true;
  bool _revealing = false;
  String? _error;
  String? _rewardMessage;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await AppApi.ticket(widget.ticketId);
      if (!mounted) return;
      setState(() {
        _ticket = data;
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

  int? get _registrationId {
    final t = _ticket;
    if (t == null) return null;
    final id = t['registrationId'] ?? t['id'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    return null;
  }

  bool get _secretRewardsEnabled {
    final event = _ticket?['event'];
    if (event is Map) return event['enableSecretRewards'] == true;
    return _ticket?['enableSecretRewards'] == true;
  }

  String get _printableUrl {
    final path = _ticket?['printableUrl']?.toString();
    final base = (path != null && path.isNotEmpty)
        ? ApiConfig.mediaUrl(path)
        : ApiConfig.mediaUrl('/events/ticket/${_str(_ticket?['ticketId']) ?? widget.ticketId}');
    if (base.contains('?')) return '$base&download=true';
    return '$base?download=true';
  }

  Future<void> _openPrintable() async {
    final uri = Uri.tryParse(_printableUrl);
    if (uri == null) return;
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else if (mounted) {
      AppTheme.showError(context, 'Could not open ticket page');
    }
  }

  Future<void> _shareTicket() async {
    final event = _ticket?['event'] is Map
        ? Map<String, dynamic>.from(_ticket!['event'] as Map)
        : <String, dynamic>{};
    final title = event['title']?.toString() ?? 'Event';
    final tid = _str(_ticket?['ticketId']) ?? widget.ticketId;
    await Share.share(
      'My Youthian ticket for $title\nTicket: $tid\n$_printableUrl',
      subject: 'Ticket — $title',
    );
  }

  Future<void> _revealReward() async {
    final regId = _registrationId;
    if (regId == null) return;
    setState(() => _revealing = true);
    try {
      await AppApi.revealReward(regId);
      if (!mounted) return;
      setState(() => _rewardMessage = 'Reward revealed!');
      AppTheme.showSuccess(context, 'Reward revealed');
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _revealing = false);
    }
  }

  Widget _field(String label, String? value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(
              label,
              style: GoogleFonts.inter(fontSize: 13, color: Colors.black45, fontWeight: FontWeight.w600),
            ),
          ),
          Expanded(
            child: Text(
              (value == null || value.isEmpty) ? '—' : value,
              style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 15),
            ),
          ),
        ],
      ),
    );
  }

  String? _str(dynamic v) => v?.toString();

  @override
  Widget build(BuildContext context) {
    final event = _ticket?['event'] is Map
        ? Map<String, dynamic>.from(_ticket!['event'] as Map)
        : <String, dynamic>{};

    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Ticket', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        actions: [
          if (_ticket != null)
            IconButton(
              tooltip: 'Share',
              onPressed: _shareTicket,
              icon: const Icon(Icons.share_outlined),
            ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(_error!, style: const TextStyle(color: Colors.red)),
                      ElevatedButton(onPressed: _load, child: const Text('Retry')),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: Colors.blue.shade100),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              event['title']?.toString() ?? 'Event Ticket',
                              style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 22),
                            ),
                            const SizedBox(height: 8),
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 16),
                              decoration: BoxDecoration(
                                color: AppTheme.primary.withValues(alpha: 0.1),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(color: AppTheme.primary.withValues(alpha: 0.25)),
                              ),
                              child: Column(
                                children: [
                                  Text('SCAN / VERIFY', style: GoogleFonts.inter(fontSize: 11, color: AppTheme.textMuted, letterSpacing: 1.2)),
                                  const SizedBox(height: 6),
                                  Text(
                                    _str(_ticket?['ticketId']) ?? widget.ticketId,
                                    textAlign: TextAlign.center,
                                    style: GoogleFonts.outfit(
                                      fontWeight: FontWeight.w900,
                                      letterSpacing: 2,
                                      fontSize: 22,
                                      color: AppTheme.primary,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 20),
                            _field('Status', _str(_ticket?['status'])),
                            _field('Payment', _str(_ticket?['paymentStatus'])),
                            _field('Tier', _str(_ticket?['selectedTier'])),
                            _field('Registered', _str(_ticket?['registrationDate'])),
                            _field('Attendee', _str(_ticket?['fullName'] ?? _ticket?['attendeeName'])),
                            _field('Email', _str(_ticket?['email'])),
                            _field('Phone', _str(_ticket?['phone'])),
                            _field('College', _str(_ticket?['college'])),
                            _field('Year', _str(_ticket?['yearOfStudy'])),
                            _field('Quantity', _str(_ticket?['quantity'])),
                            if (_ticket?['seats'] is List && (_ticket!['seats'] as List).isNotEmpty)
                              _field('Seats', (_ticket!['seats'] as List).join(', ')),
                            if (event['venue'] != null) _field('Venue', _str(event['venue'])),
                            if (event['dateTime'] != null) _field('When', _str(event['dateTime'])),
                            if (event['category'] != null) _field('Category', _str(event['category'])),
                          ],
                        ),
                      ),
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: _openPrintable,
                              icon: const Icon(Icons.picture_as_pdf_outlined),
                              label: const Text('Print / PDF'),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: ElevatedButton.icon(
                              onPressed: _shareTicket,
                              icon: const Icon(Icons.share_outlined),
                              label: const Text('Share'),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: AppTheme.primary,
                                foregroundColor: Colors.white,
                              ),
                            ),
                          ),
                        ],
                      ),
                      if (_secretRewardsEnabled && _registrationId != null) ...[
                        const SizedBox(height: 16),
                        Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFF8E7),
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: const Color(0xFFF5C542)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Secret Reward',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                _rewardMessage ?? 'Reveal your surprise reward for this booking.',
                                style: GoogleFonts.inter(fontSize: 13, color: Colors.black54),
                              ),
                              const SizedBox(height: 12),
                              SizedBox(
                                width: double.infinity,
                                child: ElevatedButton.icon(
                                  onPressed: _revealing ? null : _revealReward,
                                  icon: _revealing
                                      ? const SizedBox(
                                          width: 16,
                                          height: 16,
                                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                        )
                                      : const Icon(Icons.card_giftcard),
                                  label: const Text('Reveal Reward'),
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: const Color(0xFFF59E0B),
                                    foregroundColor: Colors.black87,
                                    padding: const EdgeInsets.symmetric(vertical: 12),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
    );
  }
}
