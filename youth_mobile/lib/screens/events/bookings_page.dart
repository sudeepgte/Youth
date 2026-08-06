import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../config/api_config.dart';
import '../../models/event_model.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import 'ticket_page.dart';

class BookingsPage extends StatefulWidget {
  const BookingsPage({super.key});

  @override
  State<BookingsPage> createState() => _BookingsPageState();
}

class _BookingsPageState extends State<BookingsPage> {
  List<BookingModel> _bookings = [];
  bool _loading = true;
  String? _error;
  final Set<int> _cancelling = {};
  final _searchCtrl = TextEditingController();
  String _filter = 'All';

  static const _filters = ['All', 'Upcoming', 'Completed', 'Cancelled'];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await AppApi.bookings();
      if (!mounted) return;
      setState(() {
        _bookings = list;
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

  List<BookingModel> get _filtered {
    final q = _searchCtrl.text.trim().toLowerCase();
    return _bookings.where((b) {
      final status = (b.status ?? '').toUpperCase();
      final eventStatus = (b.event?.status ?? '').toUpperCase();
      switch (_filter) {
        case 'Cancelled':
          if (!status.contains('CANCEL')) return false;
          break;
        case 'Completed':
          if (status.contains('CANCEL')) return false;
          if (eventStatus != 'COMPLETED' && !status.contains('COMPLETE')) return false;
          break;
        case 'Upcoming':
          if (status.contains('CANCEL') || eventStatus == 'COMPLETED') return false;
          break;
      }
      if (q.isEmpty) return true;
      final title = (b.event?.title ?? '').toLowerCase();
      final ticket = (b.ticketId ?? '').toLowerCase();
      return title.contains(q) || ticket.contains(q);
    }).toList();
  }

  Future<void> _cancel(BookingModel booking) async {
    setState(() => _cancelling.add(booking.id));
    try {
      await AppApi.cancelBooking(booking.id);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Booking cancelled');
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _cancelling.remove(booking.id));
    }
  }

  void _openTicket(BookingModel booking) {
    final ticketId = booking.ticketId;
    if (ticketId == null || ticketId.isEmpty) {
      AppTheme.showError(context, 'No ticket available');
      return;
    }
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => TicketPage(ticketId: ticketId)),
    );
  }

  String _printableUrl(String ticketId) {
    final base = ApiConfig.mediaUrl('/events/ticket/$ticketId');
    return base.contains('?') ? '$base&download=true' : '$base?download=true';
  }

  Future<void> _openPdf(BookingModel booking) async {
    final ticketId = booking.ticketId;
    if (ticketId == null || ticketId.isEmpty) {
      AppTheme.showError(context, 'No ticket available');
      return;
    }
    final uri = Uri.tryParse(_printableUrl(ticketId));
    if (uri == null) return;
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else if (mounted) {
      AppTheme.showError(context, 'Could not open ticket');
    }
  }

  Future<void> _shareTicket(BookingModel booking) async {
    final ticketId = booking.ticketId;
    if (ticketId == null || ticketId.isEmpty) {
      AppTheme.showError(context, 'No ticket available');
      return;
    }
    final title = booking.event?.title ?? 'Event';
    await Share.share(
      'My Youthian ticket for $title\nTicket: $ticketId\n${_printableUrl(ticketId)}',
      subject: 'Ticket — $title',
    );
  }

  bool _canCancel(BookingModel b) {
    final status = (b.status ?? '').toUpperCase();
    final eventStatus = (b.event?.status ?? '').toUpperCase();
    if (status.contains('CANCEL')) return false;
    if (eventStatus == 'COMPLETED') return false;
    return true;
  }

  @override
  Widget build(BuildContext context) {
    final items = _filtered;
    return DashboardScaffold(
      active: AppDrawerItem.bookings,
      title: 'My Bookings',
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchCtrl,
              decoration: AppTheme.dashboardInput('Search name or ticket ID').copyWith(
                prefixIcon: const Icon(Icons.search),
              ),
              onChanged: (_) => setState(() {}),
            ),
          ),
          SizedBox(
            height: 40,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: _filters.length,
              separatorBuilder: (_, _) => const SizedBox(width: 8),
              itemBuilder: (_, i) {
                final f = _filters[i];
                final selected = _filter == f;
                return ChoiceChip(
                  label: Text(f),
                  selected: selected,
                  onSelected: (_) => setState(() => _filter = f),
                  selectedColor: AppTheme.primary.withValues(alpha: 0.18),
                  labelStyle: GoogleFonts.outfit(
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                    color: selected ? AppTheme.primary : AppTheme.textSecondary,
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: _loading
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
                    : items.isEmpty
                        ? Center(child: Text('No bookings yet', style: GoogleFonts.inter(color: Colors.grey)))
                        : RefreshIndicator(
                            onRefresh: _load,
                            child: ListView.separated(
                              padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                              itemCount: items.length,
                              separatorBuilder: (_, _) => const SizedBox(height: 10),
                              itemBuilder: (context, index) {
                                final booking = items[index];
                                final title = booking.event?.title ?? 'Event';
                                final status = booking.status ?? 'REGISTERED';
                                final cancelling = _cancelling.contains(booking.id);
                                return Material(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.circular(16),
                                  child: InkWell(
                                    borderRadius: BorderRadius.circular(16),
                                    onTap: () => _openTicket(booking),
                                    child: Padding(
                                      padding: const EdgeInsets.all(14),
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Row(
                                            children: [
                                              Expanded(
                                                child: Text(
                                                  title,
                                                  style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16),
                                                ),
                                              ),
                                              Container(
                                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                                decoration: BoxDecoration(
                                                  color: status.toUpperCase().contains('CANCEL')
                                                      ? const Color(0xFFFEE2E2)
                                                      : const Color(0xFFDCFCE7),
                                                  borderRadius: BorderRadius.circular(8),
                                                ),
                                                child: Text(
                                                  status,
                                                  style: GoogleFonts.outfit(fontSize: 11, fontWeight: FontWeight.w700),
                                                ),
                                              ),
                                            ],
                                          ),
                                          if (booking.ticketId != null) ...[
                                            const SizedBox(height: 6),
                                            Text('Ticket ${booking.ticketId}', style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
                                          ],
                                          if (booking.event?.venue != null) ...[
                                            const SizedBox(height: 4),
                                            Text(booking.event!.venue!, style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
                                          ],
                                          if (booking.event?.dateTime != null) ...[
                                            const SizedBox(height: 4),
                                            Text(booking.event!.dateTime!, style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
                                          ],
                                          if (booking.registrationDate != null) ...[
                                            const SizedBox(height: 4),
                                            Text(
                                              'Booked ${booking.registrationDate}',
                                              style: GoogleFonts.inter(fontSize: 11, color: Colors.black38),
                                            ),
                                          ],
                                          const SizedBox(height: 10),
                                          Row(
                                            children: [
                                              TextButton(
                                                onPressed: () => _openTicket(booking),
                                                child: const Text('View Ticket'),
                                              ),
                                              if (booking.ticketId != null && booking.ticketId!.isNotEmpty) ...[
                                                TextButton(
                                                  onPressed: () => _openPdf(booking),
                                                  child: const Text('PDF'),
                                                ),
                                                IconButton(
                                                  tooltip: 'Share',
                                                  onPressed: () => _shareTicket(booking),
                                                  icon: const Icon(Icons.share_outlined, size: 20),
                                                ),
                                              ],
                                              if (_canCancel(booking))
                                                TextButton(
                                                  onPressed: cancelling ? null : () => _cancel(booking),
                                                  child: cancelling
                                                      ? const SizedBox(
                                                          width: 16,
                                                          height: 16,
                                                          child: CircularProgressIndicator(strokeWidth: 2),
                                                        )
                                                      : const Text('Cancel', style: TextStyle(color: Colors.red)),
                                                ),
                                              const Spacer(),
                                              const Icon(Icons.chevron_right, color: Colors.black38),
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
