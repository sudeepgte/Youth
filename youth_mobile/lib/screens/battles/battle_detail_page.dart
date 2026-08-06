import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import 'battle_live_page.dart';

class BattleDetailPage extends StatefulWidget {
  const BattleDetailPage({super.key, required this.battleId});

  final int battleId;

  @override
  State<BattleDetailPage> createState() => _BattleDetailPageState();
}

class _BattleDetailPageState extends State<BattleDetailPage> {
  Map<String, dynamic>? _battle;
  bool _loading = true;
  bool _busy = false;
  String? _error;

  final _submitUrlCtrl = TextEditingController();
  final _secondaryUrlCtrl = TextEditingController();
  final _descCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _submitUrlCtrl.dispose();
    _secondaryUrlCtrl.dispose();
    _descCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await AppApi.battleDetail(widget.battleId);
      if (!mounted) return;
      setState(() {
        _battle = data;
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

  Future<void> _run(Future<void> Function() action, {String? success}) async {
    setState(() => _busy = true);
    try {
      await action();
      if (!mounted) return;
      if (success != null) AppTheme.showSuccess(context, success);
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  bool get _isLive {
    final mins = (_battle?['durationMinutes'] as num?)?.toInt();
    return mins != null && mins > 0;
  }

  String get _status => (_battle?['status'] ?? '').toString().toUpperCase();
  bool get _isCreator => _battle?['isCreator'] == true;
  bool get _isParticipant => _battle?['isParticipant'] == true;
  bool get _hasVoted => _battle?['hasVoted'] == true;
  bool get _hasSubmitted => _battle?['hasSubmitted'] == true;

  Color _statusColor(String status) {
    switch (status) {
      case 'WAITING':
        return const Color(0xFFD97706);
      case 'ACTIVE':
        return const Color(0xFF16A34A);
      case 'VOTING':
        return const Color(0xFF7C3AED);
      default:
        return AppTheme.textMuted;
    }
  }

  Widget _chip(String label, {Color? color}) {
    final c = color ?? AppTheme.primary;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: c.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: c.withValues(alpha: 0.25)),
      ),
      child: Text(label, style: GoogleFonts.outfit(fontSize: 11, fontWeight: FontWeight.w800, color: c)),
    );
  }

  Future<void> _join() async {
    final fee = (_battle?['entryFee'] as num?)?.toDouble() ?? 0;
    final mode = _battle?['mode']?.toString() ?? 'ONLINE';
    final room = _battle?['roomCode']?.toString() ?? '';

    if (fee > 0) {
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text('Pay & Join', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
          content: Text('Entry fee ₹${fee.toStringAsFixed(0)}. Confirm payment to join?'),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
              child: Text('Pay ₹${fee.toStringAsFixed(0)}'),
            ),
          ],
        ),
      );
      if (ok != true) return;
      await _run(() async {
        await AppApi.processBattlePayment(widget.battleId);
      }, success: 'Joined battle');
      return;
    }

    if (mode == 'OFFLINE') {
      await _run(() async {
        await AppApi.registerOfflineBattle(widget.battleId);
      }, success: 'Registered');
      return;
    }

    await _run(() async {
      final res = await AppApi.joinBattle(roomCode: room);
      if (res['needsPayment'] == true) {
        await AppApi.processBattlePayment(widget.battleId);
      }
    }, success: 'Joined battle');
  }

  Future<void> _openLive() async {
    final title = _battle?['title']?.toString() ?? 'Battle Live';
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => BattleLivePage(battleId: widget.battleId, title: title),
      ),
    );
    await _load();
  }

  Future<void> _submitEntry() async {
    final url = _submitUrlCtrl.text.trim();
    if (url.isEmpty) {
      AppTheme.showError(context, 'Submission URL is required');
      return;
    }
    await _run(() async {
      await AppApi.submitBattleEntry(
        widget.battleId,
        submissionUrl: url,
        description: _descCtrl.text.trim(),
        secondaryUrl: _secondaryUrlCtrl.text.trim(),
      );
      _submitUrlCtrl.clear();
      _secondaryUrlCtrl.clear();
      _descCtrl.clear();
    }, success: 'Entry submitted');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Battle', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
        backgroundColor: Colors.white,
        foregroundColor: AppTheme.textPrimary,
        elevation: 0,
        actions: [
          if (_isCreator && (_status == 'WAITING' || _status == 'ACTIVE'))
            IconButton(
              tooltip: 'Delete',
              onPressed: _busy
                  ? null
                  : () async {
                      final ok = await showDialog<bool>(
                        context: context,
                        builder: (ctx) => AlertDialog(
                          title: const Text('Delete battle?'),
                          content: const Text('This cannot be undone.'),
                          actions: [
                            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
                            TextButton(
                              onPressed: () => Navigator.pop(ctx, true),
                              child: const Text('Delete', style: TextStyle(color: Colors.red)),
                            ),
                          ],
                        ),
                      );
                      if (ok == true && mounted) {
                        await _run(() async {
                          await AppApi.deleteBattle(widget.battleId);
                          if (mounted) Navigator.pop(context);
                        }, success: 'Battle deleted');
                      }
                    },
              icon: const Icon(Icons.delete_outline),
            ),
        ],
      ),
      body: _loading
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
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(16, 12, 16, 40),
                    children: [
                      if (_busy) const LinearProgressIndicator(minHeight: 2, color: AppTheme.primary),
                      _headerCard(),
                      const SizedBox(height: 12),
                      _metaCard(),
                      const SizedBox(height: 12),
                      _actionsCard(),
                      if ((_battle?['mode']?.toString() ?? '') == 'OFFLINE' && _isParticipant) ...[
                        const SizedBox(height: 12),
                        _qrPassCard(),
                      ],
                      const SizedBox(height: 12),
                      _participantsCard(),
                      if (!_isLive && (_status == 'ACTIVE' || _status == 'VOTING' || _status == 'COMPLETED' || _status == 'TIE')) ...[
                        const SizedBox(height: 12),
                        _submissionsCard(),
                      ],
                      if (_status == 'VOTING' || _status == 'COMPLETED' || _status == 'TIE') ...[
                        const SizedBox(height: 12),
                        _leaderboardCard(),
                      ],
                      if (_status == 'COMPLETED' || _status == 'TIE') ...[
                        const SizedBox(height: 12),
                        _winnersCard(),
                      ],
                    ],
                  ),
                ),
    );
  }

  Widget _headerCard() {
    final b = _battle!;
    final title = b['title']?.toString() ?? 'Battle';
    final category = b['category']?.toString() ?? '';
    final room = b['roomCode']?.toString() ?? '';

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              if (category.isNotEmpty) _chip(category),
              const SizedBox(width: 8),
              _chip(_status, color: _statusColor(_status)),
              if (_isLive) ...[
                const SizedBox(width: 8),
                _chip('LIVE', color: Colors.red),
              ],
            ],
          ),
          const SizedBox(height: 12),
          Text(title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 24)),
          const SizedBox(height: 10),
          InkWell(
            onTap: () async {
              await Clipboard.setData(ClipboardData(text: room));
              if (mounted) AppTheme.showSuccess(context, 'Room code copied');
            },
            child: Row(
              children: [
                Text('Room code: ', style: GoogleFonts.inter(color: AppTheme.textMuted)),
                Text(
                  room,
                  style: GoogleFonts.outfit(
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.5,
                    color: AppTheme.primary,
                  ),
                ),
                const SizedBox(width: 6),
                const Icon(Icons.copy_rounded, size: 16, color: AppTheme.primary),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _metaCard() {
    final b = _battle!;
    final mode = b['mode']?.toString() ?? 'ONLINE';
    final count = (b['participantCount'] as num?)?.toInt() ?? 0;
    final max = (b['maxParticipants'] as num?)?.toInt() ?? 0;
    final fee = (b['entryFee'] as num?)?.toDouble() ?? 0;
    final p1 = (b['prize1'] as num?)?.toDouble() ?? 0;
    final p2 = (b['prize2'] as num?)?.toDouble() ?? 0;
    final p3 = (b['prize3'] as num?)?.toDouble() ?? 0;
    final xp = (b['winnerXp'] as num?)?.toInt() ?? 0;
    final mins = (b['durationMinutes'] as num?)?.toInt();
    final hours = (b['durationHours'] as num?)?.toInt();
    final endsAt = b['endsAt']?.toString();
    final venue = b['venue']?.toString();
    final eventDate = b['eventDate']?.toString();
    final eventTime = b['eventTime']?.toString();
    final jw = (b['judgeWeight'] as num?)?.toDouble();
    final aw = (b['audienceWeight'] as num?)?.toDouble();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Details', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
          const SizedBox(height: 10),
          _metaRow('Mode', mode),
          _metaRow(
            'Duration',
            mins != null && mins > 0 ? '$mins minutes' : '${hours ?? '-'} hours',
          ),
          _metaRow('Players', '$count / $max'),
          if (xp > 0) _metaRow('Winner XP', '$xp'),
          if (fee > 0) _metaRow('Entry fee', '₹${fee.toStringAsFixed(0)}'),
          if (p1 > 0 || p2 > 0 || p3 > 0)
            _metaRow(
              'Prizes',
              [
                if (p1 > 0) '1st ₹${p1.toStringAsFixed(0)}',
                if (p2 > 0) '2nd ₹${p2.toStringAsFixed(0)}',
                if (p3 > 0) '3rd ₹${p3.toStringAsFixed(0)}',
              ].join(' · '),
            ),
          if (endsAt != null && endsAt.isNotEmpty) _metaRow('Ends at', endsAt.replaceFirst('T', ' ')),
          if (mode == 'OFFLINE') ...[
            if (venue != null && venue.isNotEmpty) _metaRow('Venue', venue),
            if (eventDate != null && eventDate.isNotEmpty) _metaRow('Date', eventDate),
            if (eventTime != null && eventTime.isNotEmpty) _metaRow('Time', eventTime),
            if (jw != null) _metaRow('Judge / Audience', '${jw.toStringAsFixed(0)}% / ${aw?.toStringAsFixed(0) ?? 0}%'),
          ],
        ],
      ),
    );
  }

  Widget _metaRow(String k, String v) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(k, style: GoogleFonts.inter(color: AppTheme.textMuted, fontSize: 13)),
          ),
          Expanded(child: Text(v, style: GoogleFonts.outfit(fontWeight: FontWeight.w600, fontSize: 13))),
        ],
      ),
    );
  }

  Widget _actionsCard() {
    final count = (_battle?['participantCount'] as num?)?.toInt() ?? 0;
    final children = <Widget>[];

    if (!_isParticipant && _status == 'WAITING') {
      children.add(
        ElevatedButton(
          onPressed: _busy ? null : _join,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.primary,
            foregroundColor: Colors.white,
            minimumSize: const Size.fromHeight(46),
          ),
          child: Text(_battle?['mode'] == 'OFFLINE' ? 'Register & Get Pass' : 'Join Battle'),
        ),
      );
    }

    if (_status != 'WAITING' && !_isParticipant && !_isCreator) {
      children.add(
        Text('Joining is closed', style: GoogleFonts.inter(color: AppTheme.textMuted)),
      );
    }

    if (_isCreator && _status == 'WAITING') {
      children.add(
        ElevatedButton(
          onPressed: _busy || count < 2
              ? null
              : () => _run(() => AppApi.startBattle(widget.battleId), success: 'Battle started'),
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.primary,
            foregroundColor: Colors.white,
            minimumSize: const Size.fromHeight(46),
          ),
          child: Text(count < 2 ? 'Start (need 2+ players)' : 'Start Battle'),
        ),
      );
    }

    if (_status == 'ACTIVE' && _isLive) {
      children.add(
        ElevatedButton.icon(
          onPressed: _busy ? null : _openLive,
          icon: const Icon(Icons.videocam_rounded),
          label: Text(_isParticipant || _isCreator ? 'Go Live' : 'Watch Live'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.redAccent,
            foregroundColor: Colors.white,
            minimumSize: const Size.fromHeight(46),
          ),
        ),
      );
    }

    if (_isCreator && _status == 'ACTIVE' && !_isLive) {
      children.add(
        OutlinedButton(
          onPressed: _busy
              ? null
              : () => _run(() => AppApi.startBattleVoting(widget.battleId), success: 'Voting opened'),
          style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(46)),
          child: const Text('Open Voting'),
        ),
      );
    }

    if (_isCreator && _status == 'VOTING') {
      children.add(
        ElevatedButton(
          onPressed: _busy
              ? null
              : () => _run(() => AppApi.endBattle(widget.battleId), success: 'Battle ended'),
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.primary,
            foregroundColor: Colors.white,
            minimumSize: const Size.fromHeight(46),
          ),
          child: const Text('End & Declare Winner'),
        ),
      );
    }

    if (_isParticipant && !_isCreator && (_status == 'WAITING' || _status == 'ACTIVE')) {
      children.add(
        TextButton(
          onPressed: _busy
              ? null
              : () => _run(() async {
                    await AppApi.leaveBattle(widget.battleId);
                    if (mounted) Navigator.pop(context);
                  }, success: 'Left battle'),
          child: const Text('Leave battle', style: TextStyle(color: Colors.red)),
        ),
      );
    }

    if (_status == 'ACTIVE' && !_isLive && _isParticipant && !_hasSubmitted) {
      children.add(const SizedBox(height: 8));
      children.add(
        Text('Submit your entry', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 15)),
      );
      children.add(const SizedBox(height: 8));
      children.add(TextField(controller: _submitUrlCtrl, decoration: AppTheme.dashboardInput('Submission URL *')));
      children.add(const SizedBox(height: 8));
      children.add(TextField(controller: _secondaryUrlCtrl, decoration: AppTheme.dashboardInput('Secondary URL (optional)')));
      children.add(const SizedBox(height: 8));
      children.add(
        TextField(
          controller: _descCtrl,
          maxLines: 3,
          decoration: AppTheme.dashboardInput('Description (optional)'),
        ),
      );
      children.add(const SizedBox(height: 8));
      children.add(
        ElevatedButton(
          onPressed: _busy ? null : _submitEntry,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.primary,
            foregroundColor: Colors.white,
            minimumSize: const Size.fromHeight(46),
          ),
          child: const Text('Submit Entry'),
        ),
      );
    }

    if (children.isEmpty) {
      return const SizedBox.shrink();
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text('Actions', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
          const SizedBox(height: 10),
          ...children.map((w) => Padding(padding: const EdgeInsets.only(bottom: 8), child: w)),
        ],
      ),
    );
  }

  Widget _qrPassCard() {
    final code = _battle?['myQrPassCode']?.toString() ?? '';
    final seat = _battle?['mySeatNumber']?.toString();
    final num = _battle?['myParticipantNumber']?.toString();
    final checkedIn = _battle?['myCheckedIn'] == true;
    final short = code.length > 8 ? code.substring(0, 8).toUpperCase() : code.toUpperCase();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFBFDBFE)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text('QR Entry Pass', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
              const Spacer(),
              _chip(checkedIn ? 'CHECKED IN' : 'REGISTERED', color: checkedIn ? Colors.teal : AppTheme.primary),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 20),
            decoration: BoxDecoration(
              color: const Color(0xFFEFF6FF),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Column(
              children: [
                const Icon(Icons.qr_code_2, size: 72, color: AppTheme.primary),
                const SizedBox(height: 8),
                Text(
                  short.isEmpty ? '—' : short,
                  style: GoogleFonts.outfit(fontWeight: FontWeight.w900, letterSpacing: 2, fontSize: 22),
                ),
                if (code.isNotEmpty)
                  TextButton.icon(
                    onPressed: () {
                      Clipboard.setData(ClipboardData(text: code));
                      AppTheme.showSuccess(context, 'Pass code copied');
                    },
                    icon: const Icon(Icons.copy, size: 16),
                    label: const Text('Copy full code'),
                  ),
              ],
            ),
          ),
          if (seat != null || num != null) ...[
            const SizedBox(height: 10),
            Text(
              [
                if (seat != null) 'Seat $seat',
                if (num != null) 'Participant #$num',
              ].join(' · '),
              style: GoogleFonts.inter(color: AppTheme.textMuted),
            ),
          ],
        ],
      ),
    );
  }

  Widget _participantsCard() {
    final parts = (_battle?['participants'] as List? ?? [])
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
    final mode = _battle?['mode']?.toString() ?? 'ONLINE';
    final checkedInCount = parts.where((p) => p['checkedIn'] == true).length;
    final absent = parts.length - checkedInCount;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Participants (${parts.length})', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
          if (mode == 'OFFLINE' && parts.isNotEmpty) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                _chip('${parts.length} registered', color: AppTheme.primary),
                _chip('$checkedInCount checked in', color: Colors.teal),
                _chip('$absent absent', color: Colors.orange.shade800),
              ],
            ),
          ],
          const SizedBox(height: 10),
          if (parts.isEmpty)
            Text('No participants yet', style: GoogleFonts.inter(color: AppTheme.textMuted))
          else
            ...parts.map((p) {
              final name = p['username']?.toString() ?? 'User';
              final photo = p['photoUrl']?.toString();
              final isHost = p['isHost'] == true;
              final seat = p['seatNumber']?.toString();
              final pNum = p['participantNumber']?.toString();
              final checkedIn = p['checkedIn'] == true;
              final pid = (p['id'] as num?)?.toInt();
              final showVote = _status == 'VOTING' && !_hasVoted && pid != null;
              final canCheckIn = mode == 'OFFLINE' && _isCreator && !checkedIn && pid != null;

              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 18,
                      backgroundColor: AppTheme.secondary.withValues(alpha: 0.35),
                      backgroundImage: photo != null && photo.isNotEmpty
                          ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                          : null,
                      child: photo == null || photo.isEmpty
                          ? Text(name.isNotEmpty ? name[0].toUpperCase() : '?')
                          : null,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Flexible(
                                child: Text(name, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                              ),
                              if (isHost) ...[
                                const SizedBox(width: 6),
                                _chip('HOST', color: AppTheme.gold),
                              ],
                              if (mode == 'OFFLINE' && checkedIn) ...[
                                const SizedBox(width: 6),
                                _chip('IN', color: Colors.teal),
                              ],
                            ],
                          ),
                          if (mode == 'OFFLINE')
                            Text(
                              [
                                if (seat != null) 'Seat $seat',
                                if (pNum != null) '#$pNum',
                              ].join(' · '),
                              style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted),
                            ),
                        ],
                      ),
                    ),
                    if (canCheckIn)
                      TextButton(
                        onPressed: _busy
                            ? null
                            : () => _run(
                                  () => AppApi.checkInBattleParticipant(widget.battleId, pid),
                                  success: 'Checked in',
                                ),
                        child: const Text('Check In'),
                      ),
                    if (showVote)
                      TextButton(
                        onPressed: _busy
                            ? null
                            : () => _run(
                                  () => AppApi.voteBattleParticipant(widget.battleId, pid),
                                  success: 'Vote cast',
                                ),
                        child: const Text('Vote'),
                      ),
                  ],
                ),
              );
            }),
        ],
      ),
    );
  }

  Widget _submissionsCard() {
    final subs = (_battle?['submissions'] as List? ?? [])
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Submissions', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
          const SizedBox(height: 10),
          if (subs.isEmpty)
            Text('No submissions yet', style: GoogleFonts.inter(color: AppTheme.textMuted))
          else
            ...subs.map((s) {
              final id = (s['id'] as num?)?.toInt();
              final name = s['username']?.toString() ?? 'User';
              final url = s['submissionUrl']?.toString() ?? '';
              final desc = s['description']?.toString() ?? '';
              final votes = (s['voteCount'] as num?)?.toInt() ?? 0;
              final canVote = _status == 'VOTING' && !_hasVoted && id != null && url != 'Direct Vote';

              return Container(
                margin: const EdgeInsets.only(bottom: 10),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFE2E8F0)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(name, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                        ),
                        Text('$votes votes', style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
                      ],
                    ),
                    if (desc.isNotEmpty && desc != 'Auto-generated submission' && desc != 'Direct participant vote')
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Text(desc, style: GoogleFonts.inter(fontSize: 13)),
                      ),
                    if (url.isNotEmpty && url != 'Direct Vote')
                      TextButton(
                        onPressed: () async {
                          await Clipboard.setData(ClipboardData(text: url));
                          if (mounted) AppTheme.showSuccess(context, 'Submission link copied');
                        },
                        child: const Text('Copy submission link'),
                      ),
                    if (canVote)
                      ElevatedButton(
                        onPressed: _busy
                            ? null
                            : () => _run(
                                  () => AppApi.voteBattleSubmission(widget.battleId, id),
                                  success: 'Vote cast',
                                ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.primary,
                          foregroundColor: Colors.white,
                        ),
                        child: Text('Vote for $name'),
                      ),
                    if (_hasVoted && _status == 'VOTING')
                      Text("You've voted", style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
                  ],
                ),
              );
            }),
        ],
      ),
    );
  }

  Widget _leaderboardCard() {
    final board = (_battle?['leaderboard'] as List? ?? [])
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
    if (board.isEmpty) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Leaderboard', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
          const SizedBox(height: 10),
          ...board.map((row) {
            final rank = row['rank'];
            final name = row['username']?.toString() ?? 'User';
            final votes = row['voteCount'];
            final weighted = row['weightedScore'];
            return ListTile(
              dense: true,
              contentPadding: EdgeInsets.zero,
              leading: CircleAvatar(
                backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                child: Text('#$rank', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary, fontSize: 12)),
              ),
              title: Text(name, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
              trailing: Text(
                weighted != null ? '${(weighted as num).toStringAsFixed(1)} pts' : '$votes votes',
                style: GoogleFonts.inter(fontWeight: FontWeight.w600, color: AppTheme.textMuted),
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _winnersCard() {
    final b = _battle!;
    final w1 = b['winnerUsername']?.toString();
    final w2 = b['winner2Username']?.toString();
    final w3 = b['winner3Username']?.toString();
    final p1 = (b['prize1'] as num?)?.toDouble() ?? 0;
    final p2 = (b['prize2'] as num?)?.toDouble() ?? 0;
    final p3 = (b['prize3'] as num?)?.toDouble() ?? 0;
    if (w1 == null && w2 == null && w3 == null) return const SizedBox.shrink();

    Widget row(String place, String? name, double prize, Color color) {
      if (name == null) return const SizedBox.shrink();
      return ListTile(
        contentPadding: EdgeInsets.zero,
        leading: Icon(Icons.emoji_events, color: color),
        title: Text('$place · $name', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        trailing: prize > 0
            ? Text('₹${prize.toStringAsFixed(0)}', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: const Color(0xFF10B981)))
            : null,
      );
    }

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: AppTheme.cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            _status == 'TIE' ? 'Tied Winners' : 'Winners',
            style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16),
          ),
          row('1st', w1, p1, const Color(0xFFF5C542)),
          row('2nd', w2, _status == 'TIE' ? p1 : p2, const Color(0xFF94A3B8)),
          row('3rd', w3, p3, const Color(0xFFCD7F32)),
        ],
      ),
    );
  }
}
