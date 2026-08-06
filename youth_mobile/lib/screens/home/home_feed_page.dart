import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../models/post_model.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/comment_sheet.dart';
import '../../widgets/story_viewer_page.dart';
import '../battles/battle_detail_page.dart';
import '../battles/battles_page.dart';
import '../explore/explore_page.dart';
import '../music/music_page.dart';
import '../music/music_room_live_page.dart';
import '../profile/create_post_page.dart';
import '../profile/profile_page.dart';

class HomeFeedPage extends StatefulWidget {
  const HomeFeedPage({super.key});

  @override
  State<HomeFeedPage> createState() => _HomeFeedPageState();
}

class _HomeFeedPageState extends State<HomeFeedPage> {
  final _scroll = ScrollController();
  final _searchCtrl = TextEditingController();
  List<PostModel> _posts = [];
  List<Map<String, dynamic>> _storyGroups = [];
  List<Map<String, dynamic>> _activeBattles = [];
  List<Map<String, dynamic>> _suggested = [];
  List<Map<String, dynamic>> _musicRooms = [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _page = 0;
  String? _error;
  String? _category;
  String _sort = 'latest';

  static const _categories = ['All', 'Tech', 'Sports', 'Music', 'Art', 'Gaming', 'Campus', 'Other'];

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    _load(reset: true);
  }

  @override
  void dispose() {
    _scroll.dispose();
    _searchCtrl.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 400) {
      _loadMore();
    }
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) {
      setState(() {
        _loading = true;
        _error = null;
        _page = 0;
        _hasMore = true;
      });
    }
    try {
      final results = await Future.wait([
        AppApi.feed(page: 0, size: 15, category: _category),
        AppApi.stories(),
        AppApi.battles(),
        AppApi.exploreUsers().catchError((_) => <Map<String, dynamic>>[]),
        AppApi.musicRooms().catchError((_) => <Map<String, dynamic>>[]),
      ]);
      var posts = results[0] as List<PostModel>;
      posts = await _enrichStats(posts);
      posts = _applyFilters(posts);
      final stories = results[1] as List<Map<String, dynamic>>;
      final battlesMap = results[2] as Map<String, dynamic>;
      final suggested = results[3] as List<Map<String, dynamic>>;
      final rooms = results[4] as List<Map<String, dynamic>>;
      if (!mounted) return;
      setState(() {
        _posts = posts;
        _storyGroups = stories;
        _activeBattles = (battlesMap['active'] as List? ?? []).cast<Map<String, dynamic>>();
        _suggested = suggested.take(8).toList();
        _musicRooms = rooms.take(5).toList();
        _page = 0;
        _hasMore = (results[0] as List).length >= 15;
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

  Future<void> _loadMore() async {
    if (_loadingMore || !_hasMore || _loading) return;
    setState(() => _loadingMore = true);
    try {
      final next = _page + 1;
      var posts = await AppApi.feed(page: next, size: 15, category: _category);
      if (posts.isEmpty) {
        setState(() {
          _hasMore = false;
          _loadingMore = false;
        });
        return;
      }
      posts = await _enrichStats(posts);
      posts = _applyFilters(posts);
      if (!mounted) return;
      setState(() {
        _posts = [..._posts, ...posts];
        _page = next;
        _hasMore = posts.length >= 15;
        _loadingMore = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loadingMore = false);
    }
  }

  Future<List<PostModel>> _enrichStats(List<PostModel> posts) async {
    final out = <PostModel>[];
    for (final p in posts) {
      try {
        final s = await AppApi.postStats(p.id);
        out.add(p.copyWith(
          likeCount: (s['likes'] as num?)?.toInt() ?? p.likeCount,
          commentCount: (s['comments'] as num?)?.toInt() ?? p.commentCount,
          liked: s['liked'] == true,
          saved: s['saved'] == true,
        ));
      } catch (_) {
        out.add(p);
      }
    }
    return out;
  }

  List<PostModel> _applyFilters(List<PostModel> posts) {
    var list = List<PostModel>.from(posts);
    // Category is filtered server-side via feed(?category=); keep search + sort client-side.
    final q = _searchCtrl.text.trim().toLowerCase();
    if (q.isNotEmpty) {
      list = list
          .where((p) =>
              (p.content ?? '').toLowerCase().contains(q) ||
              (p.user?.username ?? '').toLowerCase().contains(q) ||
              (p.hashtags ?? '').toLowerCase().contains(q) ||
              (p.category ?? '').toLowerCase().contains(q))
          .toList();
    }
    if (_sort == 'popular') {
      list.sort((a, b) => b.likeCount.compareTo(a.likeCount));
    }
    return list;
  }

  Future<void> _like(PostModel post, int index) async {
    try {
      final res = await AppApi.likePost(post.id);
      if (!mounted) return;
      setState(() {
        _posts[index] = post.copyWith(
          liked: res['liked'] == true,
          likeCount: (res['likeCount'] as num?)?.toInt() ?? post.likeCount,
        );
      });
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _save(PostModel post, int index) async {
    try {
      final res = await AppApi.savePost(post.id);
      if (!mounted) return;
      setState(() {
        _posts[index] = post.copyWith(saved: res['saved'] == true);
      });
      AppTheme.showSuccess(context, res['saved'] == true ? 'Saved' : 'Removed from saved');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _showComments(PostModel post, int index) async {
    final newCount = await showCommentSheet(
      context: context,
      postId: post.id,
      commentsDisabled: post.commentsDisabled,
      initialCount: post.commentCount,
    );
    if (!mounted || newCount == null) return;
    setState(() {
      _posts[index] = post.copyWith(commentCount: newCount);
    });
  }

  void _openStory(Map<String, dynamic> group) {
    final stories = (group['stories'] as List? ?? [])
        .whereType<Map>()
        .map((e) => Map<String, dynamic>.from(e))
        .toList();
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => StoryViewerPage(
          username: group['username']?.toString() ?? 'User',
          profilePhotoUrl: group['profilePhotoUrl']?.toString(),
          stories: stories,
        ),
      ),
    );
  }

  Future<void> _pickCategory() async {
    final selected = await AppTheme.showBottomSheet<String>(
      context,
      (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: _categories
              .map((c) => ListTile(
                    title: Text(c),
                    trailing: (_category == c || (_category == null && c == 'All'))
                        ? const Icon(Icons.check, color: AppTheme.primary)
                        : null,
                    onTap: () => Navigator.pop(ctx, c),
                  ))
              .toList(),
        ),
      ),
    );
    if (selected == null) return;
    setState(() => _category = selected == 'All' ? null : selected);
    await _load(reset: true);
  }

  Future<void> _pickSort() async {
    final selected = await AppTheme.showBottomSheet<String>(
      context,
      (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              title: const Text('Latest'),
              trailing: _sort == 'latest' ? const Icon(Icons.check, color: AppTheme.primary) : null,
              onTap: () => Navigator.pop(ctx, 'latest'),
            ),
            ListTile(
              title: const Text('Popular'),
              trailing: _sort == 'popular' ? const Icon(Icons.check, color: AppTheme.primary) : null,
              onTap: () => Navigator.pop(ctx, 'popular'),
            ),
          ],
        ),
      ),
    );
    if (selected == null) return;
    setState(() {
      _sort = selected;
      _posts = _applyFilters(_posts);
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator(color: AppTheme.primary));
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_error!, style: GoogleFonts.inter(color: Colors.red)),
            ElevatedButton(onPressed: () => _load(reset: true), child: const Text('Retry')),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () => _load(reset: true),
      child: ListView.builder(
        controller: _scroll,
        padding: const EdgeInsets.fromLTRB(14, 8, 14, 100),
        itemCount: 7 + _posts.length + (_loadingMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index == 0) return Padding(padding: const EdgeInsets.only(bottom: 12), child: _searchRow());
          if (index == 1) return Padding(padding: const EdgeInsets.only(bottom: 12), child: _storiesSection());
          if (index == 2) return Padding(padding: const EdgeInsets.only(bottom: 12), child: _shareThoughtCard());
          if (index == 3) return Padding(padding: const EdgeInsets.only(bottom: 12), child: _filterRow());
          if (index == 4) return Padding(padding: const EdgeInsets.only(bottom: 12), child: _suggestedPeopleCard());
          if (index == 5) return Padding(padding: const EdgeInsets.only(bottom: 12), child: _musicRoomsCard());
          final postIndex = index - 6;
          if (postIndex < _posts.length) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _postCard(_posts[postIndex], postIndex),
            );
          }
          final afterPosts = index - 6 - _posts.length;
          if (_loadingMore && afterPosts == 0) {
            return const Padding(
              padding: EdgeInsets.all(16),
              child: Center(child: CircularProgressIndicator()),
            );
          }
          return Padding(padding: const EdgeInsets.only(top: 4, bottom: 12), child: _battlesCard());
        },
      ),
    );
  }

  Widget _searchRow() {
    return Row(
      children: [
        Expanded(
          child: TextField(
            controller: _searchCtrl,
            decoration: AppTheme.dashboardInput('Search people, posts...').copyWith(
              prefixIcon: const Icon(Icons.search),
              contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
            ),
            onSubmitted: (_) => _load(reset: true),
          ),
        ),
        const SizedBox(width: 10),
        InkWell(
          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const CreatePostPage())),
          borderRadius: BorderRadius.circular(14),
          child: Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              gradient: AppTheme.brandGradient,
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Icon(Icons.add, color: Colors.white, size: 28),
          ),
        ),
      ],
    );
  }

  Widget _shareThoughtCard() {
    return Container(
      decoration: AppTheme.cardDecoration(),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () async {
          await Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const CreatePostPage(initialType: 'POST')),
          );
          if (mounted) _load(reset: true);
        },
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  gradient: AppTheme.brandGradient,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.edit_square, color: Colors.white),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Share Your Thought', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 18, color: AppTheme.textPrimary)),
                    Text("What's on your mind?", style: GoogleFonts.outfit(color: AppTheme.textSecondary)),
                  ],
                ),
              ),
              const Icon(Icons.photo_outlined, color: AppTheme.textMuted),
            ],
          ),
        ),
      ),
    );
  }

  Widget _filterRow() {
    return Row(
      children: [
        InkWell(
          onTap: _pickCategory,
          borderRadius: BorderRadius.circular(20),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.82),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: const Color(0xFFBFDBFE)),
            ),
            child: Row(
              children: [
                const Icon(Icons.filter_alt, size: 16, color: AppTheme.primary),
                const SizedBox(width: 6),
                Text(_category ?? 'Filter', style: GoogleFonts.outfit(fontWeight: FontWeight.w600, color: AppTheme.textPrimary)),
                const Icon(Icons.keyboard_arrow_down, color: AppTheme.textMuted),
              ],
            ),
          ),
        ),
        const Spacer(),
        InkWell(
          onTap: _pickSort,
          child: Row(
            children: [
              Text(_sort == 'popular' ? 'Popular' : 'Latest', style: GoogleFonts.outfit(fontWeight: FontWeight.w600, color: AppTheme.textSecondary)),
              const Icon(Icons.keyboard_arrow_down, size: 18, color: AppTheme.textMuted),
            ],
          ),
        ),
      ],
    );
  }

  Widget _postCard(PostModel post, int index) {
    final author = post.user;
    return Container(
      decoration: AppTheme.cardDecoration(),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                GestureDetector(
                  onTap: author == null
                      ? null
                      : () => Navigator.push(
                            context,
                            MaterialPageRoute(builder: (_) => ProfilePage(username: author.username)),
                          ),
                  child: CircleAvatar(
                    backgroundImage: author?.profilePhotoUrl != null
                        ? CachedNetworkImageProvider(ApiConfig.mediaUrl(author!.profilePhotoUrl))
                        : null,
                    backgroundColor: AppTheme.secondary.withValues(alpha: 0.4),
                    child: author?.profilePhotoUrl == null
                        ? Text((author?.username ?? '?')[0].toUpperCase())
                        : null,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(author?.username ?? 'Unknown', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: AppTheme.textPrimary)),
                      Text(post.category ?? post.postType ?? 'Post',
                          style: GoogleFonts.outfit(fontSize: 12, color: AppTheme.textMuted)),
                    ],
                  ),
                ),
              ],
            ),
            if (post.content != null && post.content!.isNotEmpty) ...[
              const SizedBox(height: 10),
              Text(post.content!, style: GoogleFonts.outfit(fontSize: 16, color: AppTheme.textPrimary)),
            ],
            if (post.hashtags != null && post.hashtags!.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(post.hashtags!, style: GoogleFonts.outfit(color: AppTheme.primary, fontSize: 13)),
            ],
            if (post.mediaUrl != null && post.mediaUrl!.isNotEmpty) ...[
              const SizedBox(height: 10),
              ClipRRect(
                borderRadius: BorderRadius.circular(14),
                child: CachedNetworkImage(
                  imageUrl: ApiConfig.mediaUrl(post.mediaUrl),
                  width: double.infinity,
                  height: 220,
                  fit: BoxFit.cover,
                ),
              ),
            ],
            const SizedBox(height: 8),
            Row(
              children: [
                _iconCount(
                  post.liked ? Icons.favorite : Icons.favorite_border,
                  '${post.likeCount}',
                  () => _like(post, index),
                  color: post.liked ? Colors.redAccent : AppTheme.textPrimary,
                ),
                const SizedBox(width: 18),
                _iconCount(
                  Icons.mode_comment_outlined,
                  '${post.commentCount}',
                  post.commentsDisabled ? null : () => _showComments(post, index),
                ),
                const SizedBox(width: 18),
                _iconCount(
                  Icons.send_outlined,
                  '',
                  () => _sharePost(post),
                ),
                const Spacer(),
                InkWell(
                  onTap: () => _save(post, index),
                  child: Icon(
                    post.saved ? Icons.bookmark : Icons.bookmark_border,
                    color: post.saved ? AppTheme.primary : AppTheme.textMuted,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _sharePost(PostModel post) async {
    final link = '${ApiConfig.baseUrl}/dashboard?post=${post.id}';
    List<Map<String, dynamic>> users = [];
    try {
      users = await AppApi.chatUsers();
    } catch (_) {}

    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return SafeArea(
          child: SizedBox(
            height: MediaQuery.of(ctx).size.height * 0.55,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 8),
                Center(
                  child: Container(
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(color: Colors.black26, borderRadius: BorderRadius.circular(4)),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text('Share post', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
                ),
                ListTile(
                  leading: const CircleAvatar(
                    backgroundColor: Color(0xFFE0F2FE),
                    child: Icon(Icons.link, color: AppTheme.primary),
                  ),
                  title: Text('Copy link', style: GoogleFonts.outfit(fontWeight: FontWeight.w600)),
                  onTap: () async {
                    await Clipboard.setData(ClipboardData(text: link));
                    if (ctx.mounted) Navigator.pop(ctx);
                    if (mounted) AppTheme.showSuccess(context, 'Link copied');
                  },
                ),
                const Divider(height: 1),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                  child: Text('Send in chat', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: AppTheme.textMuted)),
                ),
                Expanded(
                  child: users.isEmpty
                      ? Center(child: Text('No users to share with', style: GoogleFonts.outfit(color: AppTheme.textMuted)))
                      : ListView.builder(
                          itemCount: users.length,
                          itemBuilder: (_, i) {
                            final u = users[i];
                            final id = (u['id'] as num?)?.toInt();
                            final name = u['username']?.toString() ?? 'User';
                            final photo = u['profilePhotoUrl']?.toString();
                            return ListTile(
                              leading: CircleAvatar(
                                backgroundColor: AppTheme.secondary.withValues(alpha: 0.35),
                                backgroundImage: photo != null && photo.isNotEmpty
                                    ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                                    : null,
                                child: photo == null || photo.isEmpty
                                    ? Text(name.isNotEmpty ? name[0].toUpperCase() : '?')
                                    : null,
                              ),
                              title: Text(name, style: GoogleFonts.outfit(fontWeight: FontWeight.w600)),
                              onTap: id == null
                                  ? null
                                  : () async {
                                      try {
                                        await AppApi.sharePostToUser(postId: post.id, recipientId: id);
                                        if (ctx.mounted) Navigator.pop(ctx);
                                        if (mounted) AppTheme.showSuccess(context, 'Shared with $name');
                                      } catch (e) {
                                        if (ctx.mounted) AppTheme.showError(ctx, e);
                                      }
                                    },
                            );
                          },
                        ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _iconCount(IconData icon, String value, VoidCallback? onTap, {Color? color}) {
    return InkWell(
      onTap: onTap,
      child: Row(
        children: [
          Icon(icon, size: 22, color: color ?? Colors.black87),
          if (value.isNotEmpty) ...[
            const SizedBox(width: 6),
            Text(value, style: GoogleFonts.inter()),
          ],
        ],
      ),
    );
  }

  Widget _suggestedPeopleCard() {
    if (_suggested.isEmpty) return const SizedBox.shrink();
    return Container(
      decoration: AppTheme.cardDecoration(),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text('Suggested people', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 18)),
              const Spacer(),
              TextButton(
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const ExplorePage(standalone: true)),
                ),
                child: Text('See all', style: GoogleFonts.outfit(color: AppTheme.primary, fontWeight: FontWeight.w700)),
              ),
            ],
          ),
          const SizedBox(height: 8),
          SizedBox(
            height: 108,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _suggested.length,
              separatorBuilder: (_, __) => const SizedBox(width: 10),
              itemBuilder: (_, i) {
                final u = _suggested[i];
                final name = u['username']?.toString() ?? 'User';
                final photo = u['profilePhotoUrl']?.toString();
                final college = u['collegeName']?.toString() ?? '';
                return InkWell(
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => ProfilePage(username: name)),
                  ),
                  borderRadius: BorderRadius.circular(14),
                  child: Container(
                    width: 96,
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.75),
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: const Color(0xFFBFDBFE)),
                    ),
                    child: Column(
                      children: [
                        CircleAvatar(
                          radius: 24,
                          backgroundColor: AppTheme.secondary.withValues(alpha: 0.35),
                          backgroundImage: (photo != null && photo.isNotEmpty)
                              ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                              : null,
                          child: (photo == null || photo.isEmpty)
                              ? Text(name.isNotEmpty ? name[0].toUpperCase() : '?', style: GoogleFonts.outfit(fontWeight: FontWeight.bold))
                              : null,
                        ),
                        const SizedBox(height: 6),
                        Text(name, maxLines: 1, overflow: TextOverflow.ellipsis, style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 12)),
                        if (college.isNotEmpty)
                          Text(college, maxLines: 1, overflow: TextOverflow.ellipsis, style: GoogleFonts.inter(fontSize: 10, color: AppTheme.textMuted)),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _musicRoomsCard() {
    return Container(
      decoration: AppTheme.cardDecoration(),
      padding: const EdgeInsets.all(14),
      child: Column(
        children: [
          Row(
            children: [
              Text('Music rooms', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 18)),
              const Spacer(),
              TextButton(
                onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const MusicPage())),
                child: Text('Open', style: GoogleFonts.outfit(color: AppTheme.primary, fontWeight: FontWeight.w700)),
              ),
            ],
          ),
          if (_musicRooms.isEmpty)
            Align(
              alignment: Alignment.centerLeft,
              child: Text('No live music rooms — start one from Music.', style: GoogleFonts.inter(color: AppTheme.textMuted, fontSize: 13)),
            )
          else
            ..._musicRooms.map((r) {
              final name = r['name']?.toString() ?? r['title']?.toString() ?? 'Room';
              final code = (r['code'] ?? r['roomCode'])?.toString() ?? '';
              return ListTile(
                contentPadding: EdgeInsets.zero,
                dense: true,
                leading: CircleAvatar(
                  backgroundColor: Colors.deepPurple.withValues(alpha: 0.12),
                  child: const Icon(Icons.headphones, color: Colors.deepPurple, size: 18),
                ),
                title: Text(name, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                subtitle: Text('Code $code', style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted)),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  if (code.isEmpty) {
                    Navigator.push(context, MaterialPageRoute(builder: (_) => const MusicPage()));
                    return;
                  }
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => MusicRoomLivePage(code: code, name: name)),
                  );
                },
              );
            }),
        ],
      ),
    );
  }

  Widget _battlesCard() {
    return Container(
      decoration: AppTheme.cardDecoration(),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          children: [
            Row(
              children: [
                Text('Ongoing Battles', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 20, color: AppTheme.textPrimary)),
                const Spacer(),
                TextButton(
                  onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const BattlesPage())),
                  child: Text('View All', style: GoogleFonts.outfit(color: AppTheme.primary, fontWeight: FontWeight.w700)),
                ),
              ],
            ),
            if (_activeBattles.isEmpty)
              Align(
                alignment: Alignment.centerLeft,
                child: Text('No active battles', style: GoogleFonts.outfit(color: AppTheme.textMuted)),
              )
            else
              ..._activeBattles.take(3).map((b) {
                return Container(
                  margin: const EdgeInsets.only(top: 8),
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    color: Colors.white.withValues(alpha: 0.7),
                    border: Border.all(color: const Color(0xFFBFDBFE)),
                  ),
                  child: Row(
                    children: [
                      CircleAvatar(
                        backgroundColor: AppTheme.accent.withValues(alpha: 0.2),
                        child: const Icon(Icons.emoji_events, color: AppTheme.primary),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(b['title']?.toString() ?? 'Battle',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: AppTheme.textPrimary)),
                            Text(
                              'Code • ${b['roomCode'] ?? '-'}  •  ${b['status'] ?? ''}',
                              style: GoogleFonts.outfit(fontSize: 12, color: AppTheme.textMuted),
                            ),
                          ],
                        ),
                      ),
                      TextButton(
                        onPressed: () {
                          final id = (b['id'] as num?)?.toInt();
                          if (id != null) {
                            Navigator.push(
                              context,
                              MaterialPageRoute(builder: (_) => BattleDetailPage(battleId: id)),
                            );
                          } else {
                            Navigator.push(
                              context,
                              MaterialPageRoute(builder: (_) => const BattlesPage(openJoinOnStart: true)),
                            );
                          }
                        },
                        child: Text('Join', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: AppTheme.primary)),
                      ),
                    ],
                  ),
                );
              }),
            const SizedBox(height: 8),
            TextButton(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const ExplorePage(standalone: true)),
              ),
              child: Text('Explore people', style: GoogleFonts.outfit(color: AppTheme.accentHover, fontWeight: FontWeight.w600)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _storiesSection() {
    return Container(
      decoration: AppTheme.cardDecoration(),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Row(
              children: [
                Text('Stories', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 20, color: AppTheme.textPrimary)),
                const Spacer(),
                TextButton(
                  onPressed: () async {
                    await Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const CreatePostPage(initialType: 'STORY')),
                    );
                    if (mounted) _load(reset: true);
                  },
                  child: Text('Add', style: GoogleFonts.outfit(color: AppTheme.primary, fontWeight: FontWeight.w700)),
                ),
              ],
            ),
            const SizedBox(height: 10),
            SizedBox(
              height: 94,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: _storyGroups.isEmpty ? 1 : _storyGroups.length,
                separatorBuilder: (_, _) => const SizedBox(width: 10),
                itemBuilder: (_, i) {
                  if (_storyGroups.isEmpty) {
                    return GestureDetector(
                      onTap: () async {
                        await Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => const CreatePostPage(initialType: 'STORY')),
                        );
                        if (mounted) _load(reset: true);
                      },
                      child: _storyChip('Your Story', null, highlight: true),
                    );
                  }
                  final s = _storyGroups[i];
                  return GestureDetector(
                    onTap: () => _openStory(s),
                    child: _storyChip(
                      s['username']?.toString() ?? 'User',
                      s['profilePhotoUrl']?.toString(),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _storyChip(String name, String? photo, {bool highlight = false}) {
    return SizedBox(
      width: 72,
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(2),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: highlight ? null : AppTheme.brandGradient,
              border: highlight ? Border.all(color: const Color(0xFFBFDBFE)) : null,
            ),
            child: CircleAvatar(
              radius: 28,
              backgroundColor: const Color(0xFFE0F2FE),
              backgroundImage: (photo != null && photo.isNotEmpty)
                  ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                  : null,
              child: (photo == null || photo.isEmpty)
                  ? Icon(highlight ? Icons.add : Icons.person, color: AppTheme.primary)
                  : null,
            ),
          ),
          const SizedBox(height: 4),
          Text(name, textAlign: TextAlign.center, overflow: TextOverflow.ellipsis, style: GoogleFonts.outfit(fontSize: 12, color: AppTheme.textSecondary)),
        ],
      ),
    );
  }
}
