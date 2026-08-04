import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../models/post_model.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class HomeFeedPage extends StatefulWidget {
  const HomeFeedPage({super.key});

  @override
  State<HomeFeedPage> createState() => _HomeFeedPageState();
}

class _HomeFeedPageState extends State<HomeFeedPage> {
  List<PostModel> _posts = [];
  List<Map<String, dynamic>> _stories = [];
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
      final results = await Future.wait([
        AppApi.feed(),
        AppApi.stories(),
      ]);
      final posts = results[0] as List<PostModel>;
      final stories = results[1] as List<Map<String, dynamic>>;
      if (!mounted) return;
      setState(() {
        _posts = posts;
        _stories = stories;
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

  Future<void> _like(PostModel post, int index) async {
    try {
      final res = await AppApi.likePost(post.id);
      if (!mounted) return;
      setState(() {
        _posts[index] = PostModel(
          id: post.id,
          content: post.content,
          mediaUrl: post.mediaUrl,
          mediaType: post.mediaType,
          hashtags: post.hashtags,
          postType: post.postType,
          category: post.category,
          createdAt: post.createdAt,
          likeCount: (res['likeCount'] as num?)?.toInt() ?? post.likeCount + 1,
          commentCount: post.commentCount,
          commentsDisabled: post.commentsDisabled,
          user: post.user,
        );
      });
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _showComments(PostModel post, int index) async {
    final controller = TextEditingController();
    List<dynamic> comments = [];
    try {
      comments = await AppApi.getComments(post.id);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }

    if (!mounted) return;
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom),
          child: StatefulBuilder(
            builder: (ctx, setModalState) {
              return SizedBox(
                height: MediaQuery.of(ctx).size.height * 0.6,
                child: Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.all(16),
                      child: Text('Comments', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18)),
                    ),
                    Expanded(
                      child: comments.isEmpty
                          ? Center(child: Text('No comments yet', style: GoogleFonts.inter(color: Colors.grey)))
                          : ListView.builder(
                              itemCount: comments.length,
                              itemBuilder: (_, i) {
                                final c = comments[i] as Map<String, dynamic>;
                                return ListTile(
                                  title: Text(c['username']?.toString() ?? c['author']?.toString() ?? 'User'),
                                  subtitle: Text(c['content']?.toString() ?? ''),
                                );
                              },
                            ),
                    ),
                    if (!post.commentsDisabled)
                      Padding(
                        padding: const EdgeInsets.all(12),
                        child: Row(
                          children: [
                            Expanded(
                              child: TextField(
                                controller: controller,
                                decoration: AppTheme.dashboardInput('Write a comment...'),
                              ),
                            ),
                            IconButton(
                              icon: const Icon(Icons.send, color: Colors.blueAccent),
                              onPressed: () async {
                                final text = controller.text.trim();
                                if (text.isEmpty) return;
                                try {
                                  await AppApi.commentPost(post.id, text);
                                  comments = await AppApi.getComments(post.id);
                                  controller.clear();
                                  setModalState(() {});
                                  setState(() {
                                    _posts[index] = PostModel(
                                      id: post.id,
                                      content: post.content,
                                      mediaUrl: post.mediaUrl,
                                      mediaType: post.mediaType,
                                      hashtags: post.hashtags,
                                      postType: post.postType,
                                      category: post.category,
                                      createdAt: post.createdAt,
                                      likeCount: post.likeCount,
                                      commentCount: post.commentCount + 1,
                                      commentsDisabled: post.commentsDisabled,
                                      user: post.user,
                                    );
                                  });
                                } catch (e) {
                                  if (ctx.mounted) AppTheme.showError(ctx, e);
                                }
                              },
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
              );
            },
          ),
        );
      },
    );
    controller.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_error!, style: GoogleFonts.inter(color: Colors.red)),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: _load, child: const Text('Retry')),
          ],
        ),
      );
    }
    if (_posts.isEmpty) {
      return RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.5,
              child: Center(child: Text('No posts yet', style: GoogleFonts.inter(color: Colors.grey))),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: _posts.length + 1,
        itemBuilder: (context, index) {
          if (index == 0) {
            return _storiesSection();
          }
          final post = _posts[index - 1];
          final author = post.user;
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      CircleAvatar(
                        backgroundImage: author?.profilePhotoUrl != null
                            ? CachedNetworkImageProvider(ApiConfig.mediaUrl(author!.profilePhotoUrl))
                            : null,
                        child: author?.profilePhotoUrl == null
                            ? Text((author?.username ?? '?')[0].toUpperCase())
                            : null,
                      ),
                      const SizedBox(width: 10),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(author?.username ?? 'Unknown', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
                          if (author?.level != null)
                            Text(author!.level!, style: GoogleFonts.inter(fontSize: 11, color: Colors.grey)),
                        ],
                      ),
                    ],
                  ),
                  if (post.content != null && post.content!.isNotEmpty) ...[
                    const SizedBox(height: 10),
                    Text(post.content!, style: GoogleFonts.inter()),
                  ],
                  if (post.mediaUrl != null && post.mediaUrl!.isNotEmpty) ...[
                    const SizedBox(height: 10),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: CachedNetworkImage(
                        imageUrl: ApiConfig.mediaUrl(post.mediaUrl),
                        width: double.infinity,
                        fit: BoxFit.cover,
                        placeholder: (_, __) => const SizedBox(
                          height: 180,
                          child: Center(child: CircularProgressIndicator()),
                        ),
                        errorWidget: (_, __, ___) => const SizedBox(
                          height: 120,
                          child: Center(child: Icon(Icons.broken_image)),
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      TextButton.icon(
                        onPressed: () => _like(post, index - 1),
                        icon: const Icon(Icons.favorite_border, size: 18),
                        label: Text('${post.likeCount}'),
                      ),
                      TextButton.icon(
                        onPressed: post.commentsDisabled ? null : () => _showComments(post, index - 1),
                        icon: const Icon(Icons.comment_outlined, size: 18),
                        label: Text('${post.commentCount}'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _storiesSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Stories', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18)),
        const SizedBox(height: 8),
        if (_stories.isEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Text('No active stories', style: GoogleFonts.inter(color: Colors.grey)),
          )
        else
          SizedBox(
            height: 96,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _stories.length,
              separatorBuilder: (_, __) => const SizedBox(width: 10),
              itemBuilder: (_, i) {
                final s = _stories[i];
                final name = s['username']?.toString() ?? 'User';
                final photo = s['profilePhotoUrl']?.toString();
                return Column(
                  children: [
                    CircleAvatar(
                      radius: 30,
                      backgroundImage: (photo != null && photo.isNotEmpty)
                          ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                          : null,
                      child: (photo == null || photo.isEmpty)
                          ? Text(name[0].toUpperCase())
                          : null,
                    ),
                    const SizedBox(height: 4),
                    SizedBox(
                      width: 64,
                      child: Text(
                        name,
                        overflow: TextOverflow.ellipsis,
                        textAlign: TextAlign.center,
                        style: GoogleFonts.inter(fontSize: 12),
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
        const SizedBox(height: 8),
      ],
    );
  }
}
