import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class NotificationsPage extends StatefulWidget {
  const NotificationsPage({super.key});

  @override
  State<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends State<NotificationsPage> {
  List<Map<String, dynamic>> _notifications = [];
  bool _loading = true;
  bool _markingRead = false;
  bool _clearing = false;
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
      final list = await AppApi.notifications();
      if (!mounted) return;
      setState(() {
        _notifications = list;
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

  Future<void> _markAllRead() async {
    setState(() => _markingRead = true);
    try {
      await AppApi.markNotificationsRead();
      await _load();
      if (mounted) AppTheme.showSuccess(context, 'All notifications marked as read');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _markingRead = false);
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
        return Icons.person_add;
      case 'LIKE':
        return Icons.favorite;
      case 'COMMENT':
        return Icons.comment;
      case 'EVENT':
        return Icons.event;
      default:
        return Icons.notifications;
    }
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
              onPressed: _markingRead ? null : _markAllRead,
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
                      ElevatedButton(onPressed: _load, child: const Text('Retry')),
                    ],
                  ),
                )
              : _notifications.isEmpty
                  ? Center(child: Text('No notifications', style: GoogleFonts.inter(color: Colors.grey)))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(8),
                        itemCount: _notifications.length,
                        itemBuilder: (context, index) {
                          final n = _notifications[index];
                          final isRead = n['read'] == true || n['isRead'] == true;
                          return Dismissible(
                            key: ValueKey(n['id']?.toString() ?? '$index'),
                            direction: DismissDirection.endToStart,
                            background: Container(
                              alignment: Alignment.centerRight,
                              padding: const EdgeInsets.symmetric(horizontal: 20),
                              color: Colors.redAccent,
                              child: const Icon(Icons.delete, color: Colors.white),
                            ),
                            onDismissed: (_) async {
                              final id = (n['id'] as num?)?.toInt();
                              if (id != null) {
                                try {
                                  await AppApi.deleteNotification(id);
                                } catch (_) {}
                              }
                            },
                            child: Card(
                              color: isRead ? Colors.white : Colors.blue.shade50,
                              margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 4),
                              child: ListTile(
                                leading: Icon(_iconForType(n['type']?.toString()), color: Colors.blueAccent),
                                title: Text(
                                  n['message']?.toString() ?? n['content']?.toString() ?? 'Notification',
                                  style: GoogleFonts.inter(fontWeight: isRead ? FontWeight.normal : FontWeight.w600),
                                ),
                                subtitle: Text(n['createdAt']?.toString() ?? n['timestamp']?.toString() ?? ''),
                              ),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
