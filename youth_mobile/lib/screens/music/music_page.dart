import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class MusicPage extends StatefulWidget {
  const MusicPage({super.key});

  @override
  State<MusicPage> createState() => _MusicPageState();
}

class _MusicPageState extends State<MusicPage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  List<Map<String, dynamic>> _tracks = [];
  List<Map<String, dynamic>> _rooms = [];
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
      final results = await Future.wait([AppApi.musicTracks(), AppApi.musicRooms()]);
      if (!mounted) return;
      setState(() {
        _tracks = results[0];
        _rooms = results[1];
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

  Widget _tracksList() {
    if (_tracks.isEmpty) {
      return Center(child: Text('No tracks', style: GoogleFonts.inter(color: Colors.grey)));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _tracks.length,
      itemBuilder: (context, index) {
        final t = _tracks[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            leading: const CircleAvatar(child: Icon(Icons.music_note)),
            title: Text(t['title']?.toString() ?? t['name']?.toString() ?? 'Track'),
            subtitle: Text(t['artist']?.toString() ?? t['uploader']?.toString() ?? ''),
            trailing: Text(t['duration']?.toString() ?? '', style: GoogleFonts.inter(fontSize: 12, color: Colors.grey)),
          ),
        );
      },
    );
  }

  Widget _roomsList() {
    if (_rooms.isEmpty) {
      return Center(child: Text('No music rooms', style: GoogleFonts.inter(color: Colors.grey)));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _rooms.length,
      itemBuilder: (context, index) {
        final r = _rooms[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            leading: const CircleAvatar(child: Icon(Icons.headphones)),
            title: Text(r['name']?.toString() ?? r['title']?.toString() ?? 'Room'),
            subtitle: Text('Code: ${r['code'] ?? r['roomCode'] ?? ''} · ${r['participants'] ?? r['memberCount'] ?? 0} members'),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Music', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        bottom: TabBar(
          controller: _tabCtrl,
          labelColor: Colors.blueAccent,
          tabs: [
            Tab(text: 'Tracks (${_tracks.length})'),
            Tab(text: 'Rooms (${_rooms.length})'),
          ],
        ),
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
              : RefreshIndicator(
                  onRefresh: _load,
                  child: TabBarView(
                    controller: _tabCtrl,
                    children: [_tracksList(), _roomsList()],
                  ),
                ),
    );
  }
}
