import 'package:cached_network_image/cached_network_image.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../config/gif_config.dart';
import '../theme/app_theme.dart';

/// Detects GIF / media URLs stored in comment or message text.
bool isMediaUrl(String text) {
  final t = text.trim();
  if (t.isEmpty || t.contains(' ')) return false;
  final lower = t.toLowerCase();
  return lower.startsWith('http') &&
      (lower.contains('giphy.com') ||
          lower.contains('tenor.com') ||
          lower.contains('media.tenor') ||
          lower.endsWith('.gif') ||
          lower.endsWith('.webp') ||
          lower.contains('/media/'));
}

/// Renders plain text or an inline GIF/image for comments & chat.
class CommentContentView extends StatelessWidget {
  const CommentContentView({
    super.key,
    required this.content,
    this.textStyle,
    this.maxGifHeight = 140,
  });

  final String content;
  final TextStyle? textStyle;
  final double maxGifHeight;

  @override
  Widget build(BuildContext context) {
    final text = content.trim();
    if (isMediaUrl(text)) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: CachedNetworkImage(
          imageUrl: text,
          height: maxGifHeight,
          fit: BoxFit.contain,
          placeholder: (_, __) => SizedBox(
            height: 80,
            child: Center(child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.primary)),
          ),
          errorWidget: (_, __, ___) => Text(text, style: textStyle),
        ),
      );
    }
    return Text(text, style: textStyle ?? GoogleFonts.inter(color: Colors.black87, height: 1.35));
  }
}

enum _ComposerPanel { none, emoji, gif, sticker }

/// Text field with emoji, sticker, and GIF pickers.
class RichComposer extends StatefulWidget {
  const RichComposer({
    super.key,
    required this.controller,
    required this.onSend,
    this.hint = 'Write a comment...',
    this.sending = false,
    this.enabled = true,
    this.onAttach,
    this.onTextChanged,
    this.onSendMedia,
  });

  final TextEditingController controller;
  final VoidCallback onSend;
  final String hint;
  final bool sending;
  final bool enabled;
  final VoidCallback? onAttach;
  /// Called when a GIF (or other remote media) is picked — prefer this over stuffing URL into text.
  final ValueChanged<String>? onSendMedia;
  final ValueChanged<String>? onTextChanged;

  @override
  State<RichComposer> createState() => _RichComposerState();
}

class _RichComposerState extends State<RichComposer> {
  _ComposerPanel _panel = _ComposerPanel.none;
  final _focus = FocusNode();

  static const _emojis = [
    '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊',
    '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😋', '😛', '😜', '🤪', '😝',
    '🤑', '🤗', '🤭', '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒',
    '🙄', '😬', '😮‍💨', '🤥', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕',
    '🤢', '🤮', '🥵', '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '😎', '🤓', '🧐',
    '😕', '😟', '🙁', '☹️', '😮', '😯', '😲', '😳', '🥺', '😦', '😧', '😨',
    '😰', '😥', '😢', '😭', '😱', '😖', '😣', '😞', '😓', '😩', '😫', '🥱',
    '😤', '😡', '😠', '🤬', '😈', '👿', '💀', '☠️', '💩', '🤡', '👹', '👺',
    '👻', '👽', '👾', '🤖', '👋', '🤚', '🖐️', '✋', '🖖', '👌', '🤌', '🤏',
    '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '👍',
    '👎', '✊', '👊', '🤛', '🤜', '👏', '🙌', '👐', '🤲', '🤝', '🙏', '✍️',
    '💅', '🤳', '💪', '🦾', '🦿', '🦵', '🦶', '👂', '🦻', '👃', '🧠', '🫀',
    '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕',
    '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉️', '☸️',
    '🔥', '✨', '⭐', '🌟', '💫', '💥', '💢', '💦', '💨', '🕊️', '💯', '🎉',
    '🎊', '🎈', '🎁', '🏆', '🥇', '🥈', '🥉', '⚽', '🏀', '🏈', '⚾', '🎾',
    '🎮', '🕹️', '🎲', '🎯', '🎸', '🎹', '🎺', '🎻', '🥁', '🎤', '🎧', '🎬',
    '🍕', '🍔', '🍟', '🌮', '🍣', '🍩', '🍪', '🎂', '🍰', '☕', '🍺', '🍷',
  ];

  static const _stickers = [
    '🔥', '💯', '😂', '❤️', '😍', '🥺', '😭', '👏', '🙌', '💪',
    '🎉', '✨', '😎', '🤔', '👀', '💀', '🙏', '👍', '👎', '🤩',
    '🥳', '😴', '🤯', '😤', '🥰', '😘', '🤗', '🫡', '🤝', '💥',
  ];

  @override
  void dispose() {
    _focus.dispose();
    super.dispose();
  }

  void _insert(String value) {
    final c = widget.controller;
    final text = c.text;
    final sel = c.selection;
    final start = sel.start >= 0 ? sel.start : text.length;
    final end = sel.end >= 0 ? sel.end : text.length;
    final next = text.replaceRange(start, end, value);
    c.value = TextEditingValue(
      text: next,
      selection: TextSelection.collapsed(offset: start + value.length),
    );
    widget.onTextChanged?.call(next);
    setState(() {});
  }

  void _sendGif(String url) {
    setState(() => _panel = _ComposerPanel.none);
    if (widget.onSendMedia != null) {
      widget.onSendMedia!(url);
    } else {
      widget.controller.text = url;
      widget.onSend();
    }
  }

  void _sendSticker(String sticker) {
    setState(() => _panel = _ComposerPanel.none);
    widget.controller.text = sticker;
    widget.onSend();
  }

  void _toggle(_ComposerPanel panel) {
    if (!widget.enabled) return;
    setState(() {
      _panel = _panel == panel ? _ComposerPanel.none : panel;
    });
    if (_panel == _ComposerPanel.none) {
      _focus.requestFocus();
    } else {
      _focus.unfocus();
    }
  }

  @override
  Widget build(BuildContext context) {
    final canAct = widget.enabled && !widget.sending;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Action toolbar — full width so icons aren't crushed next to the field
        Padding(
          padding: const EdgeInsets.fromLTRB(4, 4, 4, 0),
          child: Row(
            children: [
              if (widget.onAttach != null)
                _ToolBtn(
                  tooltip: 'Photo',
                  onPressed: canAct ? widget.onAttach : null,
                  child: const Icon(Icons.image_outlined, size: 22, color: Colors.black54),
                ),
              _ToolBtn(
                tooltip: 'Emoji',
                onPressed: canAct ? () => _toggle(_ComposerPanel.emoji) : null,
                child: Icon(
                  Icons.emoji_emotions_outlined,
                  size: 22,
                  color: _panel == _ComposerPanel.emoji ? AppTheme.primary : Colors.black54,
                ),
              ),
              _ToolBtn(
                tooltip: 'Stickers',
                onPressed: canAct ? () => _toggle(_ComposerPanel.sticker) : null,
                child: Icon(
                  Icons.sticky_note_2_outlined,
                  size: 22,
                  color: _panel == _ComposerPanel.sticker ? AppTheme.primary : Colors.black54,
                ),
              ),
              _ToolBtn(
                tooltip: 'GIF',
                onPressed: canAct ? () => _toggle(_ComposerPanel.gif) : null,
                child: Text(
                  'GIF',
                  style: GoogleFonts.outfit(
                    fontWeight: FontWeight.w800,
                    fontSize: 13,
                    color: _panel == _ComposerPanel.gif ? AppTheme.primary : Colors.black54,
                  ),
                ),
              ),
              const Spacer(),
              if (_panel != _ComposerPanel.none)
                TextButton(
                  onPressed: () => setState(() => _panel = _ComposerPanel.none),
                  child: Text('Close', style: GoogleFonts.inter(fontSize: 13, color: Colors.black45)),
                ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 4, 8, 8),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: TextField(
                  controller: widget.controller,
                  focusNode: _focus,
                  enabled: widget.enabled,
                  style: const TextStyle(color: AppTheme.fieldText, fontSize: 15),
                  minLines: 1,
                  maxLines: 4,
                  decoration: AppTheme.dashboardInput(widget.hint).copyWith(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  ),
                  onTap: () {
                    if (_panel != _ComposerPanel.none) {
                      setState(() => _panel = _ComposerPanel.none);
                    }
                  },
                  onChanged: (v) {
                    setState(() {});
                    widget.onTextChanged?.call(v);
                  },
                  onSubmitted: (_) {
                    if (canAct) widget.onSend();
                  },
                ),
              ),
              IconButton(
                onPressed: canAct ? widget.onSend : null,
                icon: widget.sending
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.primary),
                      )
                    : const Icon(Icons.send_rounded, color: AppTheme.primary),
              ),
            ],
          ),
        ),
        if (_panel == _ComposerPanel.emoji) _emojiPanel(),
        if (_panel == _ComposerPanel.sticker) _stickerPanel(),
        if (_panel == _ComposerPanel.gif) GifPickerPanel(onPick: _sendGif),
      ],
    );
  }

  Widget _emojiPanel() {
    return Material(
      color: const Color(0xFFF8F9FB),
      child: SizedBox(
        height: 220,
        child: GridView.builder(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 8,
            mainAxisSpacing: 4,
            crossAxisSpacing: 4,
          ),
          itemCount: _emojis.length,
          itemBuilder: (_, i) => InkWell(
            onTap: () => _insert(_emojis[i]),
            borderRadius: BorderRadius.circular(8),
            child: Center(child: Text(_emojis[i], style: const TextStyle(fontSize: 24))),
          ),
        ),
      ),
    );
  }

  Widget _stickerPanel() {
    return Material(
      color: const Color(0xFFF8F9FB),
      child: SizedBox(
        height: 180,
        child: GridView.builder(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 5,
            mainAxisSpacing: 8,
            crossAxisSpacing: 8,
          ),
          itemCount: _stickers.length,
          itemBuilder: (_, i) => InkWell(
            onTap: widget.enabled && !widget.sending ? () => _sendSticker(_stickers[i]) : null,
            borderRadius: BorderRadius.circular(14),
            child: Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: Colors.black12),
              ),
              alignment: Alignment.center,
              child: Text(_stickers[i], style: const TextStyle(fontSize: 36)),
            ),
          ),
        ),
      ),
    );
  }
}

class _ToolBtn extends StatelessWidget {
  const _ToolBtn({required this.child, this.onPressed, this.tooltip});

  final Widget child;
  final VoidCallback? onPressed;
  final String? tooltip;

  @override
  Widget build(BuildContext context) {
    final btn = IconButton(
      visualDensity: VisualDensity.compact,
      padding: const EdgeInsets.all(8),
      constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
      onPressed: onPressed,
      icon: child,
    );
    if (tooltip == null) return btn;
    return Tooltip(message: tooltip!, child: btn);
  }
}

class GifPickerPanel extends StatefulWidget {
  const GifPickerPanel({super.key, required this.onPick});

  final ValueChanged<String> onPick;

  @override
  State<GifPickerPanel> createState() => _GifPickerPanelState();
}

class _GifPickerPanelState extends State<GifPickerPanel> {
  final _search = TextEditingController();
  final _dio = Dio();
  List<Map<String, String>> _gifs = [];
  bool _loading = true;
  String? _error;

  /// Short, stable Giphy CDN URLs (fit in DB columns and load reliably).
  static const _fallback = [
    ('https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif', 'Funny'),
    ('https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif', 'Love'),
    ('https://media.giphy.com/media/26u4cqiYI30juCOGY/giphy.gif', 'Clap'),
    ('https://media.giphy.com/media/xT9IgG50Fb7Mi0prBC/giphy.gif', 'Fire'),
    ('https://media.giphy.com/media/l3q2K5jinAlChoCLS/giphy.gif', 'Wow'),
    ('https://media.giphy.com/media/3oz8xIsloV7zOmt81G/giphy.gif', 'Yes'),
    ('https://media.giphy.com/media/26BRv0ThflsHCqDrG/giphy.gif', 'Party'),
    ('https://media.giphy.com/media/l0MYC0LajbaPoEA2I/giphy.gif', 'Cry'),
  ];

  @override
  void initState() {
    super.initState();
    _loadTrending();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _loadTrending() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await _dio.get(
        'https://api.giphy.com/v1/gifs/trending',
        queryParameters: {
          'api_key': GifConfig.giphyApiKey,
          'limit': 24,
          'rating': 'pg-13',
        },
      );
      _gifs = _parse(res.data);
      if (_gifs.isEmpty) _gifs = _fallbackMaps();
    } catch (_) {
      _gifs = _fallbackMaps();
      _error = null;
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _searchGifs(String q) async {
    final query = q.trim();
    if (query.isEmpty) {
      await _loadTrending();
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await _dio.get(
        'https://api.giphy.com/v1/gifs/search',
        queryParameters: {
          'api_key': GifConfig.giphyApiKey,
          'q': query,
          'limit': 24,
          'rating': 'pg-13',
        },
      );
      _gifs = _parse(res.data);
      if (_gifs.isEmpty) {
        _gifs = _fallbackMaps();
        _error = 'No GIFs found — showing favorites';
      }
    } catch (_) {
      _gifs = _fallbackMaps();
      _error = 'Using offline GIFs';
    }
    if (mounted) setState(() => _loading = false);
  }

  List<Map<String, String>> _fallbackMaps() =>
      _fallback.map((e) => {'url': e.$1, 'title': e.$2}).toList();

  List<Map<String, String>> _parse(dynamic data) {
    if (data is! Map) return [];
    final list = data['data'];
    if (list is! List) return [];
    final out = <Map<String, String>>[];
    for (final item in list) {
      if (item is! Map) continue;
      final images = item['images'];
      if (images is! Map) continue;
      // Prefer shorter classic CDN paths over long signed v1 URLs
      final fixed = images['fixed_height_small'] ??
          images['fixed_width_small'] ??
          images['fixed_width'] ??
          images['downsized_small'] ??
          images['downsized'] ??
          images['original'];
      if (fixed is! Map) continue;
      var url = fixed['url']?.toString();
      if (url == null || url.isEmpty) continue;
      // Normalize to short media.giphy.com form when possible
      final id = item['id']?.toString();
      if (id != null && id.isNotEmpty && url.contains('giphy')) {
        url = 'https://media.giphy.com/media/$id/giphy.gif';
      }
      out.add({
        'url': url,
        'title': item['title']?.toString() ?? 'GIF',
      });
    }
    return out;
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFFF8F9FB),
      child: SizedBox(
        height: 280,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
              child: TextField(
                controller: _search,
                style: const TextStyle(color: AppTheme.fieldText),
                decoration: AppTheme.dashboardInput('Search GIFs...').copyWith(
                  prefixIcon: const Icon(Icons.search),
                  suffixIcon: IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () {
                      _search.clear();
                      _loadTrending();
                    },
                  ),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                ),
                onSubmitted: _searchGifs,
              ),
            ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(_error!, style: GoogleFonts.inter(fontSize: 12, color: Colors.black45)),
              ),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator(color: AppTheme.primary))
                  : GridView.builder(
                      padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 3,
                        mainAxisSpacing: 8,
                        crossAxisSpacing: 8,
                      ),
                      itemCount: _gifs.length,
                      itemBuilder: (_, i) {
                        final g = _gifs[i];
                        return InkWell(
                          onTap: () => widget.onPick(g['url']!),
                          borderRadius: BorderRadius.circular(10),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(10),
                            child: CachedNetworkImage(
                              imageUrl: g['url']!,
                              fit: BoxFit.cover,
                              placeholder: (_, __) => Container(color: Colors.black12),
                              errorWidget: (_, __, ___) => const Icon(Icons.broken_image),
                            ),
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
}
