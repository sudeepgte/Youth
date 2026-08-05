import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:video_player/video_player.dart';

import '../../config/api_config.dart';
import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/comment_sheet.dart';
import '../profile/create_post_page.dart';

class ReelsPage extends StatefulWidget {
  const ReelsPage({super.key});

  @override
  State<ReelsPage> createState() => _ReelsPageState();
}

class _ReelsPageState extends State<ReelsPage> {
  final PageController _pageCtrl = PageController();
  List<Map<String, dynamic>> _reels = [];
  bool _loading = true;
  String? _error;
  int _currentPage = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _pageCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final userId = context.read<AuthProvider>().user?.id;
    if (userId == null) {
      setState(() {
        _error = 'Not logged in';
        _loading = false;
      });
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final reels = await AppApi.reels(userId: userId);
      if (!mounted) return;
      setState(() {
        _reels = reels;
        _loading = false;
        _currentPage = 0;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppTheme.extractError(e);
        _loading = false;
      });
    }
  }

  Future<void> _likeReel(int index) async {
    final userId = context.read<AuthProvider>().user?.id;
    if (userId == null) return;
    final reel = _reels[index];
    final reelId = (reel['id'] as num?)?.toInt();
    if (reelId == null) return;
    try {
      await AppApi.reelInteract(reelId: reelId, userId: userId, action: 'LIKE');
      if (!mounted) return;
      setState(() {
        final updated = Map<String, dynamic>.from(_reels[index]);
        updated['likeCount'] = ((updated['likeCount'] as num?)?.toInt() ?? 0) + 1;
        updated['liked'] = true;
        _reels[index] = updated;
      });
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _saveReel(int index) async {
    final reel = _reels[index];
    final reelId = (reel['id'] as num?)?.toInt() ?? (reel['postId'] as num?)?.toInt();
    if (reelId == null) return;
    try {
      await AppApi.savePost(reelId);
      if (!mounted) return;
      setState(() {
        final updated = Map<String, dynamic>.from(_reels[index]);
        updated['saved'] = true;
        _reels[index] = updated;
      });
      AppTheme.showSuccess(context, 'Saved');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _commentReel(int index) async {
    final reel = _reels[index];
    final reelId = (reel['id'] as num?)?.toInt() ?? (reel['postId'] as num?)?.toInt();
    if (reelId == null) return;
    await showCommentSheet(
      context: context,
      postId: reelId,
      commentsDisabled: false,
      initialCount: (reel['commentCount'] as num?)?.toInt() ?? 0,
    );
  }

  Future<void> _shareReel(int index) async {
    final reel = _reels[index];
    final reelId = (reel['id'] as num?)?.toInt() ?? (reel['postId'] as num?)?.toInt();
    if (reelId == null) return;
    try {
      final users = await AppApi.chatUsers();
      if (!mounted) return;
      final pick = await showModalBottomSheet<Map<String, dynamic>>(
        context: context,
        backgroundColor: Colors.white,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
        builder: (ctx) => SafeArea(
          child: ListView(
            shrinkWrap: true,
            children: [
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text('Share reel to…', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
              ),
              ...users.map((u) {
                final id = (u['id'] as num?)?.toInt();
                final name = u['username']?.toString() ?? 'User';
                return ListTile(
                  leading: CircleAvatar(child: Text(name.isNotEmpty ? name[0].toUpperCase() : '?')),
                  title: Text(name),
                  onTap: id == null ? null : () => Navigator.pop(ctx, {'id': id, 'name': name}),
                );
              }),
            ],
          ),
        ),
      );
      if (pick == null || !mounted) return;
      await AppApi.sharePostToUser(postId: reelId, recipientId: pick['id'] as int);
      if (mounted) AppTheme.showSuccess(context, 'Shared to ${pick['name']}');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  void _trackView(int index) {
    final userId = context.read<AuthProvider>().user?.id;
    if (userId == null) return;
    final reel = _reels[index];
    final reelId = (reel['id'] as num?)?.toInt();
    if (reelId == null || reel['viewTracked'] == true) return;
    AppApi.reelInteract(reelId: reelId, userId: userId, action: 'VIEW').then((_) {
      if (!mounted) return;
      setState(() {
        final updated = Map<String, dynamic>.from(_reels[index]);
        updated['viewTracked'] = true;
        _reels[index] = updated;
      });
    }).catchError((_) {});
  }

  bool _isVideoUrl(String? url) {
    if (url == null || url.isEmpty) return false;
    final lower = url.toLowerCase();
    return lower.contains('.mp4') ||
        lower.contains('.mov') ||
        lower.contains('.webm') ||
        lower.contains('.m3u8') ||
        lower.contains('/video');
  }

  Future<void> _openUpload() async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const CreatePostPage(initialType: 'REEL')),
    );
    if (mounted) _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      extendBodyBehindAppBar: true,
      drawer: const AppDrawer(active: AppDrawerItem.reels),
      appBar: AppBar(
        title: Text('Reels', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.white)),
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        elevation: 0,
        leading: Builder(
          builder: (ctx) => IconButton(
            icon: const Icon(Icons.menu, color: Colors.white),
            onPressed: () => Scaffold.of(ctx).openDrawer(),
          ),
        ),
        actions: [
          IconButton(
            tooltip: 'Back',
            onPressed: () => Navigator.maybePop(context),
            icon: const Icon(Icons.arrow_back_rounded),
          ),
          IconButton(
            tooltip: 'Upload reel',
            onPressed: _openUpload,
            icon: const Icon(Icons.add_circle_outline),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _openUpload,
        backgroundColor: AppTheme.primary,
        child: const Icon(Icons.videocam, color: Colors.white),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: Colors.white))
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
              : _reels.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text('No reels yet', style: GoogleFonts.inter(color: Colors.white54)),
                          const SizedBox(height: 12),
                          TextButton(
                            onPressed: _openUpload,
                            child: const Text('Upload a reel', style: TextStyle(color: Colors.white)),
                          ),
                        ],
                      ),
                    )
                  : RefreshIndicator(
                      onRefresh: _load,
                      color: Colors.blueAccent,
                      child: PageView.builder(
                        controller: _pageCtrl,
                        scrollDirection: Axis.vertical,
                        itemCount: _reels.length,
                        onPageChanged: (i) {
                          setState(() => _currentPage = i);
                          _trackView(i);
                        },
                        itemBuilder: (context, index) {
                          final reel = _reels[index];
                          final mediaUrl = reel['mediaUrl']?.toString() ??
                              reel['videoUrl']?.toString() ??
                              reel['url']?.toString();
                          final content = reel['content']?.toString() ??
                              reel['caption']?.toString() ??
                              '';
                          final username = reel['username']?.toString() ??
                              (reel['user'] is Map
                                  ? (reel['user'] as Map)['username']?.toString()
                                  : null) ??
                              '';
                          final resolved = mediaUrl != null && mediaUrl.isNotEmpty
                              ? ApiConfig.mediaUrl(mediaUrl)
                              : null;
                          final liked = reel['liked'] == true;
                          final saved = reel['saved'] == true;
                          final likeCount = (reel['likeCount'] as num?)?.toInt() ?? 0;

                          return Stack(
                            fit: StackFit.expand,
                            children: [
                              if (resolved != null && _isVideoUrl(mediaUrl))
                                _ReelVideo(
                                  url: resolved,
                                  active: index == _currentPage,
                                )
                              else if (resolved != null)
                                CachedNetworkImage(
                                  imageUrl: resolved,
                                  fit: BoxFit.cover,
                                  errorWidget: (_, __, ___) => const Center(
                                    child: Icon(Icons.movie, color: Colors.white54, size: 64),
                                  ),
                                )
                              else
                                const Center(
                                  child: Icon(Icons.movie, color: Colors.white54, size: 64),
                                ),
                              const DecoratedBox(
                                decoration: BoxDecoration(
                                  gradient: LinearGradient(
                                    begin: Alignment.topCenter,
                                    end: Alignment.bottomCenter,
                                    colors: [
                                      Colors.black26,
                                      Colors.transparent,
                                      Colors.black87,
                                    ],
                                    stops: [0, 0.45, 1],
                                  ),
                                ),
                              ),
                              Positioned(
                                left: 16,
                                bottom: 32,
                                right: 80,
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    if (username.isNotEmpty)
                                      Text(
                                        '@$username',
                                        style: GoogleFonts.outfit(
                                          color: Colors.white,
                                          fontWeight: FontWeight.bold,
                                          fontSize: 16,
                                        ),
                                      ),
                                    if (content.isNotEmpty) ...[
                                      const SizedBox(height: 8),
                                      Text(
                                        content,
                                        maxLines: 4,
                                        overflow: TextOverflow.ellipsis,
                                        style: GoogleFonts.inter(color: Colors.white, height: 1.35),
                                      ),
                                    ],
                                  ],
                                ),
                              ),
                              Positioned(
                                right: 12,
                                bottom: 48,
                                child: Column(
                                  children: [
                                    IconButton(
                                      onPressed: () => _likeReel(index),
                                      icon: Icon(
                                        liked ? Icons.favorite : Icons.favorite_border,
                                        color: liked ? Colors.redAccent : Colors.white,
                                        size: 32,
                                      ),
                                    ),
                                    Text(
                                      '$likeCount',
                                      style: GoogleFonts.inter(color: Colors.white, fontSize: 12),
                                    ),
                                    const SizedBox(height: 12),
                                    IconButton(
                                      onPressed: () => _commentReel(index),
                                      icon: const Icon(Icons.chat_bubble_outline, color: Colors.white, size: 28),
                                    ),
                                    const SizedBox(height: 12),
                                    IconButton(
                                      onPressed: () => _saveReel(index),
                                      icon: Icon(
                                        saved ? Icons.bookmark : Icons.bookmark_border,
                                        color: Colors.white,
                                        size: 28,
                                      ),
                                    ),
                                    const SizedBox(height: 12),
                                    IconButton(
                                      onPressed: () => _shareReel(index),
                                      icon: const Icon(Icons.send_outlined, color: Colors.white, size: 28),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          );
                        },
                      ),
                    ),
    );
  }
}

class _ReelVideo extends StatefulWidget {
  const _ReelVideo({required this.url, required this.active});

  final String url;
  final bool active;

  @override
  State<_ReelVideo> createState() => _ReelVideoState();
}

class _ReelVideoState extends State<_ReelVideo> {
  VideoPlayerController? _ctrl;
  bool _ready = false;
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    final ctrl = VideoPlayerController.networkUrl(Uri.parse(widget.url));
    _ctrl = ctrl;
    try {
      await ctrl.initialize();
      await ctrl.setLooping(true);
      if (!mounted) return;
      setState(() => _ready = true);
      if (widget.active) ctrl.play();
    } catch (_) {
      if (mounted) setState(() => _failed = true);
    }
  }

  @override
  void didUpdateWidget(covariant _ReelVideo oldWidget) {
    super.didUpdateWidget(oldWidget);
    final ctrl = _ctrl;
    if (ctrl == null || !_ready) return;
    if (widget.active && !oldWidget.active) {
      ctrl.play();
    } else if (!widget.active && oldWidget.active) {
      ctrl.pause();
    }
  }

  @override
  void dispose() {
    _ctrl?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_failed) {
      return CachedNetworkImage(
        imageUrl: widget.url,
        fit: BoxFit.cover,
        errorWidget: (_, __, ___) => const Center(
          child: Icon(Icons.broken_image, color: Colors.white54, size: 64),
        ),
      );
    }
    if (!_ready || _ctrl == null) {
      return const Center(child: CircularProgressIndicator(color: Colors.white));
    }
    return FittedBox(
      fit: BoxFit.cover,
      child: SizedBox(
        width: _ctrl!.value.size.width,
        height: _ctrl!.value.size.height,
        child: VideoPlayer(_ctrl!),
      ),
    );
  }
}
