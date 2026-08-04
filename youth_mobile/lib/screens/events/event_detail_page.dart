import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../models/event_model.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class EventDetailPage extends StatefulWidget {
  const EventDetailPage({super.key, required this.eventId});

  final int eventId;

  @override
  State<EventDetailPage> createState() => _EventDetailPageState();
}

class _EventDetailPageState extends State<EventDetailPage> {
  EventModel? _event;
  bool _loading = true;
  bool _registering = false;
  String? _error;

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
      final event = await AppApi.eventDetail(widget.eventId);
      if (!mounted) return;
      setState(() {
        _event = event;
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

  Future<void> _register() async {
    setState(() => _registering = true);
    try {
      final res = await AppApi.registerEvent(widget.eventId);
      if (!mounted) return;
      AppTheme.showSuccess(context, res['message']?.toString() ?? 'Registered successfully');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _registering = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Event Details', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
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
              : _event == null
                  ? const Center(child: Text('Event not found'))
                  : SingleChildScrollView(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (_event!.imageUrl != null && _event!.imageUrl!.isNotEmpty)
                            ClipRRect(
                              borderRadius: BorderRadius.circular(16),
                              child: CachedNetworkImage(
                                imageUrl: ApiConfig.mediaUrl(_event!.imageUrl),
                                width: double.infinity,
                                height: 200,
                                fit: BoxFit.cover,
                              ),
                            ),
                          const SizedBox(height: 16),
                          Text(
                            _event!.title ?? 'Untitled',
                            style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 24),
                          ),
                          const SizedBox(height: 8),
                          if (_event!.organizer != null)
                            Text('Organizer: ${_event!.organizer}', style: GoogleFonts.inter(color: Colors.grey.shade700)),
                          if (_event!.venue != null) ...[
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                const Icon(Icons.location_on, size: 16, color: Colors.blueAccent),
                                const SizedBox(width: 4),
                                Expanded(child: Text(_event!.venue!, style: GoogleFonts.inter())),
                              ],
                            ),
                          ],
                          if (_event!.dateTime != null) ...[
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                const Icon(Icons.schedule, size: 16, color: Colors.blueAccent),
                                const SizedBox(width: 4),
                                Text(_event!.dateTime!, style: GoogleFonts.inter()),
                              ],
                            ),
                          ],
                          if (_event!.price != null || _event!.regularPrice != null) ...[
                            const SizedBox(height: 8),
                            Text(
                              _event!.price ?? '₹${_event!.regularPrice}',
                              style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.green.shade700),
                            ),
                          ],
                          const SizedBox(height: 16),
                          if (_event!.description != null)
                            Text(_event!.description!, style: GoogleFonts.inter(height: 1.5)),
                          const SizedBox(height: 24),
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton(
                              onPressed: _registering ? null : _register,
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.blueAccent,
                                padding: const EdgeInsets.symmetric(vertical: 14),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                              ),
                              child: _registering
                                  ? const SizedBox(
                                      height: 20,
                                      width: 20,
                                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                    )
                                  : Text(
                                      'Register',
                                      style: GoogleFonts.inter(fontWeight: FontWeight.bold, color: Colors.white),
                                    ),
                            ),
                          ),
                        ],
                      ),
                    ),
    );
  }
}
