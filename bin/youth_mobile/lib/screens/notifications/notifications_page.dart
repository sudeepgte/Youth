import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../profile/profile_page.dart';
import '../social/requests_page.dart';

class NotificationsPage extends StatefulWidget {
  const NotificationsPage({super.key});

  @override
  State<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends State<NotificationsPage> {
  List<Map<String, dynamic>> _notifications = [];
  int _pendingFollows = 0;
  int _pendingCollabs = 0;
  bool _loading = true;
  bool _markingRead = false;
  bool _clearing = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load(autoMark: true);
  }

  Future<void> _load({bool autoMark = false}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait<List<Map<String, dynamic>>>([
        AppApi.notifications(),
        AppApi.followRequests().catchError((_) => <Map<String, dynamic>>[]),
        AppApi.collaborationRequests().catchError((_) => <Map<String, dynamic>>[]),
      ]);
      if (!mounted) return;
      final list = results[0];
      final follows = results[1];
      final collabs = results[2];
      setState(() {
        _notifications = list;
        _pendingFollows = follows.length;
        _pendingCollabs = collabs.length;
        _loading = false;
      });
      if (autoMark && list.any((n) => n['read'] != true && n['isRead'] != true)) {
        _markAllRead(silent: true);
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppTheme.extractError(e);
        _loading = false;
      });
    }
  }

  Future<void> _markAllRead({bool silent = false}) async {
    if (!silent) setState(() => _markingRead = true);
    try {
      await AppApi.markNotificationsRead();
      if (!mounted) return;
      setState(() {
        _notifications = _notifications.map((n) {
          final copy = Map<String, dynamic>.from(n);
          copy['isRead'] = true;
          copy['read'] = true;
          return copy;
        }).toList();
      });
      if (!silent && mounted) AppTheme.showSuccess(context, 'All notifications marked as read');
    } catch (e) {
      if (!silent && mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted && !silent) setState(() => _markingRead = false);
    }
  }

  Future<void> _clearAll() async {
    setState(() => _clearing = true);
    try {
      await AppApi.clearNotifications();
      await _load();
      if (mounted) AppTheme.showSuccess(context, 'All notifications cleared');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _clearing = false);
    }
  }

  IconData _iconForType(String? type) {
    switch (type?.toUpperCase()) {
      case 'FOLLOW':
      case 'FOLLOW_REQUEST':
        return Icons.person_add;
      case 'LIKE':
        return Icons.favorite;
      case 'COMMENT':
        return Icons.comment;
      case 'EVENT':
        return Icons.event;
      case 'COLLAB':
      case 'COLLABORATION':
        return Icons.group_add_outlined;
      default:
        return Icons.notifications;
    }
  }

  String? _extractFollowUsername(Map<String, dynamic> n) {
    final type = n['type']?.toString().toUpperCase();
    if (type != 'FOLLOW' && type != 'FOLLOW_REQUEST') return null;

    final direct = n['username']?.toString() ??
        n['fromUsername']?.toString() ??
        n['actorUsername']?.toString() ??
        n['senderUsername']?.toString();
    if (direct != null && direct.isNotEmpty) return direct;

    final actor = n['actor'] ?? n['fromUser'] ?? n['user'] ?? n['sender'];
    if (actor is Map) {
      final u = actor['username']?.toString();
      if (u != null && u.isNotEmpty) return u;
    }

    final message = n['message']?.toString() ?? n['content']?.toString() ?? '';
    final match = RegExp(r'^@?([A-Za-z0-9_.-]+)\s+(started\s+following|followed)', caseSensitive: false)
        .firstMatch(message);
    if (match != null) return match.group(1);

    final at = RegExp(r'@([A-Za-z0-9_.-]+)').firstMatch(message);
    if (at != null) return at.group(1);

    return null;
  }

  void _onTap(Map<String, dynamic> n) {
    final type = n['type']?.toString().toUpperCase() ?? '';
    if (type.contains('COLLAB')) {
      Navigator.push(context, MaterialPageRoute(builder: (_) => const RequestsPage()));
      return;
    }
    final username = _extractFollowUsername(n);
    if (username == null || username.isEmpty) return;
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => ProfilePage(username: username)),
    );
  }

  Widget _pendingBanner() {
    final total = _pendingFollows + _pendingCollabs;
    if (total == 0) return const SizedBox.shrink();
    return Material(
      color: const Color(0xFFEEF2FF),
      child: InkWell(
        onTap: () async {
          await Navigator.push(context, MaterialPageRoute(builder: (_) => const RequestsPage()));
          if (mounted) _load();
        },
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Row(
            children: [
              const Icon(Icons.inbox_outlined, color: Color(0xFF4338CA)),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  [
                    if (_pendingFollows > 0) '$_pendingFollows follow',
                    if (_pendingCollabs > 0) '$_pendingCollabs collab',
                  ].join(' · ') +
                      ' request${total == 1 ? '' : 's'} pending',
                  style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: const Color(0xFF3730A3)),
                ),
              ),
              const Icon(Icons.chevron_right, color: Color(0xFF4338CA)),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Notifications', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        actions: [
          if (_notifications.isNotEmpty)
            TextButton(
              onPressed: _markingRead ? null : () => _markAllRead(),
              child: _markingRead
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : Text('Mark all read', style: GoogleFonts.inter(color: Colors.blueAccent)),
            ),
          if (_notifications.isNotEmpty)
            TextButton(
              onPressed: _clearing ? null : _clearAll,
              child: _clearing
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : Text('Clear', style: GoogleFonts.inter(color: Colors.redAccent)),
            ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(_error!, style: const TextStyle(color: Colors.red)),
                      ElevatedButton(onPressed: () => _load(), child: const Text('Retry')),
                    ],
                  ),
                )
              : Column(
                  children: [
                    _pendingBanner(),
                    Expanded(
                      child: _notifications.isEmpty
                          ? Center(child: Text('No notifications', style: GoogleFonts.inter(color: Colors.grey)))
                          : RefreshIndicator(
                              onRefresh: () => _load(),
                              child: ListView.separated(
                                padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                                itemCount: _notifications.length,
                                separatorBuilder: (_, __) => const SizedBox(height: 8),
                                itemBuilder: (context, index) {
                                  final n = _notifications[index];
                                  final isRead = n['read'] == true || n['isRead'] == true;
                                  final followUser = _extractFollowUsername(n);
                                  final type = n['type']?.toString().toUpperCase() ?? '';
                                  final tappable = followUser != null || type.contains('COLLAB');
                                  return Dismissible(
                                    key: ValueKey(n['id']?.toString() ?? '$index'),
                                    direction: DismissDirection.endToStart,
                                    background: Container(
                                      alignment: Alignment.centerRight,
                                      padding: const EdgeInsets.symmetric(horizontal: 20),
                                      decoration: BoxDecoration(
                                        color: Colors.redAccent,
                                        borderRadius: BorderRadius.circular(16),
                                      ),
                                      child: const Icon(Icons.delete_outline, color: Colors.white),
                                    ),
                                    onDismissed: (_) async {
                                      final id = (n['id'] as num?)?.toInt();
                                      if (id != null) {
                                        try {
                                          await AppApi.deleteNotification(id);
                                        } catch (_) {}
                                      }
                                    },
                                    child: Material(
                                      color: isRead ? Colors.white : const Color(0xFFEFF6FF),
                                      borderRadius: BorderRadius.circular(16),
                                      child: ListTile(
                                        onTap: tappable ? () => _onTap(n) : null,
                                        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                                        leading: CircleAvatar(
                                          backgroundColor: AppTheme.primary.withValues(alpha: 0.12),
                                          child: Icon(_iconForType(n['type']?.toString()), color: AppTheme.primary, size: 20),
                                        ),
                                        title: Text(
                                          n['message']?.toString() ?? n['content']?.toString() ?? 'Notification',
                                          style: GoogleFonts.inter(fontWeight: isRead ? FontWeight.w500 : FontWeight.w700),
                                        ),
                                        subtitle: Text(
                                          n['createdAt']?.toString() ?? n['timestamp']?.toString() ?? '',
                                          style: GoogleFonts.inter(fontSize: 12, color: Colors.black45),
                                        ),
                                        trailing: tappable
                                            ? const Icon(Icons.chevron_right, color: Colors.black38)
                                            : null,
                                      ),
                                    ),
                                  );
                                },
                              ),
                            ),
                    ),
                  ],
                ),
    );
  }
}
