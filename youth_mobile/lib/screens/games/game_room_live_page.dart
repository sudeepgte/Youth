import 'dart:convert';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/realtime_service.dart';
import '../../theme/app_theme.dart';

class GameRoomLivePage extends StatefulWidget {
  const GameRoomLivePage({
    super.key,
    required this.game,
    required this.roomId,
    this.playerIndex,
    this.playerNum,
  });

  final String game;
  final String roomId;
  final int? playerIndex;
  final int? playerNum;

  @override
  State<GameRoomLivePage> createState() => _GameRoomLivePageState();
}

class _GameRoomLivePageState extends State<GameRoomLivePage> {
  final List<String> _debugEvents = [];
  final _chatCtrl = TextEditingController();
  final _fromCtrl = TextEditingController();
  final _toCtrl = TextEditingController();
  final _pieceCtrl = TextEditingController(text: '0');
  final _newPosCtrl = TextEditingController();
  final _cardIdCtrl = TextEditingController();
  final _catchTargetCtrl = TextEditingController(text: '0');
  Map<String, dynamic>? _state;
  bool _showDebug = false;
  late int _playerIndex;
  late int _playerNum;

  String get _topic => '/topic/${widget.game}/${widget.roomId}';
  String get _appBase => '/app/${widget.game}/${widget.roomId}';

  @override
  void initState() {
    super.initState();
    _playerIndex = widget.playerIndex ?? 0;
    _playerNum = widget.playerNum ?? (widget.playerIndex != null ? widget.playerIndex! + 1 : 1);
    _connect();
  }

  @override
  void dispose() {
    _chatCtrl.dispose();
    _fromCtrl.dispose();
    _toCtrl.dispose();
    _pieceCtrl.dispose();
    _newPosCtrl.dispose();
    _cardIdCtrl.dispose();
    _catchTargetCtrl.dispose();
    super.dispose();
  }

  Future<void> _connect() async {
    await RealtimeService.instance.subscribeJson(_topic, (data) {
      if (!mounted) return;
      setState(() {
        _state = data;
        _debugEvents.insert(0, jsonEncode(data));
        if (_debugEvents.length > 40) _debugEvents.removeLast();
      });
    });

    if (widget.game == 'uno') {
      await RealtimeService.instance.subscribeJson('$_topic/player/$_playerIndex', (data) {
        if (!mounted) return;
        setState(() {
          _state = data;
          _debugEvents.insert(0, jsonEncode(data));
          if (_debugEvents.length > 40) _debugEvents.removeLast();
        });
      });
    }

    RealtimeService.instance.send('$_appBase/subscribe', {});
  }

  void _send(String action, [Map<String, dynamic> payload = const {}]) {
    RealtimeService.instance.send('$_appBase/$action', payload);
  }

  List<String> _playerNames() {
    final s = _state;
    if (s == null) return const [];
    final players = s['players'];
    if (players is List) {
      return players.map((p) {
        if (p is Map) return (p['name'] ?? p['username'] ?? 'Player').toString();
        return p.toString();
      }).where((n) => n.trim().isNotEmpty).toList();
    }
    if (players is Map) {
      return players.values.map((e) => e.toString()).where((n) => n.trim().isNotEmpty).toList();
    }
    final names = <String>[];
    if (s['player1'] != null && s['player1'].toString().isNotEmpty) names.add(s['player1'].toString());
    if (s['player2'] != null && s['player2'].toString().isNotEmpty) names.add(s['player2'].toString());
    if (s['whitePlayer'] != null) names.add(s['whitePlayer'].toString());
    if (s['blackPlayer'] != null) names.add(s['blackPlayer'].toString());
    return names;
  }

  String _turnLabel() {
    final s = _state;
    if (s == null) return 'Waiting for state…';
    final status = s['status']?.toString() ?? '—';
    final names = _playerNames();

    if (widget.game == 'rps') {
      final round = s['currentRound'];
      final result = s['lastResult']?.toString();
      final winner = s['matchWinner']?.toString();
      final parts = <String>['Status: $status'];
      if (round != null) parts.add('Round $round');
      if (result != null && result.isNotEmpty) parts.add(result);
      if (winner != null && winner.isNotEmpty) parts.add('Winner: $winner');
      return parts.join(' · ');
    }

    if (widget.game == 'chess') {
      final turn = s['turn']?.toString() == 'b' ? 'Black' : 'White';
      final players = s['players'];
      String whose = turn;
      if (players is Map) {
        final name = players[s['turn']?.toString() ?? 'w']?.toString();
        if (name != null && name.isNotEmpty) whose = '$turn ($name)';
      }
      return 'Status: $status · Turn: $whose';
    }

    if (widget.game == 'ludo' || widget.game == 'uno') {
      final idx = (s['currentPlayer'] as num?)?.toInt();
      final name = (idx != null && idx >= 0 && idx < names.length) ? names[idx] : 'Player ${(idx ?? 0) + 1}';
      final dice = s['diceValue'];
      final extra = widget.game == 'ludo' && dice != null ? ' · Dice: $dice' : '';
      return 'Status: $status · Turn: $name$extra';
    }

    if (widget.game == 'snake') {
      final idx = (s['turn'] as num?)?.toInt();
      final name = (idx != null && idx >= 0 && idx < names.length) ? names[idx] : 'Player ${(idx ?? 0) + 1}';
      return 'Status: $status · Turn: $name';
    }

    return 'Status: $status';
  }

  List<int> _parseCoord(String raw) {
    final cleaned = raw.trim().toLowerCase();
    // Algebraic like e2 -> [row, col] with row 0 at top (rank 8)
    final alg = RegExp(r'^([a-h])([1-8])$');
    final m = alg.firstMatch(cleaned);
    if (m != null) {
      final col = m.group(1)!.codeUnitAt(0) - 'a'.codeUnitAt(0);
      final rank = int.parse(m.group(2)!);
      final row = 8 - rank;
      return [row, col];
    }
    final parts = cleaned.split(RegExp(r'[,\s]+'));
    if (parts.length >= 2) {
      final r = int.tryParse(parts[0]);
      final c = int.tryParse(parts[1]);
      if (r != null && c != null) return [r, c];
    }
    throw FormatException('Use e2 or row,col');
  }

  Widget _sectionCard({required String title, required Widget child}) {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.blue.shade50),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 15)),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }

  Widget _playersCard() {
    final names = _playerNames();
    return _sectionCard(
      title: 'Players',
      child: names.isEmpty
          ? Text('Waiting for players…', style: GoogleFonts.inter(color: Colors.black45))
          : Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (var i = 0; i < names.length; i++)
                  Chip(
                    avatar: CircleAvatar(
                      backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                      child: Text('${i + 1}', style: GoogleFonts.outfit(fontSize: 11, fontWeight: FontWeight.w800, color: AppTheme.primary)),
                    ),
                    label: Text(names[i]),
                    backgroundColor: Colors.grey.shade50,
                  ),
              ],
            ),
    );
  }

  Widget _primaryButton(String label, VoidCallback onPressed, {Color? color}) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: color ?? AppTheme.primary,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      ),
      child: Text(label, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
    );
  }

  Widget _rpsControls() {
    return _sectionCard(
      title: 'Your move',
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: SizedBox(
                  height: 88,
                  child: ElevatedButton(
                    onPressed: () => _send('choice', {'playerNum': _playerNum, 'choice': 'ROCK'}),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF64748B),
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    child: Text('ROCK', style: GoogleFonts.outfit(fontWeight: FontWeight.w900, fontSize: 16)),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: SizedBox(
                  height: 88,
                  child: ElevatedButton(
                    onPressed: () => _send('choice', {'playerNum': _playerNum, 'choice': 'PAPER'}),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF0EA5E9),
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    child: Text('PAPER', style: GoogleFonts.outfit(fontWeight: FontWeight.w900, fontSize: 16)),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: SizedBox(
                  height: 88,
                  child: ElevatedButton(
                    onPressed: () => _send('choice', {'playerNum': _playerNum, 'choice': 'SCISSORS'}),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFFEF4444),
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    child: Text('SCISSORS', style: GoogleFonts.outfit(fontWeight: FontWeight.w900, fontSize: 14)),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: _primaryButton('Play again', () => _send('playAgain', {'playerNum': _playerNum}))),
              const SizedBox(width: 8),
              Expanded(child: _primaryButton('Next round', () => _send('nextRound'))),
            ],
          ),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(onPressed: () => _send('start'), child: const Text('Start match')),
          ),
        ],
      ),
    );
  }

  Widget _chessControls() {
    final username = context.read<AuthProvider>().user?.username ?? 'Player';
    return _sectionCard(
      title: 'Chess move',
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _fromCtrl,
                  decoration: AppTheme.dashboardInput('From (e2 or 6,4)'),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: _toCtrl,
                  decoration: AppTheme.dashboardInput('To (e4 or 4,4)'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: _primaryButton('Send move', () {
                  try {
                    final from = _parseCoord(_fromCtrl.text);
                    final to = _parseCoord(_toCtrl.text);
                    _send('move', {'from': from, 'to': to});
                  } catch (e) {
                    AppTheme.showError(context, e.toString());
                  }
                }),
              ),
              const SizedBox(width: 8),
              _primaryButton('Leave', () => _send('leave'), color: Colors.redAccent),
            ],
          ),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(onPressed: () => _send('start'), child: const Text('Start game')),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _chatCtrl,
                  decoration: AppTheme.dashboardInput('Chat message'),
                  onSubmitted: (_) {
                    final text = _chatCtrl.text.trim();
                    if (text.isEmpty) return;
                    _send('chat', {'sender': username, 'text': text, 'color': 'w'});
                    _chatCtrl.clear();
                  },
                ),
              ),
              IconButton(
                onPressed: () {
                  final text = _chatCtrl.text.trim();
                  if (text.isEmpty) return;
                  _send('chat', {'sender': username, 'text': text, 'color': 'w'});
                  _chatCtrl.clear();
                },
                icon: const Icon(Icons.send, color: AppTheme.primary),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _ludoControls() {
    final username = context.read<AuthProvider>().user?.username ?? 'Player';
    return _sectionCard(
      title: 'Ludo actions',
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: _primaryButton('Roll', () {
                  final val = Random().nextInt(6) + 1;
                  _send('roll', {'val': val, 'playerIndex': _playerIndex});
                }),
              ),
              const SizedBox(width: 8),
              Expanded(child: _primaryButton('Skip', () => _send('skip', {'playerIndex': _playerIndex}))),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _pieceCtrl,
                  keyboardType: TextInputType.number,
                  decoration: AppTheme.dashboardInput('Piece index (0-3)'),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: _newPosCtrl,
                  keyboardType: TextInputType.number,
                  decoration: AppTheme.dashboardInput('New pos'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: _primaryButton('Move piece', () {
              final piece = int.tryParse(_pieceCtrl.text.trim());
              final newPos = int.tryParse(_newPosCtrl.text.trim());
              if (piece == null || newPos == null) {
                AppTheme.showError(context, 'Enter piece index and new position');
                return;
              }
              _send('move', {
                'playerIndex': _playerIndex,
                'pieceIndex': piece,
                'newPos': newPos,
              });
            }),
          ),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(onPressed: () => _send('start'), child: const Text('Start game')),
          ),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _chatCtrl,
                  decoration: AppTheme.dashboardInput('Chat'),
                  onSubmitted: (_) {
                    final text = _chatCtrl.text.trim();
                    if (text.isEmpty) return;
                    _send('chat', {'sender': username, 'text': text});
                    _chatCtrl.clear();
                  },
                ),
              ),
              IconButton(
                onPressed: () {
                  final text = _chatCtrl.text.trim();
                  if (text.isEmpty) return;
                  _send('chat', {'sender': username, 'text': text});
                  _chatCtrl.clear();
                },
                icon: const Icon(Icons.send, color: AppTheme.primary),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _unoControls() {
    return _sectionCard(
      title: 'UNO actions',
      child: Column(
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _primaryButton('Draw', () => _send('draw', {'playerIndex': _playerIndex})),
              _primaryButton('Call UNO', () => _send('call-uno', {'playerIndex': _playerIndex})),
              _primaryButton('Start', () => _send('start')),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _cardIdCtrl,
                  keyboardType: TextInputType.number,
                  decoration: AppTheme.dashboardInput('Card id to play'),
                ),
              ),
              const SizedBox(width: 8),
              _primaryButton('Play', () {
                final cardId = int.tryParse(_cardIdCtrl.text.trim());
                if (cardId == null) {
                  AppTheme.showError(context, 'Enter a card id');
                  return;
                }
                _send('play', {'playerIndex': _playerIndex, 'cardId': cardId});
              }),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _catchTargetCtrl,
                  keyboardType: TextInputType.number,
                  decoration: AppTheme.dashboardInput('Catch target index'),
                ),
              ),
              const SizedBox(width: 8),
              _primaryButton('Catch UNO', () {
                final target = int.tryParse(_catchTargetCtrl.text.trim()) ?? 0;
                _send('catch-uno', {'catcherIndex': _playerIndex, 'targetIndex': target});
              }, color: Colors.orange.shade700),
            ],
          ),
          if (_state?['players'] is List) ...[
            const SizedBox(height: 12),
            Text('Your hand / top card', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
            const SizedBox(height: 6),
            Text(
              () {
                final players = _state!['players'] as List;
                if (_playerIndex < players.length && players[_playerIndex] is Map) {
                  final hand = (players[_playerIndex] as Map)['hand'];
                  final top = _state!['topCard'];
                  return 'Top: $top\nHand: $hand';
                }
                return 'Top: ${_state!['topCard']}';
              }(),
              style: GoogleFonts.inter(fontSize: 12, color: Colors.black54),
            ),
          ],
        ],
      ),
    );
  }

  Widget _snakeControls() {
    return _sectionCard(
      title: 'Snake & ladders',
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: _primaryButton('Roll', () {
                  final steps = Random().nextInt(6) + 1;
                  _send('roll', {'steps': steps, 'playerIndex': _playerIndex});
                }),
              ),
              const SizedBox(width: 8),
              Expanded(child: _primaryButton('Start', () => _send('start'))),
              const SizedBox(width: 8),
              Expanded(child: _primaryButton('Leave', () => _send('leave'), color: Colors.redAccent)),
            ],
          ),
          if (_state?['players'] is List) ...[
            const SizedBox(height: 12),
            ...(_state!['players'] as List).asMap().entries.map((e) {
              final p = e.value;
              final name = p is Map ? (p['name'] ?? 'Player').toString() : p.toString();
              final pos = p is Map ? p['pos'] : '—';
              return Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text('$name · square $pos', style: GoogleFonts.inter(fontSize: 13)),
              );
            }),
          ],
        ],
      ),
    );
  }

  Widget _gameControls() {
    switch (widget.game) {
      case 'rps':
        return _rpsControls();
      case 'chess':
        return _chessControls();
      case 'ludo':
        return _ludoControls();
      case 'uno':
        return _unoControls();
      case 'snake':
        return _snakeControls();
      default:
        return _sectionCard(
          title: 'Actions',
          child: Wrap(
            spacing: 8,
            children: [
              _primaryButton('Start', () => _send('start')),
              _primaryButton('Leave', () => _send('leave'), color: Colors.redAccent),
            ],
          ),
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text(
          '${widget.game.toUpperCase()} · ${widget.roomId}',
          style: GoogleFonts.outfit(fontWeight: FontWeight.bold),
        ),
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
      body: ListView(
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
                Text('Live status', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
                const SizedBox(height: 6),
                Text(_turnLabel(), style: GoogleFonts.inter(fontSize: 14, color: Colors.black87)),
                Text(
                  'You: ${widget.game == 'rps' ? 'Player $_playerNum' : 'index $_playerIndex'}',
                  style: GoogleFonts.inter(fontSize: 12, color: Colors.black45),
                ),
              ],
            ),
          ),
          _playersCard(),
          _gameControls(),
          if (_showDebug)
            _sectionCard(
              title: 'Debug',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (_state != null)
                    Text(
                      const JsonEncoder.withIndent('  ').convert(_state),
                      style: const TextStyle(fontFamily: 'monospace', fontSize: 11),
                    ),
                  ..._debugEvents.take(8).map(
                        (e) => Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(e, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontFamily: 'monospace', fontSize: 10, color: Colors.black54)),
                        ),
                      ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
