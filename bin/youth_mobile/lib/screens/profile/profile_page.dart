import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../config/api_config.dart';
import '../../models/post_model.dart';
import '../../models/user_model.dart';
import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../chat/chat_list_page.dart';
import '../chat/chat_thread_page.dart';
import 'edit_profile_page.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key, this.username});

  final String? username;

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> with SingleTickerProviderStateMixin {
  AppUser? _profile;
  bool _loading = true;
  String? _error;
  bool _followLoading = false;
  List<Map<String, dynamic>> _badges = [];
  List<Map<String, dynamic>> _explorePeople = [];
  TabController? _tabs;

  String get _targetUsername =>
      widget.username ?? context.read<AuthProvider>().user?.username ?? '';

  bool get _isOwn =>
      _profile?.isOwnProfile == true ||
      _profile?.username == context.read<AuthProvider>().user?.username;

  bool get _embeddedInShell => widget.username == null;

  int get _tabCount => _isOwn ? 5 : 4;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _tabs?.dispose();
    super.dispose();
  }

  void _ensureTabs() {
    final count = _tabCount;
    if (_tabs == null || _tabs!.length != count) {
      final oldIndex = _tabs?.index ?? 0;
      _tabs?.dispose();
      _tabs = TabController(length: count, vsync: this, initialIndex: oldIndex.clamp(0, count - 1));
      _tabs!.addListener(() {
        if (mounted) setState(() {});
      });
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final profile = await AppApi.profile(_targetUsername);
      List<Map<String, dynamic>> badges = [];
      List<Map<String, dynamic>> explore = [];
      try {
        final ach = await AppApi.achievements();
        badges = (ach['badges'] as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList();
      } catch (_) {}
      try {
        explore = await AppApi.exploreUsers();
      } catch (_) {}
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _badges = badges;
        _explorePeople = explore;
        _loading = false;
      });
      _ensureTabs();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppTheme.extractError(e);
        _loading = false;
      });
    }
  }

  Future<void> _toggleFollow() async {
    if (_profile == null) return;
    setState(() => _followLoading = true);
    try {
      if (_profile!.isFollowing == true) {
        await AppApi.unfollow(_profile!.id);
      } else {
        await AppApi.follow(_profile!.id);
      }
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _followLoading = false);
    }
  }

  Future<void> _messageUser() async {
    if (_profile == null) return;
    final ctrl = TextEditingController();
    final sent = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Message @${_profile!.username}', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        content: TextField(
          controller: ctrl,
          decoration: AppTheme.dashboardInput('Say hello...'),
          maxLines: 3,
          autofocus: true,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
            child: const Text('Send'),
          ),
        ],
      ),
    );
    final text = ctrl.text.trim();
    ctrl.dispose();
    if (sent != true || text.isEmpty || !mounted) return;

    try {
      final res = await AppApi.sendDirect(receiverId: _profile!.id, content: text);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Message sent');

      int? convId;
      final conv = res['conversation'];
      if (conv is Map) {
        final id = conv['id'];
        if (id is num) convId = id.toInt();
      }
      convId ??= (res['conversationId'] as num?)?.toInt();

      if (convId != null) {
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ChatThreadPage(
              conversationId: convId!,
              title: _profile!.username,
              otherUserId: _profile!.id,
            ),
          ),
        );
      } else {
        await Navigator.push(context, MaterialPageRoute(builder: (_) => const ChatListPage()));
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  List<PostModel> get _allPosts {
    final raw = _profile?.posts;
    if (raw == null) return [];
    return raw
        .whereType<Map>()
        .map((e) => PostModel.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  List<PostModel> get _posts =>
      _allPosts.where((p) => (p.postType ?? 'POST').toUpperCase() != 'REEL').toList();

  List<PostModel> get _reels =>
      _allPosts.where((p) => (p.postType ?? '').toUpperCase() == 'REEL').toList();

  List<PostModel> get _savedPosts => _allPosts.where((p) => p.saved).toList();

  Future<void> _editPost(PostModel post) async {
    final ctrl = TextEditingController(text: post.content ?? '');
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Edit post'),
        content: TextField(
          controller: ctrl,
          maxLines: 4,
          decoration: AppTheme.dashboardInput('Caption'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Save')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    try {
      await AppApi.editPost(post.id, content: ctrl.text.trim(), hashtags: post.hashtags);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Post updated');
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _deletePost(PostModel post) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete post?'),
        content: const Text('This cannot be undone.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Delete', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    try {
      await AppApi.deletePost(post.id);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Post deleted');
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  int get _xpToNext {
    final xp = _profile?.xp ?? 0;
    return ((xp ~/ 100) + 1) * 100;
  }

  double get _xpProgress {
    final xp = _profile?.xp ?? 0;
    final next = _xpToNext;
    final base = next - 100;
    if (next <= base) return 0;
    return ((xp - base) / 100).clamp(0.0, 1.0);
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const ColoredBox(
        color: AppTheme.dashboardBg,
        child: Center(child: CircularProgressIndicator(color: AppTheme.primary)),
      );
    }
    if (_error != null) {
      return ColoredBox(
        color: AppTheme.dashboardBg,
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(_error!, style: const TextStyle(color: Colors.redAccent)),
              const SizedBox(height: 12),
              ElevatedButton(onPressed: _load, child: const Text('Retry')),
            ],
          ),
        ),
      );
    }
    if (_profile == null) {
      return ColoredBox(
        color: AppTheme.dashboardBg,
        child: Center(child: Text('Profile not found', style: GoogleFonts.inter(color: Colors.black45))),
      );
    }

    final p = _profile!;
    if (_tabs == null) _ensureTabs();
    final body = RefreshIndicator(
      color: AppTheme.primary,
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
        children: [
          _header(p),
          const SizedBox(height: 18),
          _statsRow(p),
          const SizedBox(height: 14),
          if (p.collegeName != null && p.collegeName!.isNotEmpty) ...[
            _collegeCard(p.collegeName!),
            const SizedBox(height: 14),
          ],
          _primaryActions(p),
          const SizedBox(height: 18),
          _tabBar(),
          const SizedBox(height: 14),
          _tabBody(p),
        ],
      ),
    );

    if (_embeddedInShell) {
      return ColoredBox(color: AppTheme.dashboardBg, child: body);
    }

    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        title: Image.asset('assets/images/youthian_logo.png', height: 32),
      ),
      body: body,
    );
  }

  Widget _header(AppUser p) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Stack(
          children: [
            CircleAvatar(
              radius: 42,
              backgroundColor: AppTheme.primary,
              backgroundImage: p.profilePhotoUrl != null
                  ? CachedNetworkImageProvider(ApiConfig.mediaUrl(p.profilePhotoUrl))
                  : null,
              child: p.profilePhotoUrl == null
                  ? Text(
                      p.username.isNotEmpty ? p.username[0].toUpperCase() : '?',
                      style: GoogleFonts.outfit(fontSize: 34, fontWeight: FontWeight.w800, color: Colors.white),
                    )
                  : null,
            ),
            Positioned(
              right: 2,
              bottom: 2,
              child: Container(
                width: 14,
                height: 14,
                decoration: BoxDecoration(
                  color: const Color(0xFF22C55E),
                  shape: BoxShape.circle,
                  border: Border.all(color: AppTheme.dashboardBg, width: 2),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Flexible(
                    child: Text(
                      p.username,
                      style: GoogleFonts.outfit(
                        color: Colors.black87,
                        fontWeight: FontWeight.w800,
                        fontSize: 26,
                      ),
                    ),
                  ),
                  if (p.isPremium) ...[
                    const SizedBox(width: 6),
                    const Icon(Icons.workspace_premium, color: AppTheme.gold, size: 20),
                  ],
                  if (p.privateAccount) ...[
                    const SizedBox(width: 6),
                    const Icon(Icons.lock_outline, color: Colors.black45, size: 18),
                  ],
                ],
              ),
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF8E1),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: AppTheme.gold.withValues(alpha: 0.6)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.bar_chart_rounded, color: Color(0xFFB45309), size: 14),
                    const SizedBox(width: 4),
                    Text(
                      'Level ${p.level}',
                      style: GoogleFonts.inter(
                        color: const Color(0xFFB45309),
                        fontWeight: FontWeight.w600,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 10),
              Text(
                '${p.xp} XP',
                style: GoogleFonts.outfit(color: Colors.black87, fontWeight: FontWeight.w800, fontSize: 22),
              ),
              const SizedBox(height: 6),
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: _xpProgress,
                  minHeight: 5,
                  backgroundColor: Colors.black12,
                  color: AppTheme.gold,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                '${p.xp} / $_xpToNext XP to next level',
                style: GoogleFonts.inter(color: Colors.black45, fontSize: 11),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _statsRow(AppUser p) {
    return Row(
      children: [
        Expanded(child: _statChip(Icons.people_alt_outlined, 'Followers', '${p.followersCount}')),
        const SizedBox(width: 8),
        Expanded(child: _statChip(Icons.person_add_alt_1_outlined, 'Following', '${p.followingCount}')),
        const SizedBox(width: 8),
        Expanded(child: _statChip(Icons.monetization_on_outlined, 'Coins', '${p.coins}')),
      ],
    );
  }

  Widget _statChip(IconData icon, String label, String value) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        children: [
          Icon(icon, color: const Color(0xFFB45309), size: 18),
          const SizedBox(height: 4),
          Text(value, style: GoogleFonts.outfit(color: Colors.black87, fontWeight: FontWeight.w800, fontSize: 16)),
          Text(label, style: GoogleFonts.inter(color: Colors.black45, fontSize: 11)),
        ],
      ),
    );
  }

  Widget _collegeCard(String college) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          const Icon(Icons.school_outlined, color: Color(0xFFB45309)),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('College', style: GoogleFonts.inter(color: Colors.black45, fontSize: 12)),
                Text(
                  college,
                  style: GoogleFonts.outfit(color: Colors.black87, fontWeight: FontWeight.w700, fontSize: 16),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _primaryActions(AppUser p) {
    if (_isOwn) {
      return SizedBox(
        width: double.infinity,
        height: 48,
        child: ElevatedButton.icon(
          onPressed: () async {
            await Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => EditProfilePage(user: p)),
            );
            if (!mounted) return;
            await _load();
            if (mounted) context.read<AuthProvider>().refreshMe();
          },
          icon: const Icon(Icons.edit_outlined),
          label: Text('Edit Profile', style: GoogleFonts.inter(fontWeight: FontWeight.w700)),
          style: ElevatedButton.styleFrom(
            backgroundColor: AppTheme.primary,
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
        ),
      );
    }

    final following = p.isFollowing == true;
    return Row(
      children: [
        Expanded(
          child: SizedBox(
            height: 48,
            child: ElevatedButton.icon(
              onPressed: _followLoading ? null : _toggleFollow,
              icon: Icon(following ? Icons.person_remove_alt_1 : Icons.person_add_alt_1),
              label: _followLoading
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : Text(
                      following ? 'Unfollow' : 'Follow',
                      style: GoogleFonts.inter(fontWeight: FontWeight.w700),
                    ),
              style: ElevatedButton.styleFrom(
                backgroundColor: following ? Colors.grey.shade200 : AppTheme.primary,
                foregroundColor: following ? Colors.black87 : Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: SizedBox(
            height: 48,
            child: OutlinedButton.icon(
              onPressed: _messageUser,
              icon: const Icon(Icons.chat_bubble_outline),
              label: Text('Message', style: GoogleFonts.inter(fontWeight: FontWeight.w700)),
              style: OutlinedButton.styleFrom(
                foregroundColor: AppTheme.primary,
                side: const BorderSide(color: AppTheme.primary),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _tabBar() {
    final tabs = <Tab>[
      const Tab(icon: Icon(Icons.article_outlined, size: 18), text: 'Posts'),
      const Tab(icon: Icon(Icons.movie_outlined, size: 18), text: 'Reels'),
      const Tab(icon: Icon(Icons.people_outline, size: 18), text: 'Followers'),
      if (_isOwn) const Tab(icon: Icon(Icons.bookmark_border, size: 18), text: 'Saved'),
      if (_isOwn) const Tab(icon: Icon(Icons.settings_outlined, size: 18), text: 'Settings'),
      if (!_isOwn) const Tab(icon: Icon(Icons.military_tech_outlined, size: 18), text: 'Badges'),
    ];

    return Container(
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: Color(0xFFE5E7EB))),
      ),
      child: TabBar(
        controller: _tabs,
        isScrollable: true,
        indicatorColor: AppTheme.primary,
        labelColor: AppTheme.primary,
        unselectedLabelColor: Colors.black45,
        labelStyle: GoogleFonts.inter(fontWeight: FontWeight.w700, fontSize: 13),
        tabs: tabs,
      ),
    );
  }

  Widget _tabBody(AppUser p) {
    final index = _tabs?.index ?? 0;
    if (_isOwn) {
      switch (index) {
        case 0:
          return _postsGrid(_posts, empty: 'No posts yet');
        case 1:
          return _postsGrid(_reels, empty: 'No reels yet', reelStyle: true);
        case 2:
          return _followersTab(p);
        case 3:
          return _savedTab();
        case 4:
          return _settingsTab(p);
      }
    } else {
      switch (index) {
        case 0:
          return _postsGrid(_posts, empty: 'No posts yet');
        case 1:
          return _postsGrid(_reels, empty: 'No reels yet', reelStyle: true);
        case 2:
          return _followersTab(p);
        case 3:
          return _badgesTab();
      }
    }
    return const SizedBox.shrink();
  }

  Widget _postsGrid(List<PostModel> posts, {required String empty, bool reelStyle = false}) {
    if (posts.isEmpty) {
      return Padding(
        padding: const EdgeInsets.only(top: 40),
        child: Center(child: Text(empty, style: GoogleFonts.inter(color: Colors.black45))),
      );
    }
    return Column(
      children: posts.map((post) {
        return Container(
          margin: const EdgeInsets.only(bottom: 12),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 12, 8, 8),
                child: Row(
                  children: [
                    Icon(
                      reelStyle ? Icons.movie_outlined : Icons.star,
                      color: AppTheme.gold,
                      size: 18,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        post.category ??
                            (post.content?.isNotEmpty == true ? post.content! : (reelStyle ? 'Reel' : 'Post')),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: GoogleFonts.outfit(color: Colors.black87, fontWeight: FontWeight.w700),
                      ),
                    ),
                    if (_isOwn)
                      PopupMenuButton<String>(
                        onSelected: (v) async {
                          if (v == 'edit') {
                            await _editPost(post);
                          } else if (v == 'delete') {
                            await _deletePost(post);
                          }
                        },
                        itemBuilder: (_) => const [
                          PopupMenuItem(value: 'edit', child: Text('Edit caption')),
                          PopupMenuItem(value: 'delete', child: Text('Delete')),
                        ],
                      ),
                  ],
                ),
              ),
              if (post.mediaUrl != null && post.mediaUrl!.isNotEmpty)
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      CachedNetworkImage(
                        imageUrl: ApiConfig.mediaUrl(post.mediaUrl),
                        width: double.infinity,
                        height: reelStyle ? 240 : 200,
                        fit: BoxFit.cover,
                        errorWidget: (context, url, error) => Container(
                          height: reelStyle ? 240 : 200,
                          color: Colors.black12,
                          alignment: Alignment.center,
                          child: Icon(
                            reelStyle ? Icons.play_circle_outline : Icons.broken_image_outlined,
                            size: 40,
                            color: Colors.black38,
                          ),
                        ),
                      ),
                      if (reelStyle || (post.mediaType ?? '').toUpperCase() == 'VIDEO')
                        const Icon(Icons.play_circle_fill, color: Colors.white70, size: 48),
                    ],
                  ),
                )
              else if (post.content != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(12, 0, 12, 8),
                  child: Text(post.content!, style: GoogleFonts.inter(color: Colors.black87)),
                ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                child: Row(
                  children: [
                    const Icon(Icons.favorite_border, color: Colors.black54, size: 20),
                    const SizedBox(width: 4),
                    Text('${post.likeCount}', style: GoogleFonts.inter(color: Colors.black54)),
                    const SizedBox(width: 16),
                    const Icon(Icons.mode_comment_outlined, color: Colors.black54, size: 20),
                    const SizedBox(width: 4),
                    Text('${post.commentCount}', style: GoogleFonts.inter(color: Colors.black54)),
                    const Spacer(),
                    Icon(
                      post.saved ? Icons.bookmark : Icons.bookmark_border,
                      color: Colors.black54,
                      size: 20,
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }

  Widget _followersTab(AppUser p) {
    final me = context.read<AuthProvider>().user;
    final others = _explorePeople.where((u) {
      final id = (u['id'] as num?)?.toInt();
      return id != null && id != p.id && id != me?.id;
    }).take(12).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${p.followersCount} followers · ${p.followingCount} following',
                style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 16),
              ),
              const SizedBox(height: 8),
              Text(
                p.isFollowing == true
                    ? 'You follow this account.'
                    : (_isOwn ? 'Your network summary' : 'You are not following this account yet.'),
                style: GoogleFonts.inter(color: Colors.black54, fontSize: 13),
              ),
              const SizedBox(height: 6),
              Text(
                'Full follower lists may be limited on mobile. Counts and follow status are shown above.',
                style: GoogleFonts.inter(color: Colors.black38, fontSize: 12),
              ),
            ],
          ),
        ),
        if (others.isNotEmpty) ...[
          const SizedBox(height: 14),
          Text('Discover more', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          ...others.map((u) {
            final username = u['username']?.toString() ?? '';
            final college = u['collegeName']?.toString() ?? '';
            return Container(
              margin: const EdgeInsets.only(bottom: 8),
              decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14)),
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                  child: Text(
                    username.isNotEmpty ? username[0].toUpperCase() : '?',
                    style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary),
                  ),
                ),
                title: Text(username, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                subtitle: college.isNotEmpty ? Text(college, style: GoogleFonts.inter(fontSize: 12)) : null,
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => ProfilePage(username: username)),
                ),
              ),
            );
          }),
        ],
      ],
    );
  }

  Widget _savedTab() {
    if (_savedPosts.isEmpty) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
        child: Column(
          children: [
            const Icon(Icons.bookmark_border, size: 40, color: Colors.black26),
            const SizedBox(height: 10),
            Text('No saved posts yet', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
            const SizedBox(height: 6),
            Text(
              'Saved items appear when the API marks posts as saved on your profile.',
              textAlign: TextAlign.center,
              style: GoogleFonts.inter(color: Colors.black45, fontSize: 13),
            ),
          ],
        ),
      );
    }
    return _postsGrid(_savedPosts, empty: 'No saved posts yet');
  }

  Widget _settingsTab(AppUser p) {
    return Column(
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('About', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
              const SizedBox(height: 12),
              _aboutLine('Bio', p.bio?.isNotEmpty == true ? p.bio! : 'No bio yet'),
              const SizedBox(height: 12),
              _aboutLine('About', p.aboutMe?.isNotEmpty == true ? p.aboutMe! : 'No details yet'),
              const SizedBox(height: 12),
              _aboutLine('Skills', p.skills?.isNotEmpty == true ? p.skills! : 'Not set'),
              const SizedBox(height: 12),
              _aboutLine('College', p.collegeName?.isNotEmpty == true ? p.collegeName! : 'Not set'),
              const SizedBox(height: 12),
              _aboutLine('Email', p.email?.isNotEmpty == true ? p.email! : 'Not set'),
              const SizedBox(height: 12),
              _aboutLine('Gender', p.gender?.isNotEmpty == true ? p.gender! : 'Not set'),
              const SizedBox(height: 12),
              _aboutLine('Date of birth', p.dob?.isNotEmpty == true ? p.dob! : 'Not set'),
              const SizedBox(height: 12),
              _aboutLine('Account', p.privateAccount ? 'Private' : 'Public'),
            ],
          ),
        ),
        const SizedBox(height: 14),
        _badgesTab(),
        const SizedBox(height: 14),
        SizedBox(
          width: double.infinity,
          child: ElevatedButton.icon(
            onPressed: () async {
              await Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => EditProfilePage(user: p)),
              );
              if (!mounted) return;
              await _load();
            },
            icon: const Icon(Icons.edit_outlined),
            label: const Text('Edit profile'),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primary,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
            ),
          ),
        ),
        const SizedBox(height: 10),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: _showResetPassword,
            icon: const Icon(Icons.lock_reset_outlined),
            label: const Text('Reset password'),
            style: OutlinedButton.styleFrom(
              foregroundColor: AppTheme.primary,
              padding: const EdgeInsets.symmetric(vertical: 14),
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _showResetPassword() async {
    final passCtrl = TextEditingController();
    final confirmCtrl = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Reset password', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: passCtrl,
              obscureText: true,
              decoration: AppTheme.dashboardInput('New password'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: confirmCtrl,
              obscureText: true,
              decoration: AppTheme.dashboardInput('Confirm password'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
            child: const Text('Update'),
          ),
        ],
      ),
    );
    final pass = passCtrl.text;
    final confirm = confirmCtrl.text;
    passCtrl.dispose();
    confirmCtrl.dispose();
    if (ok != true || !mounted) return;
    if (pass.isEmpty || pass != confirm) {
      AppTheme.showError(context, 'Passwords do not match');
      return;
    }
    try {
      await AppApi.resetPassword(pass);
      if (mounted) AppTheme.showSuccess(context, 'Password updated');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Widget _aboutLine(String title, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: GoogleFonts.inter(color: Colors.black45, fontSize: 12)),
        const SizedBox(height: 4),
        Text(value, style: GoogleFonts.inter(color: Colors.black87, height: 1.4)),
      ],
    );
  }

  Widget _badgesTab() {
    if (_badges.isEmpty) {
      return Padding(
        padding: const EdgeInsets.only(top: 24),
        child: Center(child: Text('No badges yet', style: GoogleFonts.inter(color: Colors.black45))),
      );
    }

    IconData iconFor(String id) {
      switch (id) {
        case 'social':
          return Icons.groups_outlined;
        case 'coins':
          return Icons.monetization_on_outlined;
        case 'xp':
          return Icons.bolt_outlined;
        case 'premium':
          return Icons.workspace_premium_outlined;
        default:
          return Icons.emoji_events_outlined;
      }
    }

    return Wrap(
      spacing: 10,
      runSpacing: 10,
      children: _badges.map((b) {
        final unlocked = b['unlocked'] == true;
        final title = b['title']?.toString() ?? 'Badge';
        final desc = b['description']?.toString() ?? '';
        final id = b['id']?.toString() ?? '';
        return Container(
          width: (MediaQuery.of(context).size.width - 52) / 2,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: unlocked ? AppTheme.gold.withValues(alpha: 0.6) : const Color(0xFFE5E7EB),
            ),
          ),
          child: Column(
            children: [
              Icon(iconFor(id), color: unlocked ? AppTheme.gold : Colors.black26, size: 28),
              const SizedBox(height: 8),
              Text(
                title,
                textAlign: TextAlign.center,
                style: GoogleFonts.inter(
                  color: unlocked ? Colors.black87 : Colors.black45,
                  fontWeight: FontWeight.w600,
                ),
              ),
              if (desc.isNotEmpty) ...[
                const SizedBox(height: 4),
                Text(
                  desc,
                  textAlign: TextAlign.center,
                  style: GoogleFonts.inter(color: Colors.black38, fontSize: 11),
                ),
              ],
              const SizedBox(height: 4),
              Text(
                unlocked ? 'Unlocked' : 'Locked',
                style: GoogleFonts.inter(
                  color: unlocked ? const Color(0xFFB45309) : Colors.black26,
                  fontSize: 11,
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}
