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
import 'edit_profile_page.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key, this.username});

  final String? username;

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  AppUser? _profile;
  bool _loading = true;
  String? _error;
  bool _followLoading = false;

  String get _targetUsername =>
      widget.username ?? context.read<AuthProvider>().user?.username ?? '';

  bool get _isOwn =>
      _profile?.isOwnProfile == true ||
      _profile?.username == context.read<AuthProvider>().user?.username;

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
      final profile = await AppApi.profile(_targetUsername);
      if (!mounted) return;
      setState(() {
        _profile = profile;
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

  List<PostModel> get _posts {
    final raw = _profile?.posts;
    if (raw == null) return [];
    return raw
        .whereType<Map>()
        .map((e) => PostModel.fromJson(Map<String, dynamic>.from(e)))
        .toList();
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
    if (_profile == null) {
      return Center(child: Text('Profile not found', style: GoogleFonts.inter(color: Colors.grey)));
    }

    final p = _profile!;
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 36,
                backgroundImage: p.profilePhotoUrl != null
                    ? CachedNetworkImageProvider(ApiConfig.mediaUrl(p.profilePhotoUrl))
                    : null,
                child: p.profilePhotoUrl == null
                    ? Text(p.username.isNotEmpty ? p.username[0].toUpperCase() : '?', style: const TextStyle(fontSize: 28))
                    : null,
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(p.username, style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 22)),
                    Text('Level ${p.level} · ${p.xp} XP', style: GoogleFonts.inter(color: Colors.grey.shade700)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _stat('Followers', '${p.followersCount}'),
              _stat('Following', '${p.followingCount}'),
              _stat('Coins', '${p.coins}'),
            ],
          ),
          if (p.bio != null && p.bio!.isNotEmpty) ...[
            const SizedBox(height: 16),
            Text(p.bio!, style: GoogleFonts.inter()),
          ],
          if (p.collegeName != null) ...[
            const SizedBox(height: 8),
            Text('College: ${p.collegeName}', style: GoogleFonts.inter(color: Colors.grey.shade700)),
          ],
          const SizedBox(height: 16),
          if (_isOwn)
            ElevatedButton.icon(
              onPressed: () async {
                await Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => EditProfilePage(user: p)),
                );
                if (mounted) {
                  await _load();
                  if (mounted) context.read<AuthProvider>().refreshMe();
                }
              },
              icon: const Icon(Icons.edit),
              label: const Text('Edit Profile'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blueAccent,
                foregroundColor: Colors.white,
              ),
            )
          else
            ElevatedButton(
              onPressed: _followLoading ? null : _toggleFollow,
              style: ElevatedButton.styleFrom(
                backgroundColor: p.isFollowing == true ? Colors.grey : Colors.blueAccent,
                foregroundColor: Colors.white,
              ),
              child: _followLoading
                  ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Text(p.isFollowing == true ? 'Unfollow' : 'Follow'),
            ),
          const SizedBox(height: 24),
          Text('Posts', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18)),
          const SizedBox(height: 8),
          if (_posts.isEmpty)
            Text('No posts yet', style: GoogleFonts.inter(color: Colors.grey))
          else
            ..._posts.map((post) => Card(
                  margin: const EdgeInsets.only(bottom: 10),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (post.content != null) Text(post.content!),
                        if (post.mediaUrl != null && post.mediaUrl!.isNotEmpty) ...[
                          const SizedBox(height: 8),
                          ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: CachedNetworkImage(
                              imageUrl: ApiConfig.mediaUrl(post.mediaUrl),
                              width: double.infinity,
                              fit: BoxFit.cover,
                            ),
                          ),
                        ],
                        const SizedBox(height: 6),
                        Text('${post.likeCount} likes · ${post.commentCount} comments',
                            style: GoogleFonts.inter(fontSize: 12, color: Colors.grey)),
                      ],
                    ),
                  ),
                )),
        ],
      ),
    );
  }

  Widget _stat(String label, String value) {
    return Column(
      children: [
        Text(value, style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18)),
        Text(label, style: GoogleFonts.inter(fontSize: 12, color: Colors.grey)),
      ],
    );
  }
}
