import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';

class RequestsPage extends StatefulWidget {
  const RequestsPage({super.key});

  @override
  State<RequestsPage> createState() => _RequestsPageState();
}

class _RequestsPageState extends State<RequestsPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabs;
  List<Map<String, dynamic>> _follow = [];
  List<Map<String, dynamic>> _collabs = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabs = TabController(length: 2, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await Future.wait([
        AppApi.followRequests(),
        AppApi.collaborationRequests(),
      ]);
      if (!mounted) return;
      setState(() {
        _follow = result[0];
        _collabs = result[1];
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

  Future<void> _handleFollow(int id, bool accept) async {
    try {
      if (accept) {
        await AppApi.acceptFollowRequest(id);
      } else {
        await AppApi.rejectFollowRequest(id);
      }
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _handleCollab(int id, bool accept) async {
    try {
      if (accept) {
        await AppApi.acceptCollaboration(id);
      } else {
        await AppApi.rejectCollaboration(id);
      }
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Widget _followList() {
    if (_follow.isEmpty) {
      return Center(
        child: Text('No follow requests', style: GoogleFonts.inter(color: Colors.grey)),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _follow.length,
      itemBuilder: (_, i) {
        final r = _follow[i];
        final id = (r['id'] as num).toInt();
        return Card(
          child: ListTile(
            title: Text('@${r['senderUsername'] ?? 'user'}',
                style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
            subtitle: Text('Wants to follow you', style: GoogleFonts.inter()),
            trailing: Wrap(
              spacing: 8,
              children: [
                TextButton(onPressed: () => _handleFollow(id, false), child: const Text('Reject')),
                ElevatedButton(onPressed: () => _handleFollow(id, true), child: const Text('Accept')),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _collabList() {
    if (_collabs.isEmpty) {
      return Center(
        child: Text('No collaboration requests', style: GoogleFonts.inter(color: Colors.grey)),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _collabs.length,
      itemBuilder: (_, i) {
        final r = _collabs[i];
        final id = (r['id'] as num).toInt();
        final from = r['fromUsername']?.toString() ?? 'user';
        return Card(
          child: ListTile(
            title: Text('@$from invited you', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
            subtitle: Text(
              r['postContent']?.toString() ?? 'Collaboration invite',
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: GoogleFonts.inter(),
            ),
            trailing: Wrap(
              spacing: 8,
              children: [
                TextButton(onPressed: () => _handleCollab(id, false), child: const Text('Reject')),
                ElevatedButton(onPressed: () => _handleCollab(id, true), child: const Text('Accept')),
              ],
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      drawer: const AppDrawer(active: AppDrawerItem.requests),
      appBar: AppBar(
        title: Text('Requests', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        leading: Builder(
          builder: (ctx) => IconButton(
            icon: const Icon(Icons.menu, color: AppTheme.textPrimary),
            onPressed: () => Scaffold.of(ctx).openDrawer(),
          ),
        ),
        actions: [
          IconButton(
            tooltip: 'Back',
            icon: const Icon(Icons.arrow_back_rounded),
            onPressed: () => Navigator.maybePop(context),
          ),
        ],
        bottom: TabBar(
          controller: _tabs,
          tabs: const [
            Tab(text: 'Follow'),
            Tab(text: 'Collaboration'),
          ],
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: const TextStyle(color: Colors.red)))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: TabBarView(
                    controller: _tabs,
                    children: [
                      _followList(),
                      _collabList(),
                    ],
                  ),
                ),
    );
  }
}
