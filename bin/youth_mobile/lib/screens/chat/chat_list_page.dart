import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../config/api_config.dart';
import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../services/realtime_service.dart';
import '../../theme/app_theme.dart';
import 'chat_thread_page.dart';
import 'new_chat_page.dart';
import 'new_group_page.dart';

class ChatListPage extends StatefulWidget {
  const ChatListPage({super.key});

  @override
  State<ChatListPage> createState() => _ChatListPageState();
}

class _ChatListPageState extends State<ChatListPage> with SingleTickerProviderStateMixin {
  final _searchCtrl = TextEditingController();
  late TabController _tabCtrl;
  List<Map<String, dynamic>> _conversations = [];
  List<Map<String, dynamic>> _searchUsers = [];
  bool _loading = true;
  bool _searchingUsers = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _load();
    _bindRealtime();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _tabCtrl.dispose();
    super.dispose();
  }

  Future<void> _bindRealtime() async {
    await RealtimeService.instance.subscribeJson('/user/queue/conversation-update', (_) {
      if (mounted) _load(silent: true);
    });
    await RealtimeService.instance.subscribeJson('/user/queue/messages', (_) {
      if (mounted) _load(silent: true);
    });
    await RealtimeService.instance.subscribeJson('/user/queue/conversation-delete', (_) {
      if (mounted) _load(silent: true);
    });
  }

  Future<void> _load({bool silent = false}) async {
    if (!silent) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
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

  Future<void> _onSearchChanged(String q) async {
    setState(() {});
    final query = q.trim();
    if (query.length < 2) {
      setState(() => _searchUsers = []);
      return;
    }
    setState(() => _searchingUsers = true);
    try {
      final users = await AppApi.chatUsers(query: query);
      if (!mounted) return;
      setState(() {
        _searchUsers = users;
        _searchingUsers = false;
      });
    } catch (_) {
      if (mounted) setState(() => _searchingUsers = false);
    }
  }

  int? _myId() => context.read<AuthProvider>().user?.id;

  int? _conversationId(Map<String, dynamic> c) {
    final id = c['id'] ?? c['conversationId'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    return null;
  }

  bool _isGroup(Map<String, dynamic> c) {
    final t = (c['type'] ?? '').toString().toUpperCase();
    return t == 'GROUP';
  }

  bool _isPending(Map<String, dynamic> c) {
    return (c['status'] ?? '').toString().toUpperCase() == 'PENDING';
  }

  String _title(Map<String, dynamic> c) {
    if (_isGroup(c) && (c['name']?.toString().isNotEmpty ?? false)) {
      return c['name'].toString();
    }
    final me = _myId();
    final parts = c['participants'];
    if (parts is List) {
      for (final p in parts) {
        if (p is! Map) continue;
        final id = p['id'];
        final pid = id is num ? id.toInt() : int.tryParse('$id');
        if (me != null && pid == me) continue;
        final name = p['username']?.toString();
        if (name != null && name.isNotEmpty) return name;
      }
    }
    return c['title']?.toString() ??
        c['name']?.toString() ??
        c['otherUsername']?.toString() ??
        c['username']?.toString() ??
        'Conversation';
  }

  String? _photo(Map<String, dynamic> c) {
    if (_isGroup(c)) return null;
    final me = _myId();
    final parts = c['participants'];
    if (parts is List) {
      for (final p in parts) {
        if (p is! Map) continue;
        final id = p['id'];
        final pid = id is num ? id.toInt() : int.tryParse('$id');
        if (me != null && pid == me) continue;
        final photo = p['profilePhotoUrl']?.toString() ?? p['profilePicture']?.toString();
        if (photo != null && photo.isNotEmpty) return photo;
      }
    }
    return c['otherPhoto']?.toString() ?? c['profilePhotoUrl']?.toString();
  }

  String _lastMessage(Map<String, dynamic> c) {
    final last = c['lastMessage'];
    if (last is Map) return last['content']?.toString() ?? last['text']?.toString() ?? '';
    return last?.toString() ?? c['lastMessageText']?.toString() ?? '';
  }

  String _timeLabel(Map<String, dynamic> c) {
    final raw = c['lastMessageTime']?.toString();
    if (raw == null || raw.isEmpty) return '';
    try {
      final dt = DateTime.tryParse(raw);
      if (dt == null) return '';
      final local = dt.toLocal();
      final now = DateTime.now();
      if (now.difference(local).inDays == 0) return DateFormat.jm().format(local);
      if (now.difference(local).inDays < 7) return DateFormat.E().format(local);
      return DateFormat.MMMd().format(local);
    } catch (_) {
      return '';
    }
  }

  int? _otherUserId(Map<String, dynamic> c) {
    if (_isGroup(c)) return null;
    final me = _myId();
    final parts = c['participants'];
    if (parts is List) {
      for (final p in parts) {
        if (p is! Map) continue;
        final id = p['id'];
        final pid = id is num ? id.toInt() : int.tryParse('$id');
        if (me != null && pid == me) continue;
        if (pid != null) return pid;
      }
    }
    final id = c['otherUserId'] ?? c['receiverId'] ?? c['userId'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    if (c['otherUser'] is Map) {
      final ouId = (c['otherUser'] as Map)['id'];
      if (ouId is num) return ouId.toInt();
    }
    return null;
  }

  bool _unread(Map<String, dynamic> c) => c['unread'] == true;

  List<Map<String, dynamic>> get _filtered {
    final q = _searchCtrl.text.trim().toLowerCase();
    var list = _conversations;
    if (q.isNotEmpty) {
      list = list.where((c) {
        final title = _title(c).toLowerCase();
        final last = _lastMessage(c).toLowerCase();
        return title.contains(q) || last.contains(q);
      }).toList();
    }
    return list;
  }

  List<Map<String, dynamic>> get _allChats =>
      _filtered.where((c) => !_isPending(c) || _isGroup(c)).toList();

  List<Map<String, dynamic>> get _requests {
    final me = _myId();
    return _filtered.where((c) {
      if (!_isPending(c) || _isGroup(c)) return false;
      // Incoming request: creator is not me
      final creator = c['creator'];
      int? creatorId;
      if (creator is Map) {
        final id = creator['id'];
        if (id is num) creatorId = id.toInt();
      } else if (c['creatorId'] is num) {
        creatorId = (c['creatorId'] as num).toInt();
      }
      if (me != null && creatorId != null && creatorId == me) return false;
      return true;
    }).toList();
  }

  Future<void> _openThread(Map<String, dynamic> c) async {
    final id = _conversationId(c);
    if (id == null) {
      AppTheme.showError(context, 'Invalid conversation');
      return;
    }
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ChatThreadPage(
          conversationId: id,
          title: _title(c),
          otherUserId: _otherUserId(c),
          isGroup: _isGroup(c),
          status: c['status']?.toString(),
          photoUrl: _photo(c),
          iAmCreator: () {
            final me = _myId();
            final creator = c['creator'];
            if (me == null) return false;
            if (creator is Map) {
              final cid = creator['id'];
              final id = cid is num ? cid.toInt() : int.tryParse('$cid');
              return id == me;
            }
            return false;
          }(),
        ),
      ),
    );
    if (mounted) _load(silent: true);
  }

  Future<void> _startUserChat(Map<String, dynamic> u) async {
    final id = u['id'];
    final uid = id is num ? id.toInt() : int.tryParse('$id');
    if (uid == null) return;
    final name = u['username']?.toString() ?? 'User';
    final photo = u['profilePhotoUrl']?.toString() ?? u['profilePicture']?.toString();

    // Prefer existing conversation
    for (final c in _conversations) {
      if (_otherUserId(c) == uid) {
        await _openThread(c);
        return;
      }
    }

    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ChatThreadPage(
          title: name,
          otherUserId: uid,
          photoUrl: photo,
        ),
      ),
    );
    if (mounted) _load();
  }

  @override
  Widget build(BuildContext context) {
    final requestCount = _requests.length;
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 8, 0),
            child: Row(
              children: [
                Expanded(
                  child: Text('Messages', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 22)),
                ),
                IconButton(
                  tooltip: 'New group',
                  onPressed: () async {
                    await Navigator.push(context, MaterialPageRoute(builder: (_) => const NewGroupPage()));
                    if (mounted) _load();
                  },
                  icon: const Icon(Icons.group_add_outlined),
                ),
                IconButton(
                  tooltip: 'New message',
                  onPressed: () async {
                    await Navigator.push(context, MaterialPageRoute(builder: (_) => const NewChatPage()));
                    if (mounted) _load();
                  },
                  icon: const Icon(Icons.edit_square),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
            child: TextField(
              controller: _searchCtrl,
              decoration: AppTheme.dashboardInput('Search…').copyWith(
                prefixIcon: const Icon(Icons.search_rounded, color: Colors.black45),
                suffixIcon: _searchCtrl.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _searchCtrl.clear();
                          setState(() => _searchUsers = []);
                        },
                      )
                    : null,
              ),
              onChanged: _onSearchChanged,
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFFBFDBFE)),
              ),
              child: TabBar(
                controller: _tabCtrl,
                labelColor: AppTheme.primary,
                unselectedLabelColor: AppTheme.textMuted,
                indicatorColor: AppTheme.primary,
                labelStyle: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 13),
                tabs: [
                  const Tab(text: 'All Chats'),
                  Tab(text: requestCount > 0 ? 'Requests ($requestCount)' : 'Requests'),
                ],
              ),
            ),
          ),
          if (_searchUsers.isNotEmpty || _searchingUsers)
            SizedBox(
              height: 72,
              child: _searchingUsers
                  ? const Center(child: SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2)))
                  : ListView.builder(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                      itemCount: _searchUsers.length,
                      itemBuilder: (_, i) {
                        final u = _searchUsers[i];
                        final name = u['username']?.toString() ?? 'User';
                        final photo = u['profilePhotoUrl']?.toString();
                        return Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: ActionChip(
                            avatar: CircleAvatar(
                              backgroundImage: photo != null && photo.isNotEmpty
                                  ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                                  : null,
                              child: photo == null || photo.isEmpty
                                  ? Text(name.isNotEmpty ? name[0].toUpperCase() : '?')
                                  : null,
                            ),
                            label: Text('Chat $name'),
                            onPressed: () => _startUserChat(u),
                          ),
                        );
                      },
                    ),
            ),
          Expanded(
            child: TabBarView(
              controller: _tabCtrl,
              children: [
                _listBody(_allChats, emptyLabel: 'No conversations yet'),
                _listBody(_requests, emptyLabel: 'No chat requests'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _listBody(List<Map<String, dynamic>> items, {required String emptyLabel}) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: AppTheme.primary));
    }
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
    if (items.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.3,
              child: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.chat_bubble_outline, size: 48, color: Colors.grey.shade400),
                    const SizedBox(height: 12),
                    Text(emptyLabel, style: GoogleFonts.inter(color: Colors.black45)),
                  ],
                ),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 100),
        itemCount: items.length,
        separatorBuilder: (_, _) => const SizedBox(height: 8),
        itemBuilder: (context, index) {
          final c = items[index];
          final title = _title(c);
          final unread = _unread(c);
          final photo = _photo(c);
          final time = _timeLabel(c);
          final group = _isGroup(c);
          return Material(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () => _openThread(c),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    Stack(
                      children: [
                        CircleAvatar(
                          radius: 26,
                          backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                          backgroundImage: photo != null && photo.isNotEmpty
                              ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                              : null,
                          child: photo == null || photo.isEmpty
                              ? (group
                                  ? const Icon(Icons.groups_rounded, color: AppTheme.primary, size: 26)
                                  : Text(
                                      title.isNotEmpty ? title[0].toUpperCase() : '?',
                                      style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary),
                                    ))
                              : null,
                        ),
                        if (unread)
                          Positioned(
                            right: 0,
                            top: 0,
                            child: Container(
                              width: 12,
                              height: 12,
                              decoration: BoxDecoration(
                                color: AppTheme.primary,
                                shape: BoxShape.circle,
                                border: Border.all(color: Colors.white, width: 2),
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  title,
                                  style: GoogleFonts.outfit(
                                    fontWeight: unread ? FontWeight.w800 : FontWeight.w700,
                                    fontSize: 16,
                                  ),
                                ),
                              ),
                              if (time.isNotEmpty)
                                Text(time, style: GoogleFonts.inter(fontSize: 11, color: AppTheme.textMuted)),
                            ],
                          ),
                          const SizedBox(height: 2),
                          Text(
                            _lastMessage(c).isEmpty
                                ? (group ? 'Group chat' : 'Tap to open chat')
                                : _lastMessage(c),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: GoogleFonts.inter(
                              color: unread ? Colors.black87 : Colors.black45,
                              fontSize: 13,
                              fontWeight: unread ? FontWeight.w600 : FontWeight.w400,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
