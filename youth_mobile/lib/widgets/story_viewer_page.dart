import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../config/api_config.dart';
import '../theme/app_theme.dart';

class StoryViewerPage extends StatefulWidget {
  const StoryViewerPage({
    super.key,
    required this.username,
    required this.stories,
    this.profilePhotoUrl,
  });

  final String username;
  final String? profilePhotoUrl;
  final List<Map<String, dynamic>> stories;

  @override
  State<StoryViewerPage> createState() => _StoryViewerPageState();
}

class _StoryViewerPageState extends State<StoryViewerPage> {
  late PageController _page;
  int _index = 0;

  @override
  void initState() {
    super.initState();
    _page = PageController();
  }

  @override
  void dispose() {
    _page.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.stories.isEmpty) {
      return Scaffold(
        backgroundColor: Colors.black,
        appBar: AppBar(backgroundColor: Colors.black, foregroundColor: Colors.white),
        body: Center(child: Text('No stories', style: GoogleFonts.inter(color: Colors.white70))),
      );
    }

    final story = widget.stories[_index];
    final media = story['mediaUrl']?.toString();
    final content = story['content']?.toString();

    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Stack(
          children: [
            PageView.builder(
              controller: _page,
              itemCount: widget.stories.length,
              onPageChanged: (i) => setState(() => _index = i),
              itemBuilder: (_, i) {
                final s = widget.stories[i];
                final url = s['mediaUrl']?.toString();
                final text = s['content']?.toString();
                return GestureDetector(
                  onTapUp: (d) {
                    final w = MediaQuery.of(context).size.width;
                    if (d.globalPosition.dx > w / 2) {
                      if (_index < widget.stories.length - 1) {
                        _page.nextPage(duration: const Duration(milliseconds: 200), curve: Curves.easeOut);
                      } else {
                        Navigator.pop(context);
                      }
                    } else {
                      if (_index > 0) {
                        _page.previousPage(duration: const Duration(milliseconds: 200), curve: Curves.easeOut);
                      }
                    }
                  },
                  child: Container(
                    color: AppTheme.primary.withValues(alpha: 0.15),
                    alignment: Alignment.center,
                    child: url != null && url.isNotEmpty
                        ? CachedNetworkImage(
                            imageUrl: ApiConfig.mediaUrl(url),
                            fit: BoxFit.contain,
                            width: double.infinity,
                            height: double.infinity,
                          )
                        : Padding(
                            padding: const EdgeInsets.all(24),
                            child: Text(
                              text ?? '',
                              textAlign: TextAlign.center,
                              style: GoogleFonts.outfit(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w700),
                            ),
                          ),
                  ),
                );
              },
            ),
            Positioned(
              top: 8,
              left: 12,
              right: 12,
              child: Column(
                children: [
                  Row(
                    children: List.generate(widget.stories.length, (i) {
                      return Expanded(
                        child: Container(
                          height: 3,
                          margin: const EdgeInsets.symmetric(horizontal: 2),
                          decoration: BoxDecoration(
                            color: i <= _index ? Colors.white : Colors.white24,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      );
                    }),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      CircleAvatar(
                        radius: 16,
                        backgroundImage: widget.profilePhotoUrl != null && widget.profilePhotoUrl!.isNotEmpty
                            ? CachedNetworkImageProvider(ApiConfig.mediaUrl(widget.profilePhotoUrl))
                            : null,
                        child: widget.profilePhotoUrl == null || widget.profilePhotoUrl!.isEmpty
                            ? Text(widget.username.isNotEmpty ? widget.username[0].toUpperCase() : '?')
                            : null,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          widget.username,
                          style: GoogleFonts.outfit(color: Colors.white, fontWeight: FontWeight.w700),
                        ),
                      ),
                      IconButton(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(Icons.close, color: Colors.white),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            if (media != null && media.isNotEmpty && content != null && content.isNotEmpty)
              Positioned(
                left: 16,
                right: 16,
                bottom: 32,
                child: Text(content, style: GoogleFonts.inter(color: Colors.white, fontSize: 16)),
              ),
          ],
        ),
      ),
    );
  }
}
