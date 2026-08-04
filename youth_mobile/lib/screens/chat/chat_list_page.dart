import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import 'chat_thread_page.dart';

class ChatListPage extends StatefulWidget {
  const ChatListPage({super.key});

  @override
  State<ChatListPage> createState() => _ChatListPageState();
}

class _ChatListPageState extends State<ChatListPage> {
  List<Map<String, dynamic>> _conversations = [];
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
      final list = await AppApi.conversations();
      if (!mounted) return;
      setState(() {
        _conversations = list;
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

  int? _conversationId(Map<String, dynamic> c) {
    final id = c['id'] ?? c['conversationId'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    return null;
  }

  String _title(Map<String, dynamic> c) {
    return c['title']?.toString() ??
        c['name']?.toString() ??
        c['otherUsername']?.toString() ??
        c['username']?.toString() ??
        'Conversation';
  }

  String _lastMessage(Map<String, dynamic> c) {
    final last = c['lastMessage'];
    if (last is Map) return last['content']?.toString() ?? last['text']?.toString() ?? '';
    return last?.toString() ?? c['lastMessageText']?.toString() ?? '';
  }

  int? _otherUserId(Map<String, dynamic> c) {
    final id = c['otherUserId'] ?? c['receiverId'] ?? c['userId'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    if (c['otherUser'] is Map) {
      final ou = c['otherUser'] as Map;
      final ouId = ou['id'];
      if (ouId is num) return ouId.toInt();
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_error!, style: const TextStyle(color: Colors.red)),
            ElevatedButton(onPressed: _load, child: const Text('Retry')),
          ],
        ),
      );
    }
    if (_conversations.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.4,
              child: Center(child: Text('No conversations', style: GoogleFonts.inter(color: Colors.grey))),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(8),
        itemCount: _conversations.length,
        itemBuilder: (context, index) {
          final c = _conversations[index];
          final id = _conversationId(c);
          return Card(
            margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 4),
            child: ListTile(
              leading: CircleAvatar(
                child: Text(_title(c).isNotEmpty ? _title(c)[0].toUpperCase() : '?'),
              ),
              title: Text(_title(c), style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
              subtitle: Text(
                _lastMessage(c),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              onTap: id == null
                  ? () => AppTheme.showError(context, 'Invalid conversation')
                  : () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => ChatThreadPage(
                            conversationId: id,
                            title: _title(c),
                            otherUserId: _otherUserId(c),
                          ),
                        ),
                      ),
            ),
          );
        },
      ),
    );
  }
}
