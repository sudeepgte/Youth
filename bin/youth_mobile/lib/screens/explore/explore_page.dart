import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../config/api_config.dart';
import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../profile/profile_page.dart';

class ExplorePage extends StatefulWidget {
  const ExplorePage({super.key, this.standalone = false});

  /// When opened via Navigator.push (not MainShell tab), show its own Scaffold/AppBar.
  final bool standalone;

  @override
  State<ExplorePage> createState() => _ExplorePageState();
}

class _ExplorePageState extends State<ExplorePage> {
  final _nameCtrl = TextEditingController();
  final _collegeCtrl = TextEditingController();
  List<Map<String, dynamic>> _users = [];
  List<Map<String, dynamic>> _requests = [];
  final Set<int> _followingIds = {};
  final Set<int> _followBusy = {};
  bool _loading = false;
  bool _requestsLoading = false;
  String? _error;

  @override
  void dispose() {
    _nameCtrl.dispose();
    _collegeCtrl.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _search();
    _loadRequests();
  }

  Future<void> _loadRequests() async {
    setState(() => _requestsLoading = true);
    try {
      final list = await AppApi.followRequests();
      if (!mounted) return;
      setState(() {
        _requests = list;
        _requestsLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _requestsLoading = false);
    }
  }

  Future<void> _search() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final users = await AppApi.exploreUsers(
        name: _nameCtrl.text.trim(),
        college: _collegeCtrl.text.trim(),
      );
      if (!mounted) return;
      setState(() {
        _users = users;
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

  Future<void> _refreshAll() async {
    await Future.wait([_search(), _loadRequests()]);
  }

  Future<void> _handleRequest(int id, bool accept) async {
    try {
      if (accept) {
        await AppApi.acceptFollowRequest(id);
      } else {
        await AppApi.rejectFollowRequest(id);
      }
      await _loadRequests();
      if (mounted) {
        AppTheme.showSuccess(context, accept ? 'Request accepted' : 'Request rejected');
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _toggleFollow(Map<String, dynamic> u) async {
    final id = (u['id'] as num?)?.toInt();
    if (id == null) return;
    final me = context.read<AuthProvider>().user;
    if (me != null && me.id == id) return;

    setState(() => _followBusy.add(id));
    try {
      final following = _followingIds.contains(id) || u['isFollowing'] == true;
      if (following) {
        await AppApi.unfollow(id);
        setState(() {
          _followingIds.remove(id);
          u['isFollowing'] = false;
        });
      } else {
        await AppApi.follow(id);
        setState(() {
          _followingIds.add(id);
          u['isFollowing'] = true;
        });
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _followBusy.remove(id));
    }
  }

  bool _isFollowing(Map<String, dynamic> u) {
    final id = (u['id'] as num?)?.toInt();
    if (id != null && _followingIds.contains(id)) return true;
    return u['isFollowing'] == true;
  }

  @override
  Widget build(BuildContext context) {
    final meId = context.watch<AuthProvider>().user?.id;

    final body = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (!widget.standalone)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
            child: Text(
              'Discover people',
              style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 22),
            ),
          ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
          child: TextField(
            controller: _nameCtrl,
            decoration: AppTheme.dashboardInput('Search by name...').copyWith(
              prefixIcon: const Icon(Icons.search_rounded, color: Colors.black45),
              suffixIcon: _nameCtrl.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear_rounded),
                      onPressed: () {
                        _nameCtrl.clear();
                        setState(() {});
                        _search();
                      },
                    )
                  : null,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            ),
            onChanged: (_) => setState(() {}),
            onSubmitted: (_) => _search(),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: TextField(
            controller: _collegeCtrl,
            decoration: AppTheme.dashboardInput('Search by college...').copyWith(
              prefixIcon: const Icon(Icons.school_outlined, color: Colors.black45),
              suffixIcon: _collegeCtrl.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear_rounded),
                      onPressed: () {
                        _collegeCtrl.clear();
                        setState(() {});
                        _search();
                      },
                    )
                  : null,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            ),
            onChanged: (_) => setState(() {}),
            onSubmitted: (_) => _search(),
          ),
        ),
        Expanded(
          child: _loading && _users.isEmpty
              ? const Center(child: CircularProgressIndicator(color: AppTheme.primary))
              : _error != null && _users.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(_error!, style: const TextStyle(color: Colors.red)),
                          const SizedBox(height: 8),
                          ElevatedButton(onPressed: _search, child: const Text('Retry')),
                        ],
                      ),
                    )
                  : RefreshIndicator(
                      color: AppTheme.primary,
                      onRefresh: _refreshAll,
                      child: ListView(
                        padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
                        children: [
                          if (_requests.isNotEmpty || _requestsLoading) ...[
                            Text(
                              'Follow requests',
                              style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 16),
                            ),
                            const SizedBox(height: 8),
                            if (_requestsLoading && _requests.isEmpty)
                              const Padding(
                                padding: EdgeInsets.symmetric(vertical: 12),
                                child: Center(
                                  child: SizedBox(
                                    width: 22,
                                    height: 22,
                                    child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.primary),
                                  ),
                                ),
                              )
                            else
                              ..._requests.map(_requestCard),
                            const SizedBox(height: 16),
                            Text(
                              'People',
                              style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 16),
                            ),
                            const SizedBox(height: 8),
                          ],
                          if (_users.isEmpty)
                            Padding(
                              padding: const EdgeInsets.only(top: 40),
                              child: Center(
                                child: Text('No users found', style: GoogleFonts.inter(color: Colors.black45)),
                              ),
                            )
                          else
                            ..._users.map((u) => Padding(
                                  padding: const EdgeInsets.only(bottom: 8),
                                  child: _userCard(u, meId),
                                )),
                        ],
                      ),
                    ),
        ),
      ],
    );

    // Always Scaffold so TextFields have Material when opened via push or as a tab.
    return Scaffold(
      backgroundColor: widget.standalone ? AppTheme.dashboardBg : Colors.transparent,
      appBar: widget.standalone
          ? AppBar(
              title: Text('Discover people', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
              backgroundColor: Colors.white,
              foregroundColor: AppTheme.textPrimary,
              elevation: 0,
            )
          : null,
      body: body,
    );
  }

  Widget _requestCard(Map<String, dynamic> r) {
    final id = (r['id'] as num?)?.toInt();
    final username = r['senderUsername']?.toString() ?? 'user';
    final photo = r['senderPhoto']?.toString();

    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppTheme.primary.withValues(alpha: 0.15)),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 22,
            backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
            backgroundImage: photo != null && photo.isNotEmpty
                ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                : null,
            child: photo == null || photo.isEmpty
                ? Text(
                    username.isNotEmpty ? username[0].toUpperCase() : '?',
                    style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary),
                  )
                : null,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(username, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                Text('Wants to follow you', style: GoogleFonts.inter(color: Colors.black45, fontSize: 12)),
              ],
            ),
          ),
          TextButton(
            onPressed: id == null ? null : () => _handleRequest(id, false),
            child: const Text('Reject'),
          ),
          const SizedBox(width: 4),
          ElevatedButton(
            onPressed: id == null ? null : () => _handleRequest(id, true),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primary,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 12),
            ),
            child: const Text('Accept'),
          ),
        ],
      ),
    );
  }

  Future<void> _showPreview(Map<String, dynamic> u) async {
    final id = (u['id'] as num?)?.toInt();
    if (id == null) return;

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) {
        return FutureBuilder<Map<String, dynamic>>(
          future: AppApi.userPreview(id),
          builder: (ctx, snap) {
            final data = snap.data ?? u;
            final username = data['username']?.toString() ?? u['username']?.toString() ?? '';
            final photo = data['profilePhotoUrl']?.toString() ?? u['profilePhotoUrl']?.toString();
            final college = data['collegeName']?.toString() ?? u['collegeName']?.toString();
            final bio = data['bio']?.toString() ?? data['aboutMe']?.toString() ?? u['bio']?.toString();
            final level = data['level']?.toString() ?? u['level']?.toString() ?? 'Novice';
            final followers = data['followersCount'];
            final isPremium = data['isPremium'] == true || u['isPremium'] == true;
            final isBoosted = data['isBoosted'] == true || u['isBoosted'] == true;
            final meId = context.read<AuthProvider>().user?.id;
            final isSelf = meId != null && id == meId;
            final following = _isFollowing({...u, ...data});
            final busy = _followBusy.contains(id);
            final h = MediaQuery.sizeOf(ctx).height * 0.55;

            return Container(
              height: h,
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
              ),
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
              child: snap.connectionState == ConnectionState.waiting && snap.data == null
                  ? const Center(child: CircularProgressIndicator(color: AppTheme.primary))
                  : Column(
                      children: [
                        Container(
                          width: 40,
                          height: 4,
                          decoration: BoxDecoration(
                            color: Colors.black12,
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ),
                        const SizedBox(height: 16),
                        CircleAvatar(
                          radius: 40,
                          backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                          backgroundImage: photo != null && photo.isNotEmpty
                              ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                              : null,
                          child: photo == null || photo.isEmpty
                              ? Text(
                                  username.isNotEmpty ? username[0].toUpperCase() : '?',
                                  style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 28, color: AppTheme.primary),
                                )
                              : null,
                        ),
                        const SizedBox(height: 12),
                        Text(username, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 22)),
                        if (college != null && college.isNotEmpty)
                          Text(college, style: GoogleFonts.inter(color: Colors.black45, fontSize: 13)),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 6,
                          runSpacing: 6,
                          alignment: WrapAlignment.center,
                          children: [
                            _badge(level, const Color(0xFFFFF8E1), const Color(0xFFB45309)),
                            if (isPremium) _badge('Premium', const Color(0xFFEEF2FF), const Color(0xFF4338CA)),
                            if (isBoosted) _badge('Boosted', const Color(0xFFFFF1F2), const Color(0xFFBE123C)),
                            if (followers != null) _badge('$followers followers', Colors.grey.shade100, Colors.black54),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Expanded(
                          child: SingleChildScrollView(
                            child: Text(
                              (bio != null && bio.isNotEmpty) ? bio : 'No bio yet.',
                              textAlign: TextAlign.center,
                              style: GoogleFonts.inter(height: 1.45, color: Colors.black87),
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: OutlinedButton(
                                onPressed: () {
                                  Navigator.pop(ctx);
                                  Navigator.push(
                                    context,
                                    MaterialPageRoute(builder: (_) => ProfilePage(username: username)),
                                  );
                                },
                                child: const Text('View profile'),
                              ),
                            ),
                            if (!isSelf) ...[
                              const SizedBox(width: 10),
                              Expanded(
                                child: ElevatedButton(
                                  onPressed: busy
                                      ? null
                                      : () async {
                                          await _toggleFollow({...u, ...data, 'id': id});
                                          if (ctx.mounted) Navigator.pop(ctx);
                                        },
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: following ? Colors.grey.shade200 : AppTheme.primary,
                                    foregroundColor: following ? Colors.black87 : Colors.white,
                                  ),
                                  child: Text(following ? 'Unfollow' : 'Follow'),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ],
                    ),
            );
          },
        );
      },
    );
  }

  Widget _badge(String label, Color bg, Color fg) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: fg.withValues(alpha: 0.35)),
      ),
      child: Text(
        label,
        style: GoogleFonts.inter(fontSize: 10, fontWeight: FontWeight.w600, color: fg),
      ),
    );
  }

  Widget _userCard(Map<String, dynamic> u, int? meId) {
    final username = u['username']?.toString() ?? '';
    final photo = u['profilePhotoUrl']?.toString();
    final college = u['collegeName']?.toString();
    final bio = u['bio']?.toString();
    final level = u['level']?.toString() ?? 'Novice';
    final id = (u['id'] as num?)?.toInt();
    final subtitle = (college != null && college.isNotEmpty) ? college : (bio ?? '');
    final isSelf = meId != null && id == meId;
    final following = _isFollowing(u);
    final busy = id != null && _followBusy.contains(id);
    final isPremium = u['isPremium'] == true;
    final isBoosted = u['isBoosted'] == true;

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => _showPreview(u),
        onLongPress: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => ProfilePage(username: username)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              Stack(
                clipBehavior: Clip.none,
                children: [
                  CircleAvatar(
                    radius: 26,
                    backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                    backgroundImage: photo != null && photo.isNotEmpty
                        ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                        : null,
                    child: photo == null || photo.isEmpty
                        ? Text(
                            username.isNotEmpty ? username[0].toUpperCase() : '?',
                            style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary),
                          )
                        : null,
                  ),
                  if (isBoosted)
                    Positioned(
                      right: -2,
                      top: -2,
                      child: Container(
                        padding: const EdgeInsets.all(2),
                        decoration: const BoxDecoration(color: Color(0xFFBE123C), shape: BoxShape.circle),
                        child: const Icon(Icons.local_fire_department, size: 12, color: Colors.white),
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
                        Flexible(
                          child: Text(
                            username,
                            style: GoogleFonts.outfit(fontWeight: FontWeight.w700, fontSize: 16),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        if (isPremium) ...[
                          const SizedBox(width: 4),
                          const Icon(Icons.verified, size: 16, color: Color(0xFF4338CA)),
                        ],
                      ],
                    ),
                    if (subtitle.isNotEmpty)
                      Text(
                        subtitle,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: GoogleFonts.inter(color: Colors.black45, fontSize: 13),
                      ),
                    const SizedBox(height: 4),
                    Wrap(
                      spacing: 6,
                      runSpacing: 4,
                      children: [
                        _badge(level, const Color(0xFFFFF8E1), const Color(0xFFB45309)),
                        if (isBoosted) _badge('Boosted', const Color(0xFFFFF1F2), const Color(0xFFBE123C)),
                        if (isPremium) _badge('Premium', const Color(0xFFEEF2FF), const Color(0xFF4338CA)),
                      ],
                    ),
                  ],
                ),
              ),
              if (!isSelf)
                SizedBox(
                  height: 36,
                  child: ElevatedButton(
                    onPressed: busy ? null : () => _toggleFollow(u),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: following ? Colors.grey.shade200 : AppTheme.primary,
                      foregroundColor: following ? Colors.black87 : Colors.white,
                      elevation: 0,
                      padding: const EdgeInsets.symmetric(horizontal: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    child: busy
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : Text(
                            following ? 'Unfollow' : 'Follow',
                            style: GoogleFonts.inter(fontWeight: FontWeight.w600, fontSize: 13),
                          ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
