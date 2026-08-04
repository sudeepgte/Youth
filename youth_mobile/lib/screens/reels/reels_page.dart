import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../config/api_config.dart';
import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class ReelsPage extends StatefulWidget {
  const ReelsPage({super.key});

  @override
  State<ReelsPage> createState() => _ReelsPageState();
}

class _ReelsPageState extends State<ReelsPage> {
  List<Map<String, dynamic>> _reels = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
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
        _reels[index] = updated;
      });
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: Text('Reels', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.white)),
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        elevation: 0,
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
                  ? Center(child: Text('No reels yet', style: GoogleFonts.inter(color: Colors.white54)))
                  : RefreshIndicator(
                      onRefresh: _load,
                      color: Colors.blueAccent,
                      child: ListView.builder(
                        itemCount: _reels.length,
                        itemBuilder: (context, index) {
                          final reel = _reels[index];
                          final mediaUrl = reel['mediaUrl']?.toString() ?? reel['videoUrl']?.toString();
                          final content = reel['content']?.toString() ?? '';
                          final username = reel['username']?.toString() ??
                              (reel['user'] is Map ? (reel['user'] as Map)['username']?.toString() : null) ??
                              '';

                          return Container(
                            height: MediaQuery.of(context).size.height * 0.75,
                            margin: const EdgeInsets.only(bottom: 8),
                            color: Colors.grey.shade900,
                            child: Stack(
                              fit: StackFit.expand,
                              children: [
                                if (mediaUrl != null && mediaUrl.isNotEmpty)
                                  CachedNetworkImage(
                                    imageUrl: ApiConfig.mediaUrl(mediaUrl),
                                    fit: BoxFit.cover,
                                    errorWidget: (_, __, ___) => const Center(
                                      child: Icon(Icons.movie, color: Colors.white54, size: 64),
                                    ),
                                  )
                                else
                                  const Center(child: Icon(Icons.movie, color: Colors.white54, size: 64)),
                                Positioned(
                                  left: 16,
                                  bottom: 24,
                                  right: 80,
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      if (username.isNotEmpty)
                                        Text('@$username', style: GoogleFonts.outfit(color: Colors.white, fontWeight: FontWeight.bold)),
                                      if (content.isNotEmpty) ...[
                                        const SizedBox(height: 8),
                                        Text(content, style: GoogleFonts.inter(color: Colors.white)),
                                      ],
                                    ],
                                  ),
                                ),
                                Positioned(
                                  right: 16,
                                  bottom: 40,
                                  child: Column(
                                    children: [
                                      IconButton(
                                        onPressed: () => _likeReel(index),
                                        icon: const Icon(Icons.favorite_border, color: Colors.white, size: 28),
                                      ),
                                      Text('${reel['likeCount'] ?? 0}', style: const TextStyle(color: Colors.white, fontSize: 12)),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
