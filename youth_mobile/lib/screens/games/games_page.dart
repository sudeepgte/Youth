import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import 'game_webview_page.dart';

/// Games lobby matching web `games.html`: multiplayer host/join + solo play.
/// Boards still run in WebView (same HTML/JS as website).
class GamesPage extends StatefulWidget {
  const GamesPage({super.key});

  @override
  State<GamesPage> createState() => _GamesPageState();
}

class _GamesPageState extends State<GamesPage> {
  final _joinCodeCtrl = TextEditingController();
  _MpGame? _joinTarget;

  static const _multiplayer = [
    _MpGame(
      title: 'Chess Grandmaster',
      blurb: 'Strategic 1v1 chess with live moves and chat.',
      path: '/play-chess',
      apiKey: 'chess',
      icon: Icons.grid_on,
      color: Color(0xFF8B5E3C),
      playerCounts: [2],
      playersLabel: '2 players',
    ),
    _MpGame(
      title: 'Ludo King',
      blurb: 'Classic board race — host a table and invite friends.',
      path: '/play-ludo',
      apiKey: 'ludo',
      icon: Icons.casino,
      color: Color(0xFFF59E0B),
      playerCounts: [2, 4],
      playersLabel: '2 or 4 players',
    ),
    _MpGame(
      title: 'Uno Realistic',
      blurb: 'Fast card battles with UNO calls and catch.',
      path: '/play-uno',
      apiKey: 'uno',
      icon: Icons.style,
      color: Color(0xFFEF4444),
      playerCounts: [2, 3, 4],
      playersLabel: '2–4 players',
    ),
    _MpGame(
      title: 'Snake & Ladder',
      blurb: 'Climb ladders, dodge snakes — up to 4 players.',
      path: '/games/snake-and-ladder',
      apiKey: 'snake',
      icon: Icons.timeline,
      color: Color(0xFF22C55E),
      playerCounts: [2, 3, 4],
      playersLabel: '2–4 players',
    ),
    _MpGame(
      title: 'Rock Paper Scissors',
      blurb: 'Best-of rounds — quick 1v1 challenge.',
      path: '/games/rock-paper-scissors',
      apiKey: 'rps',
      icon: Icons.back_hand,
      color: Color(0xFFA855F7),
      playerCounts: [2],
      playersLabel: '2 players',
    ),
  ];

  static const _solo = [
    _SoloGame(
      title: 'Super Mario',
      blurb: 'Side-scrolling run',
      path: '/play-mario',
      icon: Icons.directions_run,
      color: Color(0xFFE11D48),
    ),
    _SoloGame(
      title: 'Memory Match',
      blurb: 'Flip and match cards',
      path: '/play-memory',
      icon: Icons.psychology,
      color: Color(0xFF0EA5E9),
    ),
    _SoloGame(
      title: 'Bubble Shooter',
      blurb: 'Arcade bubble pop',
      path: '/play-bubble-shooter',
      icon: Icons.bubble_chart,
      color: Color(0xFF06B6D4),
    ),
    _SoloGame(
      title: 'Candy Crush',
      blurb: 'Match-3 puzzle',
      path: '/play-candy-crush',
      icon: Icons.cake,
      color: Color(0xFFEC4899),
    ),
    _SoloGame(
      title: 'Zentrix Runner',
      blurb: 'Endless runner',
      path: '/play-runner',
      icon: Icons.speed,
      color: Color(0xFF14B8A6),
    ),
    _SoloGame(
      title: 'Zentrix Racing',
      blurb: 'Drive and score',
      path: '/play-car-game',
      icon: Icons.directions_car,
      color: Color(0xFF6366F1),
    ),
  ];

  @override
  void dispose() {
    _joinCodeCtrl.dispose();
    super.dispose();
  }

  String get _playerName {
    final u = context.read<AuthProvider>().user;
    final name = u?.username.trim();
    if (name != null && name.isNotEmpty) return name;
    return 'Guest_${DateTime.now().millisecondsSinceEpoch % 10000}';
  }

  void _openWeb(String title, String path, {Map<String, String> query = const {}, String? gameKey, String? roomId}) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => GameWebViewPage(
          title: title,
          path: path,
          query: query,
          gameKey: gameKey,
          roomId: roomId,
        ),
      ),
    );
  }

  Future<void> _openLobby(_MpGame game, {bool skipToHost = false}) async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => _GameLobbySheet(
        game: game,
        playerName: _playerName,
        skipToHost: skipToHost,
        onStart: (roomId) {
          Navigator.pop(ctx);
          _openWeb(game.title, game.path, query: {'room': roomId}, gameKey: game.apiKey, roomId: roomId);
        },
        onJoin: (roomId) async {
          Navigator.pop(ctx);
          try {
            await AppApi.joinGameRoom(game.apiKey, roomId: roomId, playerName: _playerName);
          } catch (_) {
            // Board URL still works if join API is optional
          }
          if (!mounted) return;
          _openWeb(game.title, game.path, query: {'room': roomId}, gameKey: game.apiKey, roomId: roomId);
        },
      ),
    );
  }

  void _joinFromStrip() {
    final code = _joinCodeCtrl.text.trim().toUpperCase();
    if (code.length < 4) {
      AppTheme.showError(context, 'Enter a valid room code');
      return;
    }
    final game = _joinTarget ?? _multiplayer.firstWhere((g) => g.apiKey == 'chess');
    _openWeb(game.title, game.path, query: {'room': code});
  }

  @override
  Widget build(BuildContext context) {
    return DashboardScaffold(
      active: AppDrawerItem.games,
      title: 'Esports & Games',
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        children: [
          Text(
            'Host a room, share the code, or jump into solo arcade — same boards as the website.',
            style: GoogleFonts.inter(color: AppTheme.textMuted, height: 1.4),
          ),
          const SizedBox(height: 16),
          _roomCodeStrip(),
          const SizedBox(height: 20),
          _sectionTitle('Multiplayer'),
          const SizedBox(height: 10),
          ..._multiplayer.map(_mpCard),
          const SizedBox(height: 20),
          _sectionTitle('Solo Arcade'),
          const SizedBox(height: 10),
          ..._solo.map(_soloCard),
        ],
      ),
    );
  }

  Widget _sectionTitle(String text) {
    return Text(
      text.toUpperCase(),
      style: GoogleFonts.inter(
        fontSize: 12,
        fontWeight: FontWeight.w800,
        letterSpacing: 0.8,
        color: AppTheme.textMuted,
      ),
    );
  }

  Widget _roomCodeStrip() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: AppTheme.cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Have a Room Code?', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
          const SizedBox(height: 4),
          Text(
            'Join a friend’s match with their 6-character code.',
            style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted),
          ),
          const SizedBox(height: 10),
          DropdownButtonFormField<_MpGame>(
            initialValue: _joinTarget ?? _multiplayer.firstWhere((g) => g.apiKey == 'chess'),
            decoration: AppTheme.dashboardInput('Game'),
            items: _multiplayer
                .map((g) => DropdownMenuItem(value: g, child: Text(g.title)))
                .toList(),
            onChanged: (v) => setState(() => _joinTarget = v),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _joinCodeCtrl,
                  textCapitalization: TextCapitalization.characters,
                  maxLength: 6,
                  decoration: AppTheme.dashboardInput('Room code').copyWith(counterText: ''),
                ),
              ),
              const SizedBox(width: 10),
              ElevatedButton(
                onPressed: _joinFromStrip,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                ),
                child: Text('Join Match', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _mpCard(_MpGame game) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Container(
        decoration: AppTheme.cardDecoration(),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 52,
                    height: 52,
                    decoration: BoxDecoration(
                      color: game.color.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: Icon(game.icon, color: game.color),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(game.title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 17)),
                        const SizedBox(height: 2),
                        Text(game.blurb, style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted, height: 1.35)),
                        const SizedBox(height: 4),
                        Text(
                          game.playersLabel,
                          style: GoogleFonts.outfit(fontSize: 11, fontWeight: FontWeight.w700, color: AppTheme.primary),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () => _openLobby(game),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFFBBF24),
                        foregroundColor: const Color(0xFF1F2937),
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                      child: Text('Play Now', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => _openLobby(game, skipToHost: true),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppTheme.primary,
                        side: const BorderSide(color: AppTheme.primary),
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                      ),
                      child: Text('Invite', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
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

  Widget _soloCard(_SoloGame game) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          onTap: () => _openWeb(game.title, game.path),
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: game.color.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(game.icon, color: game.color),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(game.title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
                      Text(game.blurb, style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
                    ],
                  ),
                ),
                ElevatedButton(
                  onPressed: () => _openWeb(game.title, game.path),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFBBF24),
                    foregroundColor: const Color(0xFF1F2937),
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  ),
                  child: Text('Play', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _MpGame {
  const _MpGame({
    required this.title,
    required this.blurb,
    required this.path,
    required this.apiKey,
    required this.icon,
    required this.color,
    required this.playerCounts,
    required this.playersLabel,
  });

  final String title;
  final String blurb;
  final String path;
  final String apiKey;
  final IconData icon;
  final Color color;
  final List<int> playerCounts;
  final String playersLabel;
}

class _SoloGame {
  const _SoloGame({
    required this.title,
    required this.blurb,
    required this.path,
    required this.icon,
    required this.color,
  });

  final String title;
  final String blurb;
  final String path;
  final IconData icon;
  final Color color;
}

class _GameLobbySheet extends StatefulWidget {
  const _GameLobbySheet({
    required this.game,
    required this.playerName,
    required this.onStart,
    required this.onJoin,
    this.skipToHost = false,
  });

  final _MpGame game;
  final String playerName;
  final bool skipToHost;
  final ValueChanged<String> onStart;
  final ValueChanged<String> onJoin;

  @override
  State<_GameLobbySheet> createState() => _GameLobbySheetState();
}

class _GameLobbySheetState extends State<_GameLobbySheet> {
  late String _step; // select | host | created | join
  late int _maxPlayers;
  final _codeCtrl = TextEditingController();
  String? _createdCode;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _maxPlayers = widget.game.playerCounts.first;
    _step = widget.skipToHost ? 'host' : 'select';
  }

  @override
  void dispose() {
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _create() async {
    setState(() => _busy = true);
    try {
      final res = await AppApi.createGameRoom(
        widget.game.apiKey,
        playerName: widget.playerName,
        maxPlayers: _maxPlayers,
      );
      final code = (res['roomId'] ?? res['room'] ?? '').toString().toUpperCase();
      if (code.isEmpty) throw Exception('No room code returned');
      if (!mounted) return;
      setState(() {
        _createdCode = code;
        _step = 'created';
      });
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
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(color: Colors.black26, borderRadius: BorderRadius.circular(4)),
                ),
              ),
              const SizedBox(height: 14),
              Text(widget.game.title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 20)),
              const SizedBox(height: 4),
              Text(
                'Playing as ${widget.playerName}',
                style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted),
              ),
              const SizedBox(height: 16),
              if (_step == 'select') ..._selectStep(),
              if (_step == 'host') ..._hostStep(),
              if (_step == 'created') ..._createdStep(),
              if (_step == 'join') ..._joinStep(),
            ],
          ),
        ),
      ),
    );
  }

  List<Widget> _selectStep() => [
        ElevatedButton.icon(
          onPressed: () => setState(() => _step = 'host'),
          icon: const Icon(Icons.add_home_rounded),
          label: Text('Host Room', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.primary,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 14),
          ),
        ),
        const SizedBox(height: 10),
        OutlinedButton.icon(
          onPressed: () => setState(() => _step = 'join'),
          icon: const Icon(Icons.login_rounded),
          label: Text('Join with Code', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
          style: OutlinedButton.styleFrom(
            foregroundColor: AppTheme.primary,
            padding: const EdgeInsets.symmetric(vertical: 14),
          ),
        ),
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Close')),
      ];

  List<Widget> _hostStep() => [
        Text('Players', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          children: widget.game.playerCounts.map((n) {
            final selected = _maxPlayers == n;
            return ChoiceChip(
              label: Text('$n'),
              selected: selected,
              onSelected: (_) => setState(() => _maxPlayers = n),
              selectedColor: AppTheme.primary.withValues(alpha: 0.2),
              labelStyle: GoogleFonts.outfit(
                fontWeight: FontWeight.w700,
                color: selected ? AppTheme.primary : AppTheme.textSecondary,
              ),
            );
          }).toList(),
        ),
        const SizedBox(height: 16),
        ElevatedButton(
          onPressed: _busy ? null : _create,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.primary,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 14),
          ),
          child: _busy
              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
              : Text('Create Room', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        ),
        TextButton(
          onPressed: widget.skipToHost ? () => Navigator.pop(context) : () => setState(() => _step = 'select'),
          child: const Text('Back'),
        ),
      ];

  List<Widget> _createdStep() => [
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: const Color(0xFFF0F9FF),
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: const Color(0xFFBFDBFE)),
          ),
          child: Column(
            children: [
              Text('Room code', style: GoogleFonts.inter(color: AppTheme.textMuted, fontSize: 12)),
              const SizedBox(height: 6),
              Text(
                _createdCode ?? '',
                style: GoogleFonts.outfit(fontWeight: FontWeight.w900, fontSize: 32, letterSpacing: 4, color: AppTheme.primary),
              ),
              TextButton.icon(
                onPressed: () async {
                  await Clipboard.setData(ClipboardData(text: _createdCode ?? ''));
                  if (mounted) AppTheme.showSuccess(context, 'Code copied');
                },
                icon: const Icon(Icons.copy_rounded, size: 16),
                label: const Text('Copy code'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        ElevatedButton(
          onPressed: () => widget.onStart(_createdCode!),
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFFFBBF24),
            foregroundColor: const Color(0xFF1F2937),
            padding: const EdgeInsets.symmetric(vertical: 14),
          ),
          child: Text('Start Game', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
        ),
      ];

  List<Widget> _joinStep() => [
        TextField(
          controller: _codeCtrl,
          textCapitalization: TextCapitalization.characters,
          maxLength: 6,
          decoration: AppTheme.dashboardInput('Room code'),
        ),
        const SizedBox(height: 8),
        ElevatedButton(
          onPressed: () {
            final code = _codeCtrl.text.trim().toUpperCase();
            if (code.length < 4) {
              AppTheme.showError(context, 'Enter a valid room code');
              return;
            }
            widget.onJoin(code);
          },
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.primary,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 14),
          ),
          child: Text('Join & Play', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        ),
        TextButton(onPressed: () => setState(() => _step = 'select'), child: const Text('Back')),
      ];
}
