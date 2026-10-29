import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import 'battle_detail_page.dart';

class BattlesPage extends StatefulWidget {
  const BattlesPage({super.key, this.openJoinOnStart = false, this.initialBattleId});

  final bool openJoinOnStart;
  final int? initialBattleId;

  @override
  State<BattlesPage> createState() => _BattlesPageState();
}

class _BattlesPageState extends State<BattlesPage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  List<Map<String, dynamic>> _active = [];
  List<Map<String, dynamic>> _completed = [];
  bool _loading = true;
  bool _busy = false;
  String? _error;

  static const _categories = [
    'Coding',
    'Music',
    'Dance',
    'Photography',
    'Singing',
    'Video Editing',
    'Drawing',
    'Fitness',
    'Gaming',
    'Meme',
    'AI Prompt',
    'Art',
    'Acting',
    'Fashion',
    'Cooking',
    'Writing',
    'Comedy',
  ];

  static const _categoryIcons = <String, IconData>{
    'Coding': Icons.code_rounded,
    'Music': Icons.music_note_rounded,
    'Dance': Icons.directions_run_rounded,
    'Photography': Icons.camera_alt_rounded,
    'Singing': Icons.mic_rounded,
    'Video Editing': Icons.videocam_rounded,
    'Drawing': Icons.brush_rounded,
    'Fitness': Icons.fitness_center_rounded,
    'Gaming': Icons.sports_esports_rounded,
    'Meme': Icons.emoji_emotions_rounded,
    'AI Prompt': Icons.smart_toy_rounded,
    'Art': Icons.palette_rounded,
    'Acting': Icons.theater_comedy_rounded,
    'Fashion': Icons.checkroom_rounded,
    'Cooking': Icons.restaurant_rounded,
    'Writing': Icons.edit_rounded,
    'Comedy': Icons.sentiment_very_satisfied_rounded,
  };

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _load().then((_) {
      if (!mounted) return;
      if (widget.initialBattleId != null) {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => BattleDetailPage(battleId: widget.initialBattleId!)),
        ).then((_) => _load());
      } else if (widget.openJoinOnStart) {
        _showJoinDialog();
      }
    });
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
      final data = await AppApi.battles();
      if (!mounted) return;
      setState(() {
        _active = (data['active'] as List? ?? [])
            .map((e) => Map<String, dynamic>.from(e as Map))
            .toList();
        _completed = (data['completed'] as List? ?? [])
            .map((e) => Map<String, dynamic>.from(e as Map))
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

  Future<void> _showJoinDialog() async {
    final codeCtrl = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Join via Code', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Enter the 6-character room code. Entry fee may apply.',
              style: GoogleFonts.inter(fontSize: 13, color: AppTheme.textMuted),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: codeCtrl,
              textCapitalization: TextCapitalization.characters,
              maxLength: 6,
              decoration: AppTheme.dashboardInput('Room code'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
            child: const Text('Join'),
          ),
        ],
      ),
    );
    final code = codeCtrl.text.trim().toUpperCase();
    codeCtrl.dispose();
    if (confirmed != true || code.isEmpty) return;

    setState(() => _busy = true);
    try {
      final result = await AppApi.joinBattle(roomCode: code);
      if (!mounted) return;
      if (result['needsPayment'] == true && result['battleId'] != null) {
        final pay = await showDialog<bool>(
          context: context,
          builder: (ctx) => AlertDialog(
            title: Text('Entry fee', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
            content: const Text('This battle has an entry fee. Confirm payment from your wallet to join?'),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
              ElevatedButton(
                onPressed: () => Navigator.pop(ctx, true),
                style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
                child: const Text('Pay & Join'),
              ),
            ],
          ),
        );
        if (pay == true) {
          await AppApi.processBattlePayment((result['battleId'] as num).toInt());
        } else {
          return;
        }
      }
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Joined battle');
      final battleId = (result['battleId'] as num?)?.toInt();
      await _load();
      if (!mounted) return;
      if (battleId != null) {
        await Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => BattleDetailPage(battleId: battleId)),
        );
        if (mounted) await _load();
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _showCreateFlow() async {
    final createdId = await showModalBottomSheet<int>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _CreateBattleSheet(categories: _categories, categoryIcons: _categoryIcons),
    );
    if (createdId != null && mounted) {
      await _load();
      await Navigator.push(
        context,
        MaterialPageRoute(builder: (_) => BattleDetailPage(battleId: createdId)),
      );
      await _load();
    }
  }

  Color _statusColor(String status) {
    switch (status.toUpperCase()) {
      case 'WAITING':
        return const Color(0xFFD97706);
      case 'ACTIVE':
        return const Color(0xFF16A34A);
      case 'VOTING':
        return const Color(0xFF7C3AED);
      case 'COMPLETED':
      case 'TIE':
        return AppTheme.textMuted;
      default:
        return AppTheme.primary;
    }
  }

  Widget _badge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withValues(alpha: 0.25)),
      ),
      child: Text(
        text,
        style: GoogleFonts.outfit(fontSize: 10, fontWeight: FontWeight.w800, color: color),
      ),
    );
  }

  String _durationLabel(Map<String, dynamic> b) {
    final mins = (b['durationMinutes'] as num?)?.toInt();
    if (mins != null && mins > 0) return '${mins}m';
    final hours = (b['durationHours'] as num?)?.toInt();
    if (hours != null) return '${hours}h';
    return '-';
  }

  Widget _battleCard(Map<String, dynamic> b, {required bool completed}) {
    final status = b['status']?.toString() ?? '';
    final category = b['category']?.toString() ?? 'Battle';
    final title = b['title']?.toString() ?? 'Battle';
    final mode = b['mode']?.toString() ?? 'ONLINE';
    final venue = b['venue']?.toString();
    final eventDate = b['eventDate']?.toString();
    final count = (b['participantCount'] as num?)?.toInt() ?? 0;
    final max = (b['maxParticipants'] as num?)?.toInt() ?? 0;
    final xp = (b['winnerXp'] as num?)?.toInt() ?? 0;
    final room = b['roomCode']?.toString() ?? '';
    final creator = b['creatorUsername']?.toString() ?? 'Host';
    final creatorPhoto = b['creatorPhotoUrl']?.toString();
    final winner = b['winnerUsername']?.toString();
    final prize1 = (b['prize1'] as num?)?.toDouble() ?? 0;

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: _busy
            ? null
            : () async {
                final id = (b['id'] as num?)?.toInt();
                if (id == null) return;
                await Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => BattleDetailPage(battleId: id)),
                );
                await _load();
              },
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: const Color(0xFFBFDBFE)),
            boxShadow: const [
              BoxShadow(color: Color(0x140F172A), blurRadius: 12, offset: Offset(0, 6)),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _badge(category, AppTheme.primary),
                  const Spacer(),
                  _badge(status, _statusColor(status)),
                ],
              ),
              const SizedBox(height: 10),
              Text(title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
              if (mode == 'OFFLINE' && venue != null && venue.isNotEmpty) ...[
                const SizedBox(height: 6),
                Row(
                  children: [
                    const Icon(Icons.place_outlined, size: 14, color: AppTheme.primary),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Text(venue, style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 10),
              Wrap(
                spacing: 12,
                runSpacing: 6,
                children: [
                  if (completed && winner != null)
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.workspace_premium, size: 14, color: Color(0xFFF5C542)),
                        const SizedBox(width: 4),
                        Text(
                          prize1 > 0 ? '$winner (₹${prize1.toStringAsFixed(0)})' : winner,
                          style: GoogleFonts.inter(fontSize: 12, fontWeight: FontWeight.w600),
                        ),
                      ],
                    )
                  else ...[
                    if (mode == 'OFFLINE' && eventDate != null && eventDate.isNotEmpty)
                      _metaChip(Icons.calendar_today_outlined, eventDate)
                    else
                      _metaChip(Icons.schedule_rounded, _durationLabel(b)),
                    _metaChip(Icons.people_outline, '$count/$max'),
                    if (xp > 0) _metaChip(Icons.emoji_events_outlined, '$xp XP'),
                  ],
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  CircleAvatar(
                    radius: 14,
                    backgroundColor: AppTheme.secondary.withValues(alpha: 0.4),
                    backgroundImage: creatorPhoto != null && creatorPhoto.isNotEmpty
                        ? CachedNetworkImageProvider(ApiConfig.mediaUrl(creatorPhoto))
                        : null,
                    child: creatorPhoto == null || creatorPhoto.isEmpty
                        ? Text(
                            creator.isNotEmpty ? creator[0].toUpperCase() : '?',
                            style: GoogleFonts.outfit(fontSize: 12, fontWeight: FontWeight.w700),
                          )
                        : null,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(creator, style: GoogleFonts.outfit(fontWeight: FontWeight.w600, fontSize: 13)),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF1F5F9),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: const Color(0xFFE2E8F0)),
                    ),
                    child: Text(
                      room,
                      style: GoogleFonts.outfit(
                        fontWeight: FontWeight.w800,
                        fontSize: 12,
                        letterSpacing: 1.2,
                        color: AppTheme.primary,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _metaChip(IconData icon, String text) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: AppTheme.textMuted),
        const SizedBox(width: 4),
        Text(text, style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
      ],
    );
  }

  Widget _battleList(List<Map<String, dynamic>> items, {required bool completed}) {
    if (items.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: 80),
          Icon(
            completed ? Icons.flag_rounded : Icons.sports_martial_arts_rounded,
            size: 48,
            color: AppTheme.textMuted.withValues(alpha: 0.5),
          ),
          const SizedBox(height: 12),
          Center(
            child: Text(
              completed ? 'No completed battles' : 'No active battles',
              style: GoogleFonts.outfit(color: AppTheme.textMuted, fontWeight: FontWeight.w600),
            ),
          ),
          const SizedBox(height: 6),
          Center(
            child: Text(
              completed ? 'Finished battles will appear here.' : 'Create one or join via room code.',
              style: GoogleFonts.inter(color: AppTheme.textMuted, fontSize: 13),
            ),
          ),
        ],
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
      itemCount: items.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (_, i) => _battleCard(items[i], completed: completed),
    );
  }

  @override
  Widget build(BuildContext context) {
    return DashboardScaffold(
      active: AppDrawerItem.battles,
      title: 'Battle Arena',
      body: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 12),
              child: Row(
                children: [
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: _busy ? null : _showCreateFlow,
                      icon: const Icon(Icons.add_rounded, size: 18),
                      label: Text('Create Battle', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primary,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: _busy ? null : _showJoinDialog,
                      icon: const Icon(Icons.login_rounded, size: 18),
                      label: Text('Join via Code', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppTheme.primary,
                        side: const BorderSide(color: AppTheme.primary),
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 16),
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
                indicatorSize: TabBarIndicatorSize.tab,
                labelStyle: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 13),
                tabs: [
                  Tab(text: 'Active (${_active.length})'),
                  Tab(text: 'Completed (${_completed.length})'),
                ],
              ),
            ),
            if (_busy) const LinearProgressIndicator(minHeight: 2, color: AppTheme.primary),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator(color: AppTheme.primary))
                  : _error != null
                      ? Center(
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(_error!, style: const TextStyle(color: Colors.red)),
                              TextButton(onPressed: _load, child: const Text('Retry')),
                            ],
                          ),
                        )
                      : RefreshIndicator(
                          color: AppTheme.primary,
                          onRefresh: _load,
                          child: TabBarView(
                            controller: _tabCtrl,
                            children: [
                              _battleList(_active, completed: false),
                              _battleList(_completed, completed: true),
                            ],
                          ),
                        ),
            ),
          ],
        ),
    );
  }
}

class _CreateBattleSheet extends StatefulWidget {
  const _CreateBattleSheet({required this.categories, required this.categoryIcons});

  final List<String> categories;
  final Map<String, IconData> categoryIcons;

  @override
  State<_CreateBattleSheet> createState() => _CreateBattleSheetState();
}

class _CreateBattleSheetState extends State<_CreateBattleSheet> {
  final _titleCtrl = TextEditingController();
  final _entryFeeCtrl = TextEditingController(text: '0');
  final _prize1Ctrl = TextEditingController(text: '0');
  final _prize2Ctrl = TextEditingController(text: '0');
  final _prize3Ctrl = TextEditingController(text: '0');
  final _venueCtrl = TextEditingController();
  final _eventDateCtrl = TextEditingController();
  final _eventTimeCtrl = TextEditingController();
  final _judgeCtrl = TextEditingController(text: '70');
  final _audienceCtrl = TextEditingController(text: '30');
  final _maxCtrl = TextEditingController(text: '2');

  int _step = 1;
  String? _category;
  String _mode = 'ONLINE';
  String _durationType = 'hours';
  int _durationHours = 24;
  int _durationMinutes = 5;
  bool _busy = false;
  bool _prizesManual = false;

  @override
  void dispose() {
    _titleCtrl.dispose();
    _entryFeeCtrl.dispose();
    _prize1Ctrl.dispose();
    _prize2Ctrl.dispose();
    _prize3Ctrl.dispose();
    _venueCtrl.dispose();
    _eventDateCtrl.dispose();
    _eventTimeCtrl.dispose();
    _judgeCtrl.dispose();
    _audienceCtrl.dispose();
    _maxCtrl.dispose();
    super.dispose();
  }

  bool get _isLive => _durationType == 'minutes';

  int get _maxParticipants {
    if (_isLive) return 2;
    return int.tryParse(_maxCtrl.text.trim()) ?? 2;
  }

  void _recalcPrizes() {
    if (_prizesManual) return;
    final fee = double.tryParse(_entryFeeCtrl.text.trim()) ?? 0;
    final max = _maxParticipants;
    final pool = fee * max;
    if (_isLive || max <= 2) {
      _prize1Ctrl.text = (pool * 0.8).toStringAsFixed(2);
      _prize2Ctrl.text = (pool * 0.2).toStringAsFixed(2);
      _prize3Ctrl.text = '0';
    } else {
      _prize1Ctrl.text = (pool * 0.6).toStringAsFixed(2);
      _prize2Ctrl.text = (pool * 0.3).toStringAsFixed(2);
      _prize3Ctrl.text = (pool * 0.1).toStringAsFixed(2);
    }
    setState(() {});
  }

  void _syncWeights({required bool fromJudge}) {
    if (fromJudge) {
      final j = double.tryParse(_judgeCtrl.text.trim()) ?? 70;
      _audienceCtrl.text = (100 - j).clamp(0, 100).toStringAsFixed(0);
    } else {
      final a = double.tryParse(_audienceCtrl.text.trim()) ?? 30;
      _judgeCtrl.text = (100 - a).clamp(0, 100).toStringAsFixed(0);
    }
    setState(() {});
  }

  Future<void> _submit() async {
    final title = _titleCtrl.text.trim();
    if (title.isEmpty || _category == null) {
      AppTheme.showError(context, 'Title and category are required');
      return;
    }
    if (_mode == 'OFFLINE' && _venueCtrl.text.trim().isEmpty) {
      AppTheme.showError(context, 'Venue is required for offline battles');
      return;
    }

    final fee = double.tryParse(_entryFeeCtrl.text.trim()) ?? 0;
    final p1 = double.tryParse(_prize1Ctrl.text.trim()) ?? 0;
    final p2 = double.tryParse(_prize2Ctrl.text.trim()) ?? 0;
    final p3 = _isLive ? 0.0 : (double.tryParse(_prize3Ctrl.text.trim()) ?? 0);
    final max = _maxParticipants;
    if (p1 + p2 + p3 > fee * max + 0.01) {
      AppTheme.showError(context, 'Prize pool cannot exceed entry fee × max participants');
      return;
    }

    final fields = <String, dynamic>{
      'title': title,
      'category': _category,
      'mode': _mode,
      'entryFee': fee.toString(),
      'prize1': p1.toString(),
      'prize2': p2.toString(),
      'prize3': p3.toString(),
      'maxParticipants': max.toString(),
      'durationType': _durationType,
    };
    if (_isLive) {
      fields['durationMinutes'] = _durationMinutes.toString();
      fields['durationHours'] = '1';
    } else {
      fields['durationHours'] = _durationHours.toString();
    }
    if (_mode == 'OFFLINE') {
      fields['venue'] = _venueCtrl.text.trim();
      fields['eventDate'] = _eventDateCtrl.text.trim();
      fields['eventTime'] = _eventTimeCtrl.text.trim();
      fields['judgeWeight'] = _judgeCtrl.text.trim();
      fields['audienceWeight'] = _audienceCtrl.text.trim();
    }

    setState(() => _busy = true);
    try {
      final res = await AppApi.createBattle(fields);
      if (!mounted) return;
      final id = (res['battleId'] as num?)?.toInt();
      if (id != null) {
        Navigator.pop(context, id);
      } else {
        AppTheme.showSuccess(context, 'Battle created');
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottom = MediaQuery.of(context).viewInsets.bottom;
    return Padding(
      padding: EdgeInsets.only(bottom: bottom),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.92,
        child: Column(
          children: [
            const SizedBox(height: 8),
            Container(width: 40, height: 4, decoration: BoxDecoration(color: Colors.black26, borderRadius: BorderRadius.circular(4))),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 8, 8),
              child: Row(
                children: [
                  Text(
                    _step == 1 ? 'Create Battle' : 'Set Rules',
                    style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 20),
                  ),
                  const Spacer(),
                  IconButton(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.close)),
                ],
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _stepDot(1),
                Container(width: 28, height: 2, color: _step >= 2 ? AppTheme.primary : Colors.black12),
                _stepDot(2),
              ],
            ),
            const SizedBox(height: 8),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                child: _step == 1 ? _buildStep1() : _buildStep2(),
              ),
            ),
            SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                child: Row(
                  children: [
                    if (_step == 2)
                      Expanded(
                        child: OutlinedButton(
                          onPressed: _busy ? null : () => setState(() => _step = 1),
                          child: const Text('Back'),
                        ),
                      ),
                    if (_step == 2) const SizedBox(width: 10),
                    Expanded(
                      flex: 2,
                      child: ElevatedButton(
                        onPressed: _busy
                            ? null
                            : () {
                                if (_step == 1) {
                                  if (_titleCtrl.text.trim().isEmpty || _category == null) {
                                    AppTheme.showError(context, 'Enter a title and pick a category');
                                    return;
                                  }
                                  setState(() => _step = 2);
                                  _recalcPrizes();
                                } else {
                                  _submit();
                                }
                              },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.primary,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        child: _busy
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                              )
                            : Text(_step == 1 ? 'Next: Set Rules' : 'Create Battle'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _stepDot(int n) {
    final active = _step >= n;
    return Container(
      width: 10,
      height: 10,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: active ? AppTheme.primary : Colors.black12,
      ),
    );
  }

  Widget _buildStep1() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Set up a creative challenge', style: GoogleFonts.inter(color: AppTheme.textMuted)),
        const SizedBox(height: 14),
        TextField(
          controller: _titleCtrl,
          maxLength: 100,
          decoration: AppTheme.dashboardInput('Battle Title *'),
        ),
        const SizedBox(height: 8),
        Text('Category', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: widget.categories.map((c) {
            final selected = _category == c;
            return InkWell(
              onTap: () => setState(() => _category = c),
              borderRadius: BorderRadius.circular(12),
              child: Container(
                width: (MediaQuery.of(context).size.width - 48) / 3 - 6,
                padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 6),
                decoration: BoxDecoration(
                  color: selected ? AppTheme.primary.withValues(alpha: 0.12) : const Color(0xFFF8FAFC),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: selected ? AppTheme.primary : const Color(0xFFE2E8F0)),
                ),
                child: Column(
                  children: [
                    Icon(widget.categoryIcons[c] ?? Icons.category, color: selected ? AppTheme.primary : AppTheme.textMuted, size: 20),
                    const SizedBox(height: 6),
                    Text(
                      c,
                      textAlign: TextAlign.center,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: GoogleFonts.outfit(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: selected ? AppTheme.primary : AppTheme.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildStep2() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        DropdownButtonFormField<String>(
          value: _mode,
          decoration: AppTheme.dashboardInput('Battle Mode'),
          items: const [
            DropdownMenuItem(value: 'ONLINE', child: Text('Online (submissions / live)')),
            DropdownMenuItem(value: 'OFFLINE', child: Text('Offline (venue & check-in)')),
          ],
          onChanged: (v) {
            if (v != null) setState(() => _mode = v);
          },
        ),
        if (_mode == 'OFFLINE') ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: const Color(0xFFBFDBFE), style: BorderStyle.solid),
              color: const Color(0xFFF0F9FF),
            ),
            child: Column(
              children: [
                TextField(controller: _venueCtrl, decoration: AppTheme.dashboardInput('Venue / Location *')),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(child: TextField(controller: _eventDateCtrl, decoration: AppTheme.dashboardInput('Event Date'))),
                    const SizedBox(width: 8),
                    Expanded(child: TextField(controller: _eventTimeCtrl, decoration: AppTheme.dashboardInput('Event Time'))),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _judgeCtrl,
                        keyboardType: TextInputType.number,
                        decoration: AppTheme.dashboardInput('Judge Weight %'),
                        onChanged: (_) => _syncWeights(fromJudge: true),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _audienceCtrl,
                        keyboardType: TextInputType.number,
                        decoration: AppTheme.dashboardInput('Audience Weight %'),
                        onChanged: (_) => _syncWeights(fromJudge: false),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
        const SizedBox(height: 12),
        TextField(
          controller: _entryFeeCtrl,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: AppTheme.dashboardInput('Entry fee (₹)'),
          onChanged: (_) => _recalcPrizes(),
        ),
        const SizedBox(height: 12),
        Text('Prizes', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _prize1Ctrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: AppTheme.dashboardInput('1st'),
                onChanged: (_) => setState(() => _prizesManual = true),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: TextField(
                controller: _prize2Ctrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: AppTheme.dashboardInput('2nd'),
                onChanged: (_) => setState(() => _prizesManual = true),
              ),
            ),
            if (!_isLive) ...[
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: _prize3Ctrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: AppTheme.dashboardInput('3rd'),
                  onChanged: (_) => setState(() => _prizesManual = true),
                ),
              ),
            ],
          ],
        ),
        const SizedBox(height: 12),
        Text('Duration', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        Row(
          children: [
            ChoiceChip(
              label: const Text('Hours'),
              selected: _durationType == 'hours',
              onSelected: (_) {
                setState(() {
                  _durationType = 'hours';
                  _prizesManual = false;
                });
                _recalcPrizes();
              },
            ),
            const SizedBox(width: 8),
            ChoiceChip(
              label: const Text('Live (minutes)'),
              selected: _durationType == 'minutes',
              onSelected: (_) {
                setState(() {
                  _durationType = 'minutes';
                  _maxCtrl.text = '2';
                  _prizesManual = false;
                });
                _recalcPrizes();
              },
            ),
          ],
        ),
        const SizedBox(height: 10),
        if (_isLive)
          DropdownButtonFormField<int>(
            value: _durationMinutes,
            decoration: AppTheme.dashboardInput('Live duration'),
            items: const [1, 3, 5, 10]
                .map((m) => DropdownMenuItem(value: m, child: Text('$m minutes')))
                .toList(),
            onChanged: (v) {
              if (v != null) setState(() => _durationMinutes = v);
            },
          )
        else
          DropdownButtonFormField<int>(
            value: _durationHours,
            decoration: AppTheme.dashboardInput('Duration (hours)'),
            items: const [1, 6, 12, 24, 48, 72]
                .map((h) => DropdownMenuItem(value: h, child: Text('$h hours')))
                .toList(),
            onChanged: (v) {
              if (v != null) setState(() => _durationHours = v);
            },
          ),
        const SizedBox(height: 12),
        TextField(
          controller: _maxCtrl,
          enabled: !_isLive,
          keyboardType: TextInputType.number,
          decoration: AppTheme.dashboardInput(_isLive ? 'Max participants (forced to 2 for live)' : 'Max participants'),
          onChanged: (_) {
            _prizesManual = false;
            _recalcPrizes();
          },
        ),
        const SizedBox(height: 12),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: const Color(0xFFF8FAFC),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: const Color(0xFFE2E8F0)),
          ),
          child: Text(
            _isLive
                ? 'Live battles are 1v1 with camera/mic engagement. Voting is public & weighted.'
                : 'Voting is public & weighted. Winner = highest combined score.',
            style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted, height: 1.4),
          ),
        ),
      ],
    );
  }
}
