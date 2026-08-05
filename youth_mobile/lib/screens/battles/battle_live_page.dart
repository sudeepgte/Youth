import 'package:flutter/material.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../services/battle_webrtc_controller.dart';
import '../../services/realtime_service.dart';
import '../../theme/app_theme.dart';

class BattleLivePage extends StatefulWidget {
  const BattleLivePage({super.key, required this.battleId, required this.title});

  final int battleId;
  final String title;

  @override
  State<BattleLivePage> createState() => _BattleLivePageState();
}

class _BattleComment {
  _BattleComment({required this.username, required this.message});
  final String username;
  final String message;
}

class _BattleParticipant {
  _BattleParticipant({
    required this.userId,
    required this.username,
    this.votes = 0,
    this.cameraOn,
    this.micOn,
  });
  final int userId;
  final String username;
  int votes;
  bool? cameraOn;
  bool? micOn;
}

class _BattleLivePageState extends State<BattleLivePage> {
  final _commentCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  final List<_BattleComment> _comments = [];
  final Map<int, _BattleParticipant> _participants = {};
  int _likeCount = 0;
  int _giftCount = 0;
  int? _viewerCount;
  bool _hasVoted = false;
  bool _liked = false;
  bool _isHost = false;
  bool _isParticipant = false;
  bool _showDebug = false;
  String? _status;
  final List<String> _debugEvents = [];
  BattleWebRtcController? _webrtc;
  bool _webrtcStarting = false;

  static const _giftTypes = [
    (type: 'ROSE', label: 'Rose', cost: 10, icon: '🌹'),
    (type: 'CROWN', label: 'Crown', cost: 200, icon: '👑'),
    (type: 'DIAMOND', label: 'Diamond', cost: 100, icon: '💎'),
  ];

  bool get _isBroadcaster => _isHost || _isParticipant;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    await _loadDetail();
    await _bind();
    await _startWebRtc();
  }

  Future<void> _loadDetail() async {
    try {
      final detail = await AppApi.battleDetail(widget.battleId);
      if (!mounted) return;
      final myId = context.read<AuthProvider>().user?.id;
      final creatorId = (detail['creatorId'] as num?)?.toInt();
      final parts = detail['participants'];
      setState(() {
        _isHost = creatorId != null && myId != null && creatorId == myId;
        _isParticipant = detail['isParticipant'] == true;
        if (detail['status'] != null) _status = detail['status'].toString();
        if (parts is List) {
          for (final e in parts) {
            if (e is! Map) continue;
            final map = Map<String, dynamic>.from(e);
            final id = map['userId'] ?? map['id'];
            int? userId;
            if (id is num) userId = id.toInt();
            if (id is String) userId = int.tryParse(id);
            if (userId == null) continue;
            // Prefer userId field; participant entity id is different
            final uid = map['userId'];
            if (uid is num) userId = uid.toInt();
            final username = map['username']?.toString() ?? 'Player $userId';
            _participants.putIfAbsent(
              userId,
              () => _BattleParticipant(userId: userId!, username: username),
            );
          }
        }
      });
    } catch (_) {}
  }

  Future<void> _startWebRtc() async {
    final myId = context.read<AuthProvider>().user?.id;
    if (myId == null || _webrtc != null) return;
    setState(() => _webrtcStarting = true);
    final ctrl = BattleWebRtcController(
      battleId: widget.battleId,
      myUserId: myId,
      isBroadcaster: _isBroadcaster,
      onChanged: () {
        if (mounted) setState(() {});
      },
      onError: (msg) {
        if (mounted) AppTheme.showError(context, msg);
      },
    );
    _webrtc = ctrl;
    try {
      await ctrl.init();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
    if (mounted) setState(() => _webrtcStarting = false);
  }

  @override
  void dispose() {
    RealtimeService.instance.send('/app/battle/${widget.battleId}/viewer-leave', {});
    _webrtc?.dispose();
    _commentCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  void _logDebug(Map<String, dynamic> data) {
    _debugEvents.insert(0, data.toString());
    if (_debugEvents.length > 40) _debugEvents.removeLast();
  }

  Future<void> _bind() async {
    await RealtimeService.instance.subscribeJson('/topic/battle/${widget.battleId}/comments', (data) {
      if (!mounted) return;
      _logDebug(data);
      final username = data['username']?.toString() ?? 'User';
      final message = data['message']?.toString() ?? data['content']?.toString() ?? '';
      if (message.isEmpty) return;
      setState(() {
        _comments.add(_BattleComment(username: username, message: message));
      });
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollCtrl.hasClients) {
          _scrollCtrl.animateTo(
            _scrollCtrl.position.maxScrollExtent,
            duration: const Duration(milliseconds: 200),
            curve: Curves.easeOut,
          );
        }
      });
    });

    await RealtimeService.instance.subscribeJson('/topic/battle/${widget.battleId}/likes', (data) {
      if (!mounted) return;
      _logDebug(data);
      final count = data['likeCount'] ?? data['likes'] ?? data['count'];
      setState(() {
        if (count is num) _likeCount = count.toInt();
      });
    });

    await RealtimeService.instance.subscribeJson('/topic/battle/${widget.battleId}/gifts', (data) {
      if (!mounted) return;
      _logDebug(data);
      final total = data['totalGifts'];
      final sender = data['senderUsername']?.toString() ?? 'Someone';
      final giftType = data['giftType']?.toString() ?? 'GIFT';
      final recipient = data['recipientUsername']?.toString() ?? 'a contestant';
      final cost = data['coinsCost'];
      setState(() {
        if (total is num) _giftCount = total.toInt();
        _comments.add(_BattleComment(
          username: 'Gift',
          message: '$sender sent $giftType to $recipient${cost != null ? ' ($cost coins)' : ''}',
        ));
      });
    });

    await RealtimeService.instance.subscribeJson('/topic/battle/${widget.battleId}/votes', (data) {
      if (!mounted) return;
      _logDebug(data);
      final entries = data['entries'] ?? data['leaderboard'] ?? data['votes'];
      if (entries is List) {
        setState(() {
          for (final e in entries) {
            if (e is! Map) continue;
            final map = Map<String, dynamic>.from(e);
            final uid = map['userId'] ?? map['id'];
            int? userId;
            if (uid is num) userId = uid.toInt();
            if (uid is String) userId = int.tryParse(uid);
            if (userId == null) continue;
            final username = map['username']?.toString() ?? 'Player $userId';
            final votes = (map['voteCount'] as num?)?.toInt() ?? (map['votes'] as num?)?.toInt() ?? 0;
            final existing = _participants[userId];
            _participants[userId] = _BattleParticipant(
              userId: userId,
              username: username,
              votes: votes,
              cameraOn: existing?.cameraOn,
              micOn: existing?.micOn,
            );
          }
        });
      }
    });

    await RealtimeService.instance.subscribeJson('/topic/battle/${widget.battleId}/participants', (data) {
      if (!mounted) return;
      _logDebug(data);
      final uid = data['userId'] ?? data['id'];
      int? userId;
      if (uid is num) userId = uid.toInt();
      if (uid is String) userId = int.tryParse(uid);
      final username = data['username']?.toString();
      final cameraOn = data['cameraOn'] ?? data['videoEnabled'] ?? data['camera'];
      final micOn = data['micOn'] ?? data['audioEnabled'] ?? data['mic'];

      if (userId != null) {
        setState(() {
          final existing = _participants[userId];
          final name = username ?? existing?.username ?? 'Player $userId';
          _participants[userId!] = _BattleParticipant(
            userId: userId,
            username: name,
            votes: existing?.votes ?? 0,
            cameraOn: cameraOn is bool ? cameraOn : existing?.cameraOn,
            micOn: micOn is bool ? micOn : existing?.micOn,
          );
        });
      }
    });

    await RealtimeService.instance.subscribeJson('/topic/battle/${widget.battleId}/status', (data) {
      if (!mounted) return;
      _logDebug(data);
      setState(() {
        if (data['status'] != null) _status = data['status'].toString();
        final vc = data['viewerCount'];
        if (vc is num) _viewerCount = vc.toInt();
      });
    });

    RealtimeService.instance.send('/app/battle/${widget.battleId}/viewer-join', {});
  }

  void _sendComment() {
    final text = _commentCtrl.text.trim();
    if (text.isEmpty) return;
    RealtimeService.instance.send('/app/battle/${widget.battleId}/comment', {'message': text});
    _commentCtrl.clear();
  }

  void _sendLike() {
    RealtimeService.instance.send('/app/battle/${widget.battleId}/like', {});
    setState(() => _liked = true);
  }

  bool get _canEndBattle {
    if (!_isHost) return false;
    final s = (_status ?? '').toUpperCase();
    return s == 'ACTIVE' || s == 'VOTING' || s == 'LIVE';
  }

  Future<void> _endBattle() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('End battle?'),
        content: const Text('Winners will be determined from live votes. This cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('End battle')),
        ],
      ),
    );
    if (ok != true) return;
    RealtimeService.instance.send('/app/battle/${widget.battleId}/end-battle', {});
    if (!mounted) return;
    AppTheme.showSuccess(context, 'End battle requested');
  }

  Future<void> _sendGift() async {
    final participants = _participants.values.toList();
    if (participants.isEmpty) {
      AppTheme.showError(context, 'No participants yet to gift');
      return;
    }

    _BattleParticipant? selected = participants.first;
    var gift = _giftTypes.first;

    final ok = await showModalBottomSheet<bool>(
      context: context,
      backgroundColor: Colors.white,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocal) => Padding(
          padding: EdgeInsets.fromLTRB(20, 16, 20, 28 + MediaQuery.of(ctx).viewInsets.bottom),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Send a gift', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
              const SizedBox(height: 14),
              DropdownButtonFormField<_BattleParticipant>(
                initialValue: selected,
                decoration: AppTheme.dashboardInput('Recipient'),
                items: participants
                    .map((p) => DropdownMenuItem(value: p, child: Text(p.username)))
                    .toList(),
                onChanged: (v) => setLocal(() => selected = v),
              ),
              const SizedBox(height: 14),
              ..._giftTypes.map((g) {
                final selectedGift = gift.type == g.type;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(14),
                    onTap: () => setLocal(() => gift = g),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                      decoration: BoxDecoration(
                        color: selectedGift ? AppTheme.primary.withValues(alpha: 0.08) : Colors.grey.shade50,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(
                          color: selectedGift ? AppTheme.primary : Colors.grey.shade200,
                        ),
                      ),
                      child: Row(
                        children: [
                          Text(g.icon, style: const TextStyle(fontSize: 22)),
                          const SizedBox(width: 12),
                          Expanded(child: Text(g.label, style: GoogleFonts.outfit(fontWeight: FontWeight.w700))),
                          Text('${g.cost} coins', style: GoogleFonts.inter(fontWeight: FontWeight.w600)),
                        ],
                      ),
                    ),
                  ),
                );
              }),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () => Navigator.pop(ctx, true),
                  style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
                  child: Text('Send ${gift.label}'),
                ),
              ),
            ],
          ),
        ),
      ),
    );

    if (ok != true || selected == null) return;
    RealtimeService.instance.send('/app/battle/${widget.battleId}/gift', {
      'giftType': gift.type,
      'recipientId': selected!.userId,
    });
  }

  Future<void> _liveVote() async {
    if (_hasVoted) {
      AppTheme.showError(context, 'You already voted');
      return;
    }
    final participants = _participants.values.toList()..sort((a, b) => b.votes.compareTo(a.votes));
    if (participants.isEmpty) {
      AppTheme.showError(context, 'No participants to vote for yet');
      return;
    }

    final picked = await showModalBottomSheet<_BattleParticipant>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text('Live vote', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
            ),
            ...participants.map(
              (p) => ListTile(
                title: Text(p.username, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                subtitle: Text('${p.votes} votes'),
                onTap: () => Navigator.pop(ctx, p),
              ),
            ),
          ],
        ),
      ),
    );

    if (picked == null) return;
    RealtimeService.instance.send('/app/battle/${widget.battleId}/live-vote', {
      'participantUserId': picked.userId,
    });
    if (!mounted) return;
    setState(() => _hasVoted = true);
    AppTheme.showSuccess(context, 'Vote sent for ${picked.username}');
  }

  Widget _statusChip(String label, {Color? color}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: (color ?? AppTheme.primary).withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        label,
        style: GoogleFonts.inter(fontSize: 12, fontWeight: FontWeight.w700, color: color ?? AppTheme.primary),
      ),
    );
  }

  Widget _videoTile({
    required String label,
    required RTCVideoRenderer renderer,
    required bool hasStream,
    bool mirror = false,
    bool cameraOff = false,
    bool expand = true,
    double? width,
    double height = 180,
  }) {
    final child = Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: const Color(0xFF0F172A),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white12),
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        fit: StackFit.expand,
        children: [
          if (hasStream && !cameraOff)
            RTCVideoView(
              renderer,
              objectFit: RTCVideoViewObjectFit.RTCVideoViewObjectFitCover,
              mirror: mirror,
            )
          else
            Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    cameraOff ? Icons.videocam_off : Icons.person,
                    color: Colors.white38,
                    size: 36,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    cameraOff ? 'Camera off' : 'Waiting…',
                    style: GoogleFonts.inter(color: Colors.white54, fontSize: 12),
                  ),
                ],
              ),
            ),
          Positioned(
            left: 8,
            bottom: 8,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.black54,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(label, style: GoogleFonts.outfit(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 12)),
            ),
          ),
        ],
      ),
    );
    if (expand) return Expanded(child: child);
    return child;
  }

  Widget _videoStage() {
    final rtc = _webrtc;
    final remotes = rtc?.remoteRenderers.entries.toList() ?? [];
    final mediaError = rtc?.mediaError;

    return Container(
      color: Colors.black,
      padding: const EdgeInsets.fromLTRB(10, 10, 10, 8),
      child: Column(
        children: [
          if (_webrtcStarting)
            const Padding(
              padding: EdgeInsets.only(bottom: 8),
              child: LinearProgressIndicator(minHeight: 2, color: AppTheme.primary),
            ),
          if (mediaError != null)
            Container(
              width: double.infinity,
              margin: const EdgeInsets.only(bottom: 8),
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFFDC2626),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Text(mediaError, style: GoogleFonts.inter(color: Colors.white, fontSize: 12)),
                  ),
                  TextButton(
                    onPressed: () => rtc?.startLocalMedia(),
                    child: const Text('Retry', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
            ),
          SizedBox(
            height: 180,
            child: Row(
              children: [
                if (_isBroadcaster && rtc != null)
                  _videoTile(
                    label: 'You',
                    renderer: rtc.localRenderer,
                    hasStream: rtc.localStream != null,
                    mirror: true,
                    cameraOff: !rtc.cameraOn,
                  ),
                if (_isBroadcaster && remotes.isNotEmpty) const SizedBox(width: 8),
                if (remotes.isNotEmpty)
                  _videoTile(
                    label: _participants[remotes.first.key]?.username ?? 'Opponent',
                    renderer: remotes.first.value,
                    hasStream: remotes.first.value.srcObject != null,
                    cameraOff: _participants[remotes.first.key]?.cameraOn == false,
                  )
                else if (!_isBroadcaster)
                  Expanded(
                    child: Container(
                      decoration: BoxDecoration(
                        color: const Color(0xFF0F172A),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        remotes.isEmpty ? 'Waiting for broadcasters…' : '',
                        style: GoogleFonts.inter(color: Colors.white54),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          if (remotes.length > 1) ...[
            const SizedBox(height: 8),
            SizedBox(
              height: 120,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: remotes.length - 1,
                separatorBuilder: (_, _) => const SizedBox(width: 8),
                itemBuilder: (_, i) {
                  final e = remotes[i + 1];
                  final name = _participants[e.key]?.username ?? 'User ${e.key}';
                  return _videoTile(
                    label: name,
                    renderer: e.value,
                    hasStream: e.value.srcObject != null,
                    cameraOff: _participants[e.key]?.cameraOn == false,
                    expand: false,
                    width: 160,
                    height: 120,
                  );
                },
              ),
            ),
          ],
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final myUsername = context.watch<AuthProvider>().user?.username;
    final participants = _participants.values.toList()..sort((a, b) => b.votes.compareTo(a.votes));
    final statusLabel = (_status ?? 'LIVE').toUpperCase();
    final rtc = _webrtc;
    final camOn = rtc?.cameraOn ?? true;
    final micOn = rtc?.micOn ?? true;

    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text(widget.title, style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        actions: [
          if (_canEndBattle)
            TextButton(
              onPressed: _endBattle,
              child: Text('End', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: Colors.redAccent)),
            ),
          IconButton(
            tooltip: _showDebug ? 'Hide debug' : 'Show debug',
            onPressed: () => setState(() => _showDebug = !_showDebug),
            icon: Icon(_showDebug ? Icons.bug_report : Icons.bug_report_outlined, size: 20),
          ),
        ],
      ),
      body: Column(
        children: [
          _videoStage(),
          Container(
            width: double.infinity,
            color: Colors.white,
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _statusChip(statusLabel, color: statusLabel == 'COMPLETED' ? Colors.grey : Colors.green.shade700),
                if (_viewerCount != null) _statusChip('$_viewerCount watching', color: Colors.indigo),
                if (_isBroadcaster) ...[
                  _statusChip(camOn ? 'Cam on' : 'Cam off', color: camOn ? Colors.teal : Colors.grey),
                  _statusChip(micOn ? 'Mic on' : 'Mic off', color: micOn ? Colors.teal : Colors.grey),
                ] else
                  _statusChip('Audience', color: Colors.blueGrey),
              ],
            ),
          ),
          Container(
            color: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
            child: Row(
              children: [
                IconButton(
                  tooltip: 'Like',
                  onPressed: _sendLike,
                  icon: Icon(_liked ? Icons.favorite : Icons.favorite_border, color: Colors.redAccent),
                ),
                Text('$_likeCount', style: GoogleFonts.inter(fontWeight: FontWeight.w700)),
                IconButton(
                  tooltip: 'Gift',
                  onPressed: _sendGift,
                  icon: const Icon(Icons.card_giftcard, color: Color(0xFFF59E0B)),
                ),
                Text('$_giftCount', style: GoogleFonts.inter(fontWeight: FontWeight.w700)),
                if (_isBroadcaster) ...[
                  IconButton(
                    tooltip: 'Camera',
                    onPressed: () => rtc?.toggleCamera(),
                    icon: Icon(camOn ? Icons.videocam : Icons.videocam_off, color: camOn ? AppTheme.primary : Colors.grey),
                  ),
                  IconButton(
                    tooltip: 'Mic',
                    onPressed: () => rtc?.toggleMic(),
                    icon: Icon(micOn ? Icons.mic : Icons.mic_off, color: micOn ? AppTheme.primary : Colors.grey),
                  ),
                  IconButton(
                    tooltip: 'Flip camera',
                    onPressed: () => rtc?.switchCamera(),
                    icon: const Icon(Icons.cameraswitch_outlined),
                  ),
                  IconButton(
                    tooltip: 'Share screen',
                    onPressed: () => rtc?.shareScreen(),
                    icon: const Icon(Icons.screen_share_outlined),
                  ),
                ],
                const Spacer(),
                TextButton.icon(
                  onPressed: _hasVoted ? null : _liveVote,
                  icon: const Icon(Icons.how_to_vote_outlined, size: 18),
                  label: Text(_hasVoted ? 'Voted' : 'Vote'),
                ),
              ],
            ),
          ),
          if (participants.isNotEmpty)
            SizedBox(
              height: 72,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
                itemCount: participants.length,
                separatorBuilder: (_, _) => const SizedBox(width: 8),
                itemBuilder: (_, i) {
                  final p = participants[i];
                  return Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: Colors.blue.shade50),
                    ),
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 14,
                          backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                          child: Text(
                            p.username.isNotEmpty ? p.username[0].toUpperCase() : '?',
                            style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary, fontSize: 12),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(p.username, style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 13)),
                            Text('${p.votes} votes', style: GoogleFonts.inter(fontSize: 11, color: Colors.black45)),
                          ],
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
          Expanded(
            child: _comments.isEmpty
                ? Center(child: Text('Live comments will show here', style: GoogleFonts.inter(color: Colors.black38)))
                : ListView.builder(
                    controller: _scrollCtrl,
                    padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
                    itemCount: _comments.length,
                    itemBuilder: (_, i) {
                      final c = _comments[i];
                      final mine = myUsername != null && c.username == myUsername;
                      final isGift = c.username == 'Gift';
                      return Align(
                        alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
                        child: Container(
                          constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.8),
                          margin: const EdgeInsets.only(bottom: 8),
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                          decoration: BoxDecoration(
                            color: isGift
                                ? const Color(0xFFFFF8E7)
                                : mine
                                    ? AppTheme.primary
                                    : Colors.white,
                            borderRadius: BorderRadius.circular(14),
                            border: isGift ? Border.all(color: const Color(0xFFF5C542)) : null,
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              if (!mine || isGift)
                                Text(
                                  c.username,
                                  style: GoogleFonts.outfit(
                                    fontWeight: FontWeight.w700,
                                    fontSize: 12,
                                    color: isGift ? const Color(0xFFB45309) : Colors.black54,
                                  ),
                                ),
                              Text(
                                c.message,
                                style: GoogleFonts.inter(color: mine && !isGift ? Colors.white : Colors.black87),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
          ),
          if (_showDebug)
            SizedBox(
              height: 100,
              child: Container(
                width: double.infinity,
                color: Colors.black87,
                padding: const EdgeInsets.all(8),
                child: ListView(
                  children: _debugEvents
                      .map((e) => Text(e, style: const TextStyle(fontFamily: 'monospace', fontSize: 10, color: Colors.white70)))
                      .toList(),
                ),
              ),
            ),
          SafeArea(
            child: Container(
              color: Colors.white,
              padding: const EdgeInsets.fromLTRB(10, 8, 6, 8),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _commentCtrl,
                      decoration: AppTheme.dashboardInput('Comment in live battle'),
                      onSubmitted: (_) => _sendComment(),
                    ),
                  ),
                  IconButton(
                    onPressed: _sendComment,
                    icon: const Icon(Icons.send, color: AppTheme.primary),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
