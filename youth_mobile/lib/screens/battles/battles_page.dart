import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class BattlesPage extends StatefulWidget {
  const BattlesPage({super.key});

  @override
  State<BattlesPage> createState() => _BattlesPageState();
}

class _BattlesPageState extends State<BattlesPage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  List<Map<String, dynamic>> _active = [];
  List<Map<String, dynamic>> _completed = [];
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
      final data = await AppApi.battles();
      if (!mounted) return;
      setState(() {
        _active = (data['active'] as List? ?? []).cast<Map<String, dynamic>>();
        _completed = (data['completed'] as List? ?? []).cast<Map<String, dynamic>>();
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

  Widget _battleList(List<Map<String, dynamic>> items) {
    if (items.isEmpty) {
      return Center(child: Text('No battles', style: GoogleFonts.inter(color: Colors.grey)));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: items.length,
      itemBuilder: (context, index) {
        final b = items[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 10),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: Colors.blueAccent.withValues(alpha: 0.15),
              child: const Icon(Icons.sports_martial_arts, color: Colors.blueAccent),
            ),
            title: Text(b['title']?.toString() ?? 'Battle', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Status: ${b['status'] ?? ''}'),
                if (b['roomCode'] != null) Text('Room: ${b['roomCode']}'),
                if (b['participantCount'] != null) Text('Participants: ${b['participantCount']}'),
                if (b['winnerUsername'] != null) Text('Winner: ${b['winnerUsername']}'),
              ],
            ),
            isThreeLine: true,
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
        title: Text('Battles', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        bottom: TabBar(
          controller: _tabCtrl,
          labelColor: Colors.blueAccent,
          tabs: [
            Tab(text: 'Active (${_active.length})'),
            Tab(text: 'Completed (${_completed.length})'),
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
                    children: [
                      _battleList(_active),
                      _battleList(_completed),
                    ],
                  ),
                ),
    );
  }
}
