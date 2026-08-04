import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../models/event_model.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('My Bookings', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
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
              : _bookings.isEmpty
                  ? Center(child: Text('No bookings yet', style: GoogleFonts.inter(color: Colors.grey)))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: _bookings.length,
                        itemBuilder: (context, index) {
                          final b = _bookings[index];
                          final event = b.event;
                          final isCancelled = b.status?.toUpperCase() == 'CANCELLED';
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              title: Text(
                                event?.title ?? 'Event #${b.id}',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.bold),
                              ),
                              subtitle: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  if (b.ticketId != null) Text('Ticket: ${b.ticketId}'),
                                  if (b.registrationDate != null) Text(b.registrationDate!),
                                  Text('Status: ${b.status ?? 'Unknown'}'),
                                ],
                              ),
                              trailing: isCancelled
                                  ? null
                                  : TextButton(
                                      onPressed: _cancelling.contains(b.id) ? null : () => _cancel(b),
                                      child: _cancelling.contains(b.id)
                                          ? const SizedBox(
                                              width: 16,
                                              height: 16,
                                              child: CircularProgressIndicator(strokeWidth: 2),
                                            )
                                          : const Text('Cancel', style: TextStyle(color: Colors.red)),
                                    ),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
