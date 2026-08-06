import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../services/realtime_service.dart';
import '../../theme/app_theme.dart';

class MusicRoomLivePage extends StatefulWidget {
  const MusicRoomLivePage({super.key, required this.code, required this.name});

  final String code;
  final String name;

  @override
  State<MusicRoomLivePage> createState() => _MusicRoomLivePageState();
}

class _MusicSubmission {
  _MusicSubmission({
    required this.id,
    required this.title,
    this.artist,
    this.submittedBy,
    this.votes = 0,
  });

  final int id;
  final String title;
  final String? artist;
  final String? submittedBy;
  int votes;
}

class _MusicRoomLivePageState extends State<MusicRoomLivePage> {
  final List<String> _debugEvents = [];
  final List<_MusicSubmission> _submissions = [];
  String? _phase;
  String? _status;
  int? _hostId;
  String? _hostUsername;
  bool _submissionsLocked = false;
  bool _votingLocked = false;
  int? _winnerSubmissionId;
  bool _showDebug = false;
  bool _isHost = false;

  String get _topic => '/topic/music-room/${widget.code}';
  String get _appBase => '/app/music-room/${widget.code}';

  // Destinations present under /app/music-room/{code}/...
  static const _hostActions = [
    'lock-submissions',
    'start-voting',
    'lock-voting',
    'end-room',
    'declare-winner',
  ];

  @override
  void initState() {
    super.initState();
    _bind();
  }

  Future<void> _bind() async {
    await RealtimeService.instance.subscribeJson(_topic, (data) {
      if (!mounted) return;
      setState(() {
        _debugEvents.insert(0, jsonEncode(data));
        if (_debugEvents.length > 50) _debugEvents.removeLast();
        _applyPayload(data);
      });
    });
    RealtimeService.instance.send('$_appBase/subscribe', {});
  }

  void _applyPayload(Map<String, dynamic> data) {
    if (data['phase'] != null) _phase = data['phase'].toString();
    if (data['status'] != null) _status = data['status'].toString();
    if (data['submissionsLocked'] is bool) _submissionsLocked = data['submissionsLocked'] as bool;
    if (data['votingLocked'] is bool) _votingLocked = data['votingLocked'] as bool;
    if (data['hostId'] is num) _hostId = (data['hostId'] as num).toInt();
    if (data['hostUsername'] != null) _hostUsername = data['hostUsername'].toString();
    if (data['winnerSubmissionId'] is num) {
      _winnerSubmissionId = (data['winnerSubmissionId'] as num).toInt();
    }

    final myId = context.read<AuthProvider>().user?.id;
    if (_hostId != null && myId != null) {
      _isHost = _hostId == myId;
    }

    final subs = data['submissions'];
    if (subs is List) {
      _submissions
        ..clear()
        ..addAll(subs.whereType<Map>().map((e) {
          final map = Map<String, dynamic>.from(e);
          final id = (map['id'] as num?)?.toInt() ?? 0;
          return _MusicSubmission(
            id: id,
            title: map['trackTitle']?.toString() ?? map['title']?.toString() ?? 'Track',
            artist: map['artist']?.toString(),
            submittedBy: map['submittedBy']?.toString(),
            votes: (map['votes'] as num?)?.toInt() ?? 0,
          );
        }));
    } else {
      // Partial event (legacy HTTP broadcast) — refresh full state
      final type = data['type']?.toString();
      if (type != null && type != 'state' && type != 'error' && type != 'hello') {
        RealtimeService.instance.send('$_appBase/subscribe', {});
      }
    }
  }

  void _send(String action, [Map<String, dynamic> payload = const {}]) {
    RealtimeService.instance.send('$_appBase/$action', payload);
  }

  void _vote(int submissionId) {
    _send('vote', {'submissionId': submissionId});
    AppTheme.showSuccess(context, 'Vote sent');
  }

  Future<void> _submitTrack() async {
    if (_submissionsLocked) {
      AppTheme.showError(context, 'Submissions are locked');
      return;
    }
    List<Map<String, dynamic>> tracks = [];
    try {
      tracks = await AppApi.musicTracks();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
      return;
    }
    if (!mounted) return;
    if (tracks.isEmpty) {
      AppTheme.showError(context, 'No approved tracks to submit');
      return;
    }
    final selected = await AppTheme.showBottomSheet<int>(
      context,
      (ctx) => SafeArea(
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: tracks.length,
          itemBuilder: (_, i) {
            final t = tracks[i];
            final id = (t['id'] as num?)?.toInt();
            return ListTile(
              title: Text(t['title']?.toString() ?? 'Track'),
              subtitle: Text(t['artist']?.toString() ?? ''),
              onTap: id == null ? null : () => Navigator.pop(ctx, id),
            );
          },
        ),
      ),
    );
    if (selected == null) return;
    try {
      await AppApi.submitMusicRoomTrack(widget.code, selected);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Track submitted');
      RealtimeService.instance.send('$_appBase/subscribe', {});
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Widget _chip(String label, {Color? color}) {
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

  Widget _hostControls() {
    if (!_isHost || _hostActions.isEmpty) return const SizedBox.shrink();
    final phase = (_phase ?? _status ?? '').toUpperCase();
    return Container(
      margin: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.amber.shade100),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Host controls', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              ElevatedButton(
                onPressed: () => _send('lock-submissions', {'locked': !_submissionsLocked}),
                style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
                child: Text(_submissionsLocked ? 'Unlock submissions' : 'Lock submissions'),
              ),
              ElevatedButton(
                onPressed: phase == 'ENDED' ? null : () => _send('start-voting', {'seconds': 300}),
                style: ElevatedButton.styleFrom(backgroundColor: Colors.teal, foregroundColor: Colors.white),
                child: const Text('Start voting'),
              ),
              ElevatedButton(
                onPressed: () => _send('lock-voting', {'locked': !_votingLocked}),
                style: ElevatedButton.styleFrom(backgroundColor: Colors.indigo, foregroundColor: Colors.white),
                child: Text(_votingLocked ? 'Unlock voting' : 'Lock voting'),
              ),
              ElevatedButton(
                onPressed: phase == 'ENDED' ? null : () => _send('end-room'),
                style: ElevatedButton.styleFrom(backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
                child: const Text('End room'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final phase = (_phase ?? _status ?? '…').toUpperCase();
    final canVote = phase == 'VOTE' && !_votingLocked;

    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text(widget.name, style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        actions: [
          IconButton(
            tooltip: _showDebug ? 'Hide debug' : 'Show debug',
            onPressed: () => setState(() => _showDebug = !_showDebug),
            icon: Icon(_showDebug ? Icons.bug_report : Icons.bug_report_outlined),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          RealtimeService.instance.send('$_appBase/subscribe', {});
        },
        child: ListView(
          padding: const EdgeInsets.only(bottom: 24),
          children: [
            Container(
              margin: const EdgeInsets.fromLTRB(12, 12, 12, 0),
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: Colors.blue.shade50),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(widget.name, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      _chip('Code ${widget.code}'),
                      _chip('Phase $phase', color: phase == 'ENDED' ? Colors.grey : Colors.teal),
                      if (_hostUsername != null) _chip('Host $_hostUsername', color: Colors.indigo),
                      if (_submissionsLocked) _chip('Submissions locked', color: Colors.orange.shade800),
                      if (_votingLocked) _chip('Voting locked', color: Colors.orange.shade800),
                    ],
                  ),
                ],
              ),
            ),
            _hostControls(),
            if (!_submissionsLocked)
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
                child: SizedBox(
                  width: double.infinity,
                  child: OutlinedButton.icon(
                    onPressed: _submitTrack,
                    icon: const Icon(Icons.library_music_outlined),
                    label: const Text('Submit a track'),
                  ),
                ),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 6),
              child: Text('Submissions', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
            ),
            if (_submissions.isEmpty)
              Padding(
                padding: const EdgeInsets.all(24),
                child: Center(
                  child: Text(
                    'No submissions yet — waiting for room updates…',
                    style: GoogleFonts.inter(color: Colors.black45),
                    textAlign: TextAlign.center,
                  ),
                ),
              )
            else
              ..._submissions.map((s) {
                final isWinner = _winnerSubmissionId == s.id;
                return Container(
                  margin: const EdgeInsets.fromLTRB(12, 0, 12, 8),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: isWinner ? Colors.amber : Colors.blue.shade50, width: isWinner ? 1.5 : 1),
                  ),
                  child: Row(
                    children: [
                      CircleAvatar(
                        backgroundColor: AppTheme.primary.withValues(alpha: 0.12),
                        child: const Icon(Icons.music_note, color: AppTheme.primary),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(s.title, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                            Text(
                              [
                                if (s.artist != null && s.artist!.isNotEmpty) s.artist!,
                                if (s.submittedBy != null) 'by ${s.submittedBy}',
                                '${s.votes} votes',
                                if (isWinner) 'Winner',
                              ].join(' · '),
                              style: GoogleFonts.inter(fontSize: 12, color: Colors.black45),
                            ),
                          ],
                        ),
                      ),
                      if (canVote)
                        TextButton(
                          onPressed: () => _vote(s.id),
                          child: const Text('Vote'),
                        ),
                      if (_isHost && phase != 'ENDED')
                        IconButton(
                          tooltip: 'Declare winner',
                          onPressed: () => _send('declare-winner', {'submissionId': s.id}),
                          icon: const Icon(Icons.emoji_events_outlined, color: Color(0xFFB45309)),
                        ),
                    ],
                  ),
                );
              }),
            if (_showDebug)
              Container(
                margin: const EdgeInsets.fromLTRB(12, 8, 12, 0),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.black87,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Debug', style: GoogleFonts.outfit(color: Colors.white, fontWeight: FontWeight.w700)),
                    const SizedBox(height: 6),
                    ..._debugEvents.take(12).map(
                          (e) => Padding(
                            padding: const EdgeInsets.only(bottom: 4),
                            child: Text(e, style: const TextStyle(fontFamily: 'monospace', fontSize: 10, color: Colors.white70)),
                          ),
                        ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
