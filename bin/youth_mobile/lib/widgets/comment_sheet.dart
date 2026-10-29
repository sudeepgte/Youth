import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../config/api_config.dart';
import '../services/app_api.dart';
import '../theme/app_theme.dart';
import 'rich_composer.dart';

Future<int?> showCommentSheet({
  required BuildContext context,
  required int postId,
  required bool commentsDisabled,
  int initialCount = 0,
}) async {
  final controller = TextEditingController();
  List<dynamic> comments = [];
  var sending = false;
  var count = initialCount;

  try {
    comments = await AppApi.getComments(postId);
  } catch (e) {
    if (context.mounted) AppTheme.showError(context, e);
  }
  if (!context.mounted) {
    controller.dispose();
    return null;
  }

  await showModalBottomSheet<void>(
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
            Future<void> send() async {
              final text = controller.text.trim();
              if (text.isEmpty || sending) return;
              setModalState(() => sending = true);
              try {
                final res = await AppApi.commentPost(postId, text);
                comments = await AppApi.getComments(postId);
                controller.clear();
                count = (res['count'] as num?)?.toInt() ?? count + 1;
                setModalState(() {});
              } catch (e) {
                if (ctx.mounted) AppTheme.showError(ctx, e);
              } finally {
                if (ctx.mounted) setModalState(() => sending = false);
              }
            }

            return SizedBox(
              height: MediaQuery.of(ctx).size.height * 0.72,
              child: Column(
                children: [
                  const SizedBox(height: 8),
                  Container(
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(
                      color: Colors.black26,
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Comments',
                      style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18),
                    ),
                  ),
                  Expanded(
                    child: comments.isEmpty
                        ? Center(
                            child: Text('No comments yet', style: GoogleFonts.inter(color: Colors.grey)),
                          )
                        : ListView.separated(
                            padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                            itemCount: comments.length,
                            separatorBuilder: (_, __) => const SizedBox(height: 8),
                            itemBuilder: (_, i) {
                              final c = comments[i] as Map<String, dynamic>;
                              final photo = c['photo']?.toString() ?? '';
                              final username = c['username']?.toString() ?? 'User';
                              final content = c['content']?.toString() ?? '';
                              return Container(
                                padding: const EdgeInsets.all(12),
                                decoration: BoxDecoration(
                                  color: const Color(0xFFF8FAFC),
                                  borderRadius: BorderRadius.circular(14),
                                ),
                                child: Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    CircleAvatar(
                                      radius: 18,
                                      backgroundColor: AppTheme.primary.withValues(alpha: 0.12),
                                      backgroundImage: photo.isNotEmpty
                                          ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                                          : null,
                                      child: photo.isEmpty
                                          ? Text(
                                              username.isNotEmpty ? username[0].toUpperCase() : '?',
                                              style: GoogleFonts.outfit(
                                                fontWeight: FontWeight.w700,
                                                color: AppTheme.primary,
                                              ),
                                            )
                                          : null,
                                    ),
                                    const SizedBox(width: 10),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            username,
                                            style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
                                          ),
                                          const SizedBox(height: 4),
                                          CommentContentView(content: content),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              );
                            },
                          ),
                  ),
                  if (!commentsDisabled)
                    SafeArea(
                      top: false,
                      child: Material(
                        elevation: 6,
                        color: Colors.white,
                        child: RichComposer(
                          controller: controller,
                          sending: sending,
                          hint: 'Comment with text, emoji or GIF...',
                          onSend: send,
                        ),
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
  return count;
}
