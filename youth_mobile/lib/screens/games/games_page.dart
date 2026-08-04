import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class GamesPage extends StatefulWidget {
  const GamesPage({super.key});

  @override
  State<GamesPage> createState() => _GamesPageState();
}

class _GamesPageState extends State<GamesPage> {
  static const _games = [
    _GameInfo('ludo', 'Ludo', Icons.casino, Colors.orange),
    _GameInfo('chess', 'Chess', Icons.grid_4x4, Colors.brown),
    _GameInfo('uno', 'UNO', Icons.style, Colors.red),
    _GameInfo('snake', 'Snake', Icons.games, Colors.green),
    _GameInfo('rps', 'Rock Paper Scissors', Icons.back_hand, Colors.purple),
  ];

  bool _busy = false;

  Future<void> _createRoom(_GameInfo game) async {
    final nameCtrl = TextEditingController(text: context.read<AuthProvider>().user?.username ?? '');
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Create ${game.label} Room'),
        content: TextField(
          controller: nameCtrl,
          decoration: AppTheme.dashboardInput('Player name'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Create')),
        ],
      ),
    );
    final playerName = nameCtrl.text.trim();
    nameCtrl.dispose();
    if (confirmed != true) return;

    setState(() => _busy = true);
    try {
      final res = await AppApi.createGameRoom(game.apiName, playerName: playerName.isEmpty ? null : playerName);
      if (!mounted) return;
      _showRoomResult(game.label, res);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _joinRoom(_GameInfo game) async {
    final roomCtrl = TextEditingController();
    final nameCtrl = TextEditingController(text: context.read<AuthProvider>().user?.username ?? '');
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Join ${game.label} Room'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: roomCtrl, decoration: AppTheme.dashboardInput('Room ID')),
            const SizedBox(height: 12),
            TextField(controller: nameCtrl, decoration: AppTheme.dashboardInput('Player name')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Join')),
        ],
      ),
    );
    final roomId = roomCtrl.text.trim();
    final playerName = nameCtrl.text.trim();
    roomCtrl.dispose();
    nameCtrl.dispose();
    if (confirmed != true || roomId.isEmpty) return;

    setState(() => _busy = true);
    try {
      final res = await AppApi.joinGameRoom(
        game.apiName,
        roomId: roomId,
        playerName: playerName.isEmpty ? null : playerName,
      );
      if (!mounted) return;
      _showRoomResult(game.label, res);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _showRoomResult(String gameName, Map<String, dynamic> res) {
    final roomId = res['roomId']?.toString() ??
        res['roomCode']?.toString() ??
        res['code']?.toString() ??
        res['id']?.toString() ??
        '—';
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('$gameName Room'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Room ID: $roomId', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18)),
            if (res['message'] != null) ...[
              const SizedBox(height: 8),
              Text(res['message'].toString()),
            ],
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('OK')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Games', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: _busy
          ? const Center(child: CircularProgressIndicator())
          : GridView.builder(
              padding: const EdgeInsets.all(16),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                childAspectRatio: 0.95,
              ),
              itemCount: _games.length,
              itemBuilder: (context, index) {
                final game = _games[index];
                return Card(
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () => _showGameActions(game),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          CircleAvatar(
                            radius: 28,
                            backgroundColor: game.color.withValues(alpha: 0.15),
                            child: Icon(game.icon, color: game.color, size: 28),
                          ),
                          const SizedBox(height: 12),
                          Text(game.label, style: GoogleFonts.outfit(fontWeight: FontWeight.bold), textAlign: TextAlign.center),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
    );
  }

  void _showGameActions(_GameInfo game) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.add_circle, color: Colors.blueAccent),
              title: const Text('Create Room'),
              onTap: () {
                Navigator.pop(ctx);
                _createRoom(game);
              },
            ),
            ListTile(
              leading: const Icon(Icons.login, color: Colors.green),
              title: const Text('Join Room'),
              onTap: () {
                Navigator.pop(ctx);
                _joinRoom(game);
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _GameInfo {
  const _GameInfo(this.apiName, this.label, this.icon, this.color);
  final String apiName;
  final String label;
  final IconData icon;
  final Color color;
}
