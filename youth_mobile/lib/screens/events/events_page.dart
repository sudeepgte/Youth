import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../models/event_model.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import 'event_detail_page.dart';

class EventsPage extends StatefulWidget {
  const EventsPage({super.key});

  @override
  State<EventsPage> createState() => _EventsPageState();
}

class _EventsPageState extends State<EventsPage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  final _searchCtrl = TextEditingController();
  List<EventModel> _events = [];
  List<EventModel> _polls = [];
  bool _loading = true;
  String? _error;
  String _category = 'All';

  static const _categories = [
    'All',
    'Tech',
    'Gaming',
    'Cultural',
    'Sports',
    'Music',
    'Art',
    'House Party',
    'Adventure',
    'Trekking',
    'Bike Riding',
  ];

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _tabCtrl.dispose();
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await AppApi.events(
        category: _category == 'All' ? null : _category,
        search: _searchCtrl.text.trim().isEmpty ? null : _searchCtrl.text.trim(),
      );
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
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: 80),
          Center(
            child: Text(
              'No ${isPoll ? 'polls' : 'events'} found',
              style: GoogleFonts.inter(color: AppTheme.textMuted),
            ),
          ),
        ],
      );
    }
    return RefreshIndicator(
      color: AppTheme.primary,
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        itemCount: items.length,
        itemBuilder: (context, index) {
          final event = items[index];
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _EventCard(event: event, isPoll: isPoll),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return DashboardScaffold(
      active: AppDrawerItem.events,
      title: 'Events',
      body: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
              child: TextField(
                controller: _searchCtrl,
                decoration: AppTheme.dashboardInput('Search venue or title…').copyWith(
                  prefixIcon: const Icon(Icons.search_rounded, color: Colors.black45),
                  suffixIcon: IconButton(
                    icon: const Icon(Icons.arrow_forward_rounded),
                    onPressed: _load,
                  ),
                ),
                onSubmitted: (_) => _load(),
              ),
            ),
            SizedBox(
              height: 40,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemCount: _categories.length,
                separatorBuilder: (_, _) => const SizedBox(width: 8),
                itemBuilder: (_, i) {
                  final c = _categories[i];
                  final selected = _category == c;
                  return ChoiceChip(
                    label: Text(c),
                    selected: selected,
                    onSelected: (_) {
                      setState(() => _category = c);
                      _load();
                    },
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
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFBFDBFE)),
                ),
                child: TabBar(
                  controller: _tabCtrl,
                  labelColor: AppTheme.primary,
                  unselectedLabelColor: AppTheme.textMuted,
                  indicatorColor: AppTheme.primary,
                  labelStyle: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 13),
                  tabs: [
                    Tab(text: 'Events (${_events.length})'),
                    Tab(text: 'Voting (${_polls.length})'),
                  ],
                ),
              ),
            ),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator(color: AppTheme.primary))
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
                      : TabBarView(
                          controller: _tabCtrl,
                          children: [
                            _buildList(_events, isPoll: false),
                            _buildList(_polls, isPoll: true),
                          ],
                        ),
            ),
          ],
        ),
    );
  }
}

class _EventCard extends StatefulWidget {
  const _EventCard({required this.event, required this.isPoll});

  final EventModel event;
  final bool isPoll;

  @override
  State<_EventCard> createState() => _EventCardState();
}

class _EventCardState extends State<_EventCard> {
  bool _voting = false;
  int _votes = 0;

  @override
  void initState() {
    super.initState();
    _votes = widget.event.pollVotes ?? 0;
  }

  Future<void> _vote() async {
    if (_voting) return;
    setState(() => _voting = true);
    try {
      final res = await AppApi.pollVote(widget.event.id);
      if (!mounted) return;
      setState(() => _votes = (res['pollVotes'] as num?)?.toInt() ?? (_votes + 1));
      AppTheme.showSuccess(context, res['message']?.toString() ?? 'Vote submitted');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _voting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final event = widget.event;
    final isPoll = widget.isPoll;
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => EventDetailPage(eventId: event.id)),
        ).then((_) {}),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Stack(
              children: [
                if (event.imageUrl != null && event.imageUrl!.isNotEmpty)
                  CachedNetworkImage(
                    imageUrl: ApiConfig.mediaUrl(event.imageUrl),
                    height: 150,
                    width: double.infinity,
                    fit: BoxFit.cover,
                    errorWidget: (_, _, _) => Container(
                      height: 100,
                      color: Colors.grey.shade200,
                      child: const Icon(Icons.event),
                    ),
                  )
                else
                  Container(
                    height: 88,
                    width: double.infinity,
                    color: AppTheme.primary.withValues(alpha: 0.08),
                    child: const Icon(Icons.event_rounded, color: AppTheme.primary, size: 36),
                  ),
                if (event.category != null)
                  Positioned(
                    left: 10,
                    top: 10,
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.black.withValues(alpha: 0.55),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        event.category!,
                        style: GoogleFonts.outfit(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w700),
                      ),
                    ),
                  ),
                Positioned(
                  right: 10,
                  top: 10,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.92),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      event.priceLabel(),
                      style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: const Color(0xFF059669), fontSize: 12),
                    ),
                  ),
                ),
              ],
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
                          style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16),
                        ),
                      ),
                      if (isPoll)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFF8E1),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            '$_votes votes',
                            style: GoogleFonts.inter(fontSize: 11, fontWeight: FontWeight.w600),
                          ),
                        ),
                    ],
                  ),
                  if (event.venue != null) ...[
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        Icon(Icons.place_outlined, size: 14, color: Colors.grey.shade600),
                        const SizedBox(width: 4),
                        Expanded(
                          child: Text(event.venue!, style: GoogleFonts.inter(fontSize: 13, color: Colors.grey.shade700)),
                        ),
                      ],
                    ),
                  ],
                  if (event.dateTime != null) ...[
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Icon(Icons.schedule_outlined, size: 14, color: Colors.grey.shade500),
                        const SizedBox(width: 4),
                        Expanded(
                          child: Text(event.dateTime!, style: GoogleFonts.inter(fontSize: 12, color: Colors.grey)),
                        ),
                      ],
                    ),
                  ],
                  if (event.registeredCount != null) ...[
                    const SizedBox(height: 6),
                    Text(
                      '${event.registeredCount} going'
                      '${event.spotsLeft != null ? ' · ${event.spotsLeft} spots left' : ''}',
                      style: GoogleFonts.outfit(fontSize: 12, fontWeight: FontWeight.w600, color: AppTheme.primary),
                    ),
                  ],
                  if (event.isRegistered)
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text('Registered', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: const Color(0xFF059669))),
                    ),
                  if (isPoll) ...[
                    const SizedBox(height: 10),
                    SizedBox(
                      width: double.infinity,
                      height: 42,
                      child: ElevatedButton(
                        onPressed: _voting ? null : _vote,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.primary,
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                        ),
                        child: _voting
                            ? const SizedBox(
                                height: 16,
                                width: 16,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                              )
                            : const Text('Vote'),
                      ),
                    ),
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
