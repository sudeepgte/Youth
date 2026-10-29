import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class MusicLeaderboardPage extends StatefulWidget {
  const MusicLeaderboardPage({super.key});

  @override
  State<MusicLeaderboardPage> createState() => _MusicLeaderboardPageState();
}

class _MusicLeaderboardPageState extends State<MusicLeaderboardPage> {
  List<Map<String, dynamic>> _voters = [];
  List<Map<String, dynamic>> _hosts = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await AppApi.musicLeaderboard();
      if (!mounted) return;
      setState(() {
        _voters = (data['topVoters'] as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList();
        _hosts = (data['topHosts'] as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList();
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

  Widget _section(String title, List<Map<String, dynamic>> rows, String countLabel) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
          const SizedBox(height: 12),
          if (rows.isEmpty)
            Text('No data yet', style: GoogleFonts.inter(color: AppTheme.textMuted))
          else
            ...rows.asMap().entries.map((e) {
              final i = e.key + 1;
              final u = e.value;
              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: Row(
                  children: [
                    SizedBox(
                      width: 28,
                      child: Text(
                        '#$i',
                        style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary),
                      ),
                    ),
                    Expanded(
                      child: Text(
                        u['username']?.toString() ?? 'User',
                        style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
                      ),
                    ),
                    Text(
                      '${u['count'] ?? 0} $countLabel',
                      style: GoogleFonts.inter(color: AppTheme.textMuted, fontSize: 13),
                    ),
                  ],
                ),
              );
            }),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Music Leaderboard', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
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
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      _section('Top voters', _voters, 'votes'),
                      _section('Top hosts', _hosts, 'rooms'),
                    ],
                  ),
                ),
    );
  }
}
