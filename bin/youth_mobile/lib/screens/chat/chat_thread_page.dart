import 'dart:async';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../config/api_config.dart';
import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../services/realtime_service.dart';
import '../../theme/app_theme.dart';
import '../../widgets/rich_composer.dart';

class ChatThreadPage extends StatefulWidget {
  const ChatThreadPage({
    super.key,
    this.conversationId,
    required this.title,
    this.otherUserId,
    this.isGroup = false,
    this.status,
    this.photoUrl,
    this.iAmCreator = false,
  });

  final int? conversationId;
  final String title;
  final int? otherUserId;
  final bool isGroup;
  final String? status;
  final String? photoUrl;
  final bool iAmCreator;

  @override
  State<ChatThreadPage> createState() => _ChatThreadPageState();
}

class _ChatThreadPageState extends State<ChatThreadPage> {
  final _messageCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  List<Map<String, dynamic>> _messages = [];
  int? _conversationId;
  bool _loading = false;
  bool _sending = false;
  String? _error;
  String? _status;
  bool _peerTyping = false;
  bool _lastMessageSeen = false;
  bool _iAmCreator = false;
  bool _vanishEnabled = false;
  String _theme = 'default';
  Timer? _typingDebounce;
  Timer? _typingHide;

  bool get _isPending => (_status ?? '').toUpperCase() == 'PENDING';
  bool get _showRequestActions => _isPending && !_iAmCreator;
  bool get _canCompose {
    if (_isPending) return false;
    if (widget.isGroup) return _conversationId != null;
    return widget.otherUserId != null;
  }

  @override
  void initState() {
    super.initState();
    _conversationId = widget.conversationId;
    _status = widget.status;
    _iAmCreator = widget.iAmCreator;
    if (_conversationId != null) {
      _load();
      _bindRealtime();
      _markSeen();
    }
  }

  @override
  void dispose() {
    _typingDebounce?.cancel();
    _typingHide?.cancel();
    _messageCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  Future<void> _markSeen() async {
    final id = _conversationId;
    if (id == null) return;
    try {
      await AppApi.markChatSeen(id);
    } catch (_) {}
  }

  Future<void> _bindRealtime() async {
    await RealtimeService.instance.subscribeJson('/user/queue/messages', (data) {
      final convId = _extractConvId(data);
      if (convId != null && convId == _conversationId && mounted) {
        _load(silent: true);
        _markSeen();
      }
    });
    await RealtimeService.instance.subscribeJson('/user/queue/seen', (data) {
      final convId = (data['conversationId'] as num?)?.toInt();
      if (convId != null && convId == _conversationId && mounted) {
        setState(() => _lastMessageSeen = true);
      }
    });
    await RealtimeService.instance.subscribeJson('/user/queue/typing', _onTyping);
    if (_conversationId != null) {
      await RealtimeService.instance.subscribeJson(
        '/topic/chat.$_conversationId.typing',
        _onTyping,
      );
    }
    await RealtimeService.instance.subscribeJson('/user/queue/conversation-update', (data) {
      final id = (data['id'] as num?)?.toInt();
      if (id != null && id == _conversationId) {
        setState(() {
          _status = data['status']?.toString() ?? _status;
          if (data['vanishModeEnabled'] != null) {
            _vanishEnabled = data['vanishModeEnabled'] == true;
          }
          if (data['theme'] != null) _theme = data['theme'].toString();
        });
      }
    });
    await RealtimeService.instance.subscribeJson('/user/queue/reaction', (_) {
      if (mounted) _load(silent: true);
    });
    await RealtimeService.instance.subscribeJson('/user/queue/vanish', (data) {
      final id = (data['conversationId'] as num?)?.toInt();
      if (id == _conversationId && mounted) {
        setState(() => _vanishEnabled = data['enabled'] == true);
      }
    });
    await RealtimeService.instance.subscribeJson('/user/queue/theme', (data) {
      final id = (data['conversationId'] as num?)?.toInt();
      if (id == _conversationId && mounted && data['theme'] != null) {
        setState(() => _theme = data['theme'].toString());
      }
    });
  }

  Future<void> _onChatMenu(String action) async {
    final id = _conversationId;
    final myId = context.read<AuthProvider>().user?.id;
    if (id == null || myId == null) return;
    try {
      if (action == 'vanish') {
        final next = !_vanishEnabled;
        await RealtimeService.instance.connect();
        RealtimeService.instance.send('/app/chat.vanish', {
          'conversationId': id,
          'enabled': next,
          'senderId': myId,
        });
        setState(() => _vanishEnabled = next);
        if (!next) await AppApi.cleanupVanish(id);
        if (mounted) AppTheme.showSuccess(context, next ? 'Vanish mode on' : 'Vanish mode off');
      } else if (action == 'theme') {
        const themes = ['default', 'sunset', 'ocean', 'forest', 'midnight'];
        final pick = await AppTheme.showBottomSheet<String>(
          context,
          (ctx) => SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: themes
                  .map((t) => ListTile(
                        title: Text(t),
                        trailing: _theme == t ? const Icon(Icons.check, color: AppTheme.primary) : null,
                        onTap: () => Navigator.pop(ctx, t),
                      ))
                  .toList(),
            ),
          ),
        );
        if (pick == null) return;
        await AppApi.updateChatTheme(id, pick);
        await RealtimeService.instance.connect();
        RealtimeService.instance.send('/app/chat.theme', {
          'conversationId': id,
          'theme': pick,
          'senderId': myId,
        });
        setState(() => _theme = pick);
        if (mounted) AppTheme.showSuccess(context, 'Theme updated');
      } else if (action == 'media') {
        final media = await AppApi.chatMedia(id);
        if (!mounted) return;
        await showModalBottomSheet<void>(
          context: context,
          isScrollControlled: true,
          backgroundColor: Colors.white,
          shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
          builder: (ctx) {
            final h = MediaQuery.sizeOf(ctx).height * 0.6;
            return SizedBox(
              height: h,
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text('Shared media', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
                  ),
                  Expanded(
                    child: media.isEmpty
                        ? Center(child: Text('No media yet', style: GoogleFonts.inter(color: AppTheme.textMuted)))
                        : GridView.builder(
                            padding: const EdgeInsets.all(12),
                            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 3,
                              mainAxisSpacing: 8,
                              crossAxisSpacing: 8,
                            ),
                            itemCount: media.length,
                            itemBuilder: (_, i) {
                              final url = media[i]['mediaUrl']?.toString() ?? '';
                              return ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: CachedNetworkImage(
                                  imageUrl: ApiConfig.mediaUrl(url),
                                  fit: BoxFit.cover,
                                  errorWidget: (_, __, ___) => const Icon(Icons.broken_image),
                                ),
                              );
                            },
                          ),
                  ),
                ],
              ),
            );
          },
        );
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _reactTo(Map<String, dynamic> m, String emoji) async {
    final mid = (m['id'] as num?)?.toInt();
    final myId = context.read<AuthProvider>().user?.id;
    if (mid == null || myId == null) return;
    try {
      await RealtimeService.instance.connect();
      RealtimeService.instance.send('/app/chat.react', {
        'messageId': mid,
        'reaction': emoji,
        'senderId': myId,
      });
      await Future<void>.delayed(const Duration(milliseconds: 200));
      await _load(silent: true);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _pinMessage(Map<String, dynamic> m) async {
    final mid = (m['id'] as num?)?.toInt();
    if (mid == null) return;
    try {
      await AppApi.togglePinMessage(mid);
      if (mounted) AppTheme.showSuccess(context, 'Pin toggled');
      await _load(silent: true);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  void _messageActions(Map<String, dynamic> m) {
    AppTheme.showBottomSheet<void>(
      context,
      (ctx) => SafeArea(
        child: Wrap(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
              child: Text('React', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: ['❤️', '😂', '👍', '🔥', '😮']
                    .map((e) => IconButton(
                          onPressed: () {
                            Navigator.pop(ctx);
                            _reactTo(m, e);
                          },
                          icon: Text(e, style: const TextStyle(fontSize: 26)),
                        ))
                    .toList(),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.push_pin_outlined),
              title: const Text('Pin / unpin'),
              onTap: () {
                Navigator.pop(ctx);
                _pinMessage(m);
              },
            ),
          ],
        ),
      ),
    );
  }

  void _onTyping(Map<String, dynamic> data) {
    final myId = context.read<AuthProvider>().user?.id;
    final senderId = (data['senderId'] as num?)?.toInt();
    if (senderId != null && myId != null && senderId == myId) return;
    final convId = (data['conversationId'] as num?)?.toInt();
    if (convId != null && _conversationId != null && convId != _conversationId) return;
    final typing = data['isTyping'] == true || data['typing'] == true;
    if (!mounted) return;
    setState(() => _peerTyping = typing);
    _typingHide?.cancel();
    if (typing) {
      _typingHide = Timer(const Duration(seconds: 3), () {
        if (mounted) setState(() => _peerTyping = false);
      });
    }
  }

  int? _extractConvId(Map<String, dynamic> data) {
    final conv = data['conversation'];
    if (conv is Map) {
      final id = conv['id'];
      if (id is num) return id.toInt();
      if (id is String) return int.tryParse(id);
    }
    final id = data['conversationId'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    return null;
  }

  Future<void> _load({bool silent = false}) async {
    final id = _conversationId;
    if (id == null) return;
    if (!silent) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final list = await AppApi.chatHistory(id);
      if (!mounted) return;
      setState(() {
        _messages = list;
        _loading = false;
        _lastMessageSeen = _computeSeen(list);
      });
      // Infer creator from first message / status only if needed
      final me = context.read<AuthProvider>().user?.id;
      if (me != null && _isPending && list.isNotEmpty) {
        final firstSender = _senderId(list.first);
        if (firstSender != null) {
          setState(() => _iAmCreator = firstSender == me);
        }
      }
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollCtrl.hasClients) {
          _scrollCtrl.jumpTo(_scrollCtrl.position.maxScrollExtent);
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppTheme.extractError(e);
        _loading = false;
      });
    }
  }

  bool _computeSeen(List<Map<String, dynamic>> list) {
    if (list.isEmpty) return false;
    final last = list.last;
    return last['seenAt'] != null || last['status']?.toString().toUpperCase() == 'SEEN';
  }

  void _emitTyping(bool isTyping) {
    final myId = context.read<AuthProvider>().user?.id;
    if (myId == null) return;
    final payload = <String, dynamic>{
      'senderId': myId,
      'isTyping': isTyping,
    };
    if (_conversationId != null) {
      payload['conversationId'] = _conversationId;
    } else if (widget.otherUserId != null) {
      payload['recipientId'] = widget.otherUserId;
    }
    RealtimeService.instance.send('/app/chat.typing', payload);
  }

  void _onTextChanged(String text) {
    _typingDebounce?.cancel();
    if (text.trim().isEmpty) {
      _emitTyping(false);
      return;
    }
    _emitTyping(true);
    _typingDebounce = Timer(const Duration(milliseconds: 1200), () => _emitTyping(false));
  }

  Future<void> _send({String? mediaUrl, String? contentOverride}) async {
    if (_sending) return;
    var text = (contentOverride ?? _messageCtrl.text).trim();
    var media = (mediaUrl != null && mediaUrl.isNotEmpty) ? mediaUrl : null;

    // GIF / remote media pasted or picked as text → send as mediaUrl
    if (media == null && isMediaUrl(text)) {
      media = text;
      text = '';
    }

    if (text.isEmpty && media == null) return;
    if (!_canCompose && widget.otherUserId == null && !widget.isGroup) return;

    setState(() => _sending = true);
    _emitTyping(false);
    try {
      final myId = context.read<AuthProvider>().user?.id;
      if (widget.isGroup && _conversationId != null && myId != null) {
        await RealtimeService.instance.connect();
        RealtimeService.instance.send('/app/chat.send', {
          'senderId': myId,
          'recipientId': _conversationId,
          'content': text,
          if (media != null) 'mediaUrl': media,
          'isGroup': true,
        });
        _messageCtrl.clear();
        await Future<void>.delayed(const Duration(milliseconds: 250));
        await _load(silent: true);
      } else if (widget.otherUserId != null) {
        final res = await AppApi.sendDirect(
          receiverId: widget.otherUserId!,
          content: text,
          mediaUrl: media,
        );
        _messageCtrl.clear();
        int? newId;
        final conv = res['conversation'];
        if (conv is Map) {
          final id = conv['id'];
          if (id is num) newId = id.toInt();
        }
        if (_conversationId == null) {
          if (newId != null) {
            _conversationId = newId;
            await _bindRealtime();
          } else {
            final convs = await AppApi.conversations();
            for (final c in convs) {
              final parts = c['participants'];
              if (parts is! List) continue;
              final match = parts.any((p) {
                if (p is! Map) return false;
                final pid = p['id'];
                final id = pid is num ? pid.toInt() : int.tryParse('$pid');
                return id == widget.otherUserId;
              });
              if (match) {
                final cid = c['id'];
                if (cid is num) {
                  _conversationId = cid.toInt();
                  _status = c['status']?.toString();
                  await _bindRealtime();
                }
                break;
              }
            }
          }
        }
        await _load(silent: true);
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _sendMediaUrl(String url) async {
    await _send(mediaUrl: url, contentOverride: '');
  }

  Future<void> _pickImage() async {
    if (_sending) return;
    try {
      final picker = ImagePicker();
      final file = await picker.pickImage(source: ImageSource.gallery, imageQuality: 85);
      if (file == null) return;
      setState(() => _sending = true);
      final url = await AppApi.uploadChatMedia(file.path, filename: file.name);
      // Keep relative /uploads/... path — ApiConfig.mediaUrl resolves it when displaying
      setState(() => _sending = false);
      await _send(mediaUrl: url, contentOverride: '');
    } catch (e) {
      if (mounted) {
        setState(() => _sending = false);
        AppTheme.showError(context, e);
      }
    }
  }

  Future<void> _accept() async {
    final id = _conversationId;
    if (id == null) return;
    try {
      await AppApi.acceptChatRequest(id);
      if (!mounted) return;
      setState(() => _status = 'ACCEPTED');
      AppTheme.showSuccess(context, 'Request accepted');
      await _load(silent: true);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _reject() async {
    final id = _conversationId;
    if (id == null) return;
    try {
      await AppApi.rejectChatRequest(id);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Request declined');
      Navigator.pop(context);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  String _messageText(Map<String, dynamic> m) =>
      m['content']?.toString() ?? m['text']?.toString() ?? m['message']?.toString() ?? '';

  String? _mediaUrl(Map<String, dynamic> m) {
    final u = m['mediaUrl']?.toString();
    if (u == null || u.isEmpty) return null;
    return u;
  }

  String _senderName(Map<String, dynamic> m) {
    if (m['sender'] is Map) {
      final s = m['sender'] as Map;
      return s['username']?.toString() ?? 'User';
    }
    return m['senderUsername']?.toString() ?? m['username']?.toString() ?? 'User';
  }

  int? _senderId(Map<String, dynamic> m) {
    final id = m['senderId'] ?? m['userId'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    if (m['sender'] is Map) {
      final sid = (m['sender'] as Map)['id'];
      if (sid is num) return sid.toInt();
      if (sid is String) return int.tryParse(sid);
    }
    return null;
  }

  bool _isMine(Map<String, dynamic> m, String? myUsername, int? myId) {
    final sid = _senderId(m);
    if (myId != null && sid != null && sid == myId) return true;
    final name = _senderName(m);
    if (myUsername != null && myUsername.isNotEmpty && name == myUsername) return true;
    return false;
  }

  String _msgTime(Map<String, dynamic> m) {
    final raw = m['timestamp']?.toString() ?? m['createdAt']?.toString() ?? m['sentAt']?.toString();
    if (raw == null) return '';
    final dt = DateTime.tryParse(raw);
    if (dt == null) return '';
    return DateFormat.jm().format(dt.toLocal());
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final myUsername = auth.user?.username;
    final myId = auth.user?.id;
    final photo = widget.photoUrl;

    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        backgroundColor: Colors.white,
        foregroundColor: AppTheme.textPrimary,
        elevation: 0,
        title: Row(
          children: [
            CircleAvatar(
              radius: 16,
              backgroundColor: AppTheme.secondary.withValues(alpha: 0.4),
              backgroundImage: photo != null && photo.isNotEmpty
                  ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                  : null,
              child: photo == null || photo.isEmpty
                  ? (widget.isGroup
                      ? const Icon(Icons.groups, size: 16)
                      : Text(widget.title.isNotEmpty ? widget.title[0].toUpperCase() : '?'))
                  : null,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(widget.title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
                  Text(
                    _peerTyping
                        ? 'typing…'
                        : (_vanishEnabled
                            ? 'Vanish on'
                            : (widget.isGroup ? 'Group' : 'Direct message')),
                    style: GoogleFonts.inter(fontSize: 11, color: AppTheme.textMuted),
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          if (_conversationId != null)
            PopupMenuButton<String>(
              onSelected: _onChatMenu,
              itemBuilder: (_) => [
                PopupMenuItem(
                  value: 'vanish',
                  child: Text(_vanishEnabled ? 'Turn vanish off' : 'Turn vanish on'),
                ),
                const PopupMenuItem(value: 'theme', child: Text('Chat theme')),
                const PopupMenuItem(value: 'media', child: Text('Media gallery')),
              ],
            ),
        ],
      ),
      body: Column(
        children: [
          if (_showRequestActions)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              color: const Color(0xFFFFF7ED),
              child: Column(
                children: [
                  Text(
                    'Message request',
                    style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: _reject,
                          child: const Text('Reject'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: _accept,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppTheme.primary,
                            foregroundColor: Colors.white,
                          ),
                          child: const Text('Accept'),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            )
          else if (_isPending)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(10),
              color: const Color(0xFFEFF6FF),
              child: Text(
                'Request sent — waiting for them to accept',
                textAlign: TextAlign.center,
                style: GoogleFonts.inter(fontSize: 12, color: AppTheme.primary),
              ),
            ),
          Expanded(
            child: _conversationId == null && _messages.isEmpty
                ? Center(
                    child: Text(
                      'Say hello to start the conversation',
                      style: GoogleFonts.inter(color: AppTheme.textMuted),
                    ),
                  )
                : _loading
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
                        : ListView.builder(
                            controller: _scrollCtrl,
                            padding: const EdgeInsets.all(12),
                            itemCount: _messages.length + (_peerTyping ? 1 : 0),
                            itemBuilder: (context, index) {
                              if (_peerTyping && index == _messages.length) {
                                return Align(
                                  alignment: Alignment.centerLeft,
                                  child: Container(
                                    margin: const EdgeInsets.only(bottom: 8),
                                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                                    decoration: BoxDecoration(
                                      color: Colors.white,
                                      borderRadius: BorderRadius.circular(16),
                                    ),
                                    child: Text('typing…', style: GoogleFonts.inter(color: AppTheme.textMuted, fontSize: 13)),
                                  ),
                                );
                              }
                              final m = _messages[index];
                              final mine = _isMine(m, myUsername, myId);
                              final media = _mediaUrl(m);
                              final rawText = _messageText(m);
                              final text = rawText.trim();
                              // Avoid duplicating GIF when stored as both mediaUrl and content
                              final showText = text.isNotEmpty &&
                                  !(media != null && (text == media || isMediaUrl(text)));
                              final time = _msgTime(m);
                              final isLastMine = mine && index == _messages.length - 1;
                              final pinned = m['pinned'] == true || m['isPinned'] == true;
                              final reactions = m['reactions'];
                              return Align(
                                alignment: mine ? Alignment.centerRight : Alignment.centerLeft,
                                child: GestureDetector(
                                  onLongPress: () => _messageActions(m),
                                  child: Container(
                                  constraints: BoxConstraints(
                                    maxWidth: MediaQuery.of(context).size.width * 0.78,
                                  ),
                                  margin: const EdgeInsets.only(bottom: 8),
                                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                                  decoration: BoxDecoration(
                                    color: mine ? AppTheme.primary : Colors.white,
                                    borderRadius: BorderRadius.only(
                                      topLeft: const Radius.circular(16),
                                      topRight: const Radius.circular(16),
                                      bottomLeft: Radius.circular(mine ? 16 : 4),
                                      bottomRight: Radius.circular(mine ? 4 : 16),
                                    ),
                                    border: pinned
                                        ? Border.all(color: Colors.amber.shade600, width: 1.5)
                                        : null,
                                  ),
                                  child: Column(
                                    crossAxisAlignment:
                                        mine ? CrossAxisAlignment.end : CrossAxisAlignment.start,
                                    children: [
                                      if (pinned)
                                        Padding(
                                          padding: const EdgeInsets.only(bottom: 4),
                                          child: Icon(Icons.push_pin, size: 14, color: mine ? Colors.white70 : Colors.amber.shade800),
                                        ),
                                      if (!mine && widget.isGroup)
                                        Text(
                                          _senderName(m),
                                          style: GoogleFonts.outfit(
                                            fontWeight: FontWeight.bold,
                                            fontSize: 12,
                                            color: Colors.black54,
                                          ),
                                        ),
                                      if (!mine && widget.isGroup) const SizedBox(height: 4),
                                      if (media != null) ...[
                                        ClipRRect(
                                          borderRadius: BorderRadius.circular(10),
                                          child: CachedNetworkImage(
                                            imageUrl: ApiConfig.mediaUrl(media),
                                            height: 180,
                                            fit: BoxFit.contain,
                                            errorWidget: (_, __, ___) => Icon(
                                              Icons.broken_image,
                                              color: mine ? Colors.white70 : Colors.black38,
                                            ),
                                          ),
                                        ),
                                        if (showText) const SizedBox(height: 6),
                                      ],
                                      if (showText)
                                        CommentContentView(
                                          content: text,
                                          textStyle: GoogleFonts.inter(
                                            color: mine ? Colors.white : Colors.black87,
                                          ),
                                          maxGifHeight: 160,
                                        ),
                                      if (reactions is Map && reactions.isNotEmpty)
                                        Padding(
                                          padding: const EdgeInsets.only(top: 4),
                                          child: Text(
                                            reactions.values.map((e) => e.toString()).join(' '),
                                            style: const TextStyle(fontSize: 14),
                                          ),
                                        ),
                                      if (time.isNotEmpty) ...[
                                        const SizedBox(height: 4),
                                        Text(
                                          time,
                                          style: GoogleFonts.inter(
                                            fontSize: 10,
                                            color: mine ? Colors.white70 : AppTheme.textMuted,
                                          ),
                                        ),
                                      ],
                                      if (isLastMine && _lastMessageSeen)
                                        Text(
                                          'Seen',
                                          style: GoogleFonts.inter(fontSize: 10, color: Colors.white70),
                                        ),
                                    ],
                                  ),
                                ),
                                ),
                              );
                            },
                          ),
          ),
          if (_canCompose || (widget.otherUserId != null && !_isPending))
            SafeArea(
              top: false,
              child: Material(
                color: Colors.white,
                elevation: 4,
                child: RichComposer(
                  controller: _messageCtrl,
                  sending: _sending,
                  enabled: !_sending,
                  hint: 'Message…',
                  onSend: () => _send(),
                  onSendMedia: _sendMediaUrl,
                  onAttach: _pickImage,
                  onTextChanged: _onTextChanged,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
