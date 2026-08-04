import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../models/event_model.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import 'event_detail_page.dart';

class EventsPage extends StatefulWidget {
  const EventsPage({super.key});

  @override
  State<EventsPage> createState() => _EventsPageState();
}

class _EventsPageState extends State<EventsPage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  List<EventModel> _events = [];
  List<EventModel> _polls = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await AppApi.events();
      if (!mounted) return;
      setState(() {
        _events = (data['events'] as List? ?? [])
            .map((e) => EventModel.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList();
        _polls = (data['votingPolls'] as List? ?? [])
            .map((e) => EventModel.fromJson(Map<String, dynamic>.from(e as Map)))
            .toList();
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

  Widget _buildList(List<EventModel> items, {required bool isPoll}) {
    if (items.isEmpty) {
      return Center(child: Text('No ${isPoll ? 'polls' : 'events'} found', style: GoogleFonts.inter(color: Colors.grey)));
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: items.length,
        itemBuilder: (context, index) {
          final event = items[index];
          return _EventCard(event: event, isPoll: isPoll);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_error!, style: const TextStyle(color: Colors.red)),
            ElevatedButton(onPressed: _load, child: const Text('Retry')),
          ],
        ),
      );
    }

    return Column(
      children: [
        TabBar(
          controller: _tabCtrl,
          labelColor: Colors.blueAccent,
          tabs: [
            Tab(text: 'Events (${_events.length})'),
            Tab(text: 'Voting (${_polls.length})'),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabCtrl,
            children: [
              _buildList(_events, isPoll: false),
              _buildList(_polls, isPoll: true),
            ],
          ),
        ),
      ],
    );
  }
}

class _EventCard extends StatelessWidget {
  const _EventCard({required this.event, required this.isPoll});

  final EventModel event;
  final bool isPoll;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => EventDetailPage(eventId: event.id)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (event.imageUrl != null && event.imageUrl!.isNotEmpty)
              CachedNetworkImage(
                imageUrl: ApiConfig.mediaUrl(event.imageUrl),
                height: 140,
                width: double.infinity,
                fit: BoxFit.cover,
                errorWidget: (_, __, ___) => Container(
                  height: 100,
                  color: Colors.grey.shade200,
                  child: const Icon(Icons.event),
                ),
              ),
            Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          event.title ?? 'Untitled',
                          style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                      ),
                      if (isPoll)
                        Chip(
                          label: Text('${event.pollVotes ?? 0} votes', style: const TextStyle(fontSize: 11)),
                          visualDensity: VisualDensity.compact,
                        ),
                    ],
                  ),
                  if (event.venue != null) ...[
                    const SizedBox(height: 4),
                    Text(event.venue!, style: GoogleFonts.inter(fontSize: 13, color: Colors.grey.shade700)),
                  ],
                  if (event.dateTime != null) ...[
                    const SizedBox(height: 4),
                    Text(event.dateTime!, style: GoogleFonts.inter(fontSize: 12, color: Colors.grey)),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
