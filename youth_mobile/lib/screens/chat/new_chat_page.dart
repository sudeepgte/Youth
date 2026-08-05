import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import 'chat_thread_page.dart';

class NewChatPage extends StatefulWidget {
  const NewChatPage({super.key});

  @override
  State<NewChatPage> createState() => _NewChatPageState();
}

class _NewChatPageState extends State<NewChatPage> {
  final _searchCtrl = TextEditingController();
  List<Map<String, dynamic>> _users = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _load({String query = ''}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await AppApi.chatUsers(query: query.trim());
      if (!mounted) return;
      setState(() {
        _users = list;
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

  int? _userId(Map<String, dynamic> u) {
    final id = u['id'] ?? u['userId'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    return null;
  }

  String _username(Map<String, dynamic> u) =>
      u['username']?.toString() ?? u['name']?.toString() ?? 'User';

  Future<void> _startChat(Map<String, dynamic> u) async {
    final id = _userId(u);
    if (id == null) {
      AppTheme.showError(context, 'Invalid user');
      return;
    }
    final title = _username(u);
    final photo = u['profilePhotoUrl']?.toString() ?? u['profilePicture']?.toString();

    // Reuse existing conversation when present
    try {
      final conversations = await AppApi.conversations();
      for (final c in conversations) {
        final parts = c['participants'];
        if (parts is List) {
          for (final p in parts) {
            if (p is! Map) continue;
            final pid = p['id'];
            final oid = pid is num ? pid.toInt() : int.tryParse('$pid');
            if (oid == id) {
              final cid = c['id'];
              final conversationId = cid is num ? cid.toInt() : int.tryParse('$cid');
              if (conversationId != null && mounted) {
                await Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(
                    builder: (_) => ChatThreadPage(
                      conversationId: conversationId,
                      title: title,
                      otherUserId: id,
                      photoUrl: photo,
                      status: c['status']?.toString(),
                    ),
                  ),
                );
                return;
              }
            }
          }
        }
      }
    } catch (_) {}

    if (!mounted) return;
    await Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (_) => ChatThreadPage(
          title: title,
          otherUserId: id,
          photoUrl: photo,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('New message', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchCtrl,
              decoration: AppTheme.dashboardInput('Search users').copyWith(
                suffixIcon: IconButton(
                  icon: const Icon(Icons.search),
                  onPressed: () => _load(query: _searchCtrl.text),
                ),
              ),
              onSubmitted: (q) => _load(query: q),
            ),
          ),
          Expanded(
            child: _loading
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
                    : _users.isEmpty
                        ? Center(child: Text('No users found', style: GoogleFonts.inter(color: Colors.grey)))
                        : RefreshIndicator(
                            onRefresh: () => _load(query: _searchCtrl.text),
                            child: ListView.separated(
                              padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                              itemCount: _users.length,
                              separatorBuilder: (_, _) => const SizedBox(height: 8),
                              itemBuilder: (context, index) {
                                final u = _users[index];
                                final name = _username(u);
                                final photo = u['profilePhotoUrl']?.toString();
                                return Material(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.circular(14),
                                  child: InkWell(
                                    borderRadius: BorderRadius.circular(14),
                                    onTap: () => _startChat(u),
                                    child: Padding(
                                      padding: const EdgeInsets.all(12),
                                      child: Row(
                                        children: [
                                          CircleAvatar(
                                            backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                                            backgroundImage: photo != null && photo.isNotEmpty
                                                ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                                                : null,
                                            child: photo == null || photo.isEmpty
                                                ? Text(
                                                    name.isNotEmpty ? name[0].toUpperCase() : '?',
                                                    style: GoogleFonts.outfit(
                                                      fontWeight: FontWeight.w800,
                                                      color: AppTheme.primary,
                                                    ),
                                                  )
                                                : null,
                                          ),
                                          const SizedBox(width: 12),
                                          Expanded(
                                            child: Column(
                                              crossAxisAlignment: CrossAxisAlignment.start,
                                              children: [
                                                Text(
                                                  name,
                                                  style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 16),
                                                ),
                                                if (u['collegeName'] != null || u['email'] != null)
                                                  Text(
                                                    (u['collegeName'] ?? u['email']).toString(),
                                                    style: GoogleFonts.inter(fontSize: 12, color: Colors.black45),
                                                  ),
                                              ],
                                            ),
                                          ),
                                          const Icon(Icons.chat_bubble_outline, color: AppTheme.primary, size: 20),
                                        ],
                                      ),
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
