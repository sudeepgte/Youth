import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:image_picker/image_picker.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class CreatePostPage extends StatefulWidget {
  const CreatePostPage({super.key, this.initialType});

  /// POST | STORY | REEL
  final String? initialType;

  @override
  State<CreatePostPage> createState() => _CreatePostPageState();
}

class _CreatePostPageState extends State<CreatePostPage> {
  static const _categories = [
    'Food', 'Vlog', 'Blog', 'Gym', 'Dance', 'Singing', 'Travel', 'Fashion', 'Comedy', 'Gaming', 'Others', 'General',
  ];

  static const _suggestedTags = [
    '#youthian', '#campus', '#gaming', '#music', '#events', '#travel', '#food', '#fitness',
  ];

  static const _storyBgs = [
    Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF7C3AED), Color(0xFFBE185D),
    Color(0xFFB45309), Color(0xFF047857), Color(0xFF0E7490), Color(0xFFDC2626),
  ];

  static const _storyTexts = [
    Colors.white, Color(0xFFFDE68A), Color(0xFFA7F3D0), Color(0xFFFBCFE8), Colors.black,
  ];

  final _contentCtrl = TextEditingController();
  final _hashtagsCtrl = TextEditingController();
  final _collaboratorsCtrl = TextEditingController();
  final _picker = ImagePicker();
  File? _media;
  bool _mediaIsVideo = false;
  late String _postType;
  String _category = 'General';
  bool _submitting = false;
  Color _storyBg = const Color(0xFF0F172A);
  Color _storyText = Colors.white;
  List<Map<String, dynamic>> _people = [];
  final Set<String> _selectedCollabs = {};

  @override
  void initState() {
    super.initState();
    final t = (widget.initialType ?? 'POST').toUpperCase();
    _postType = ['POST', 'STORY', 'REEL'].contains(t) ? t : 'POST';
    _loadPeople();
  }

  @override
  void dispose() {
    _contentCtrl.dispose();
    _hashtagsCtrl.dispose();
    _collaboratorsCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadPeople() async {
    try {
      final users = await AppApi.chatUsers();
      if (!mounted) return;
      setState(() => _people = users);
    } catch (_) {}
  }

  String _hex(Color c) {
    final r = (c.r * 255.0).round().clamp(0, 255);
    final g = (c.g * 255.0).round().clamp(0, 255);
    final b = (c.b * 255.0).round().clamp(0, 255);
    return '#${r.toRadixString(16).padLeft(2, '0')}${g.toRadixString(16).padLeft(2, '0')}${b.toRadixString(16).padLeft(2, '0')}';
  }

  void _toggleTag(String tag) {
    final current = _hashtagsCtrl.text.trim();
    final parts = current
        .split(RegExp(r'[\s,]+'))
        .where((s) => s.isNotEmpty)
        .toList();
    if (parts.any((p) => p.toLowerCase() == tag.toLowerCase())) {
      parts.removeWhere((p) => p.toLowerCase() == tag.toLowerCase());
    } else {
      parts.add(tag);
    }
    setState(() => _hashtagsCtrl.text = parts.join(' '));
  }

  Future<void> _pickCollabs() async {
    final selected = Set<String>.from(_selectedCollabs);
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setLocal) {
            final h = MediaQuery.sizeOf(ctx).height * 0.55;
            return SizedBox(
              height: h,
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text('Tag collaborators', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
                  ),
                  Expanded(
                    child: _people.isEmpty
                        ? Center(child: Text('No users found', style: GoogleFonts.inter(color: AppTheme.textMuted)))
                        : ListView.builder(
                            itemCount: _people.length,
                            itemBuilder: (_, i) {
                              final u = _people[i];
                              final name = u['username']?.toString() ?? '';
                              if (name.isEmpty) return const SizedBox.shrink();
                              final on = selected.contains(name);
                              return CheckboxListTile(
                                value: on,
                                title: Text(name),
                                onChanged: (v) {
                                  setLocal(() {
                                    if (v == true) {
                                      selected.add(name);
                                    } else {
                                      selected.remove(name);
                                    }
                                  });
                                },
                              );
                            },
                          ),
                  ),
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: ElevatedButton(
                      onPressed: () {
                        setState(() {
                          _selectedCollabs
                            ..clear()
                            ..addAll(selected);
                          _collaboratorsCtrl.text = _selectedCollabs.join(', ');
                        });
                        Navigator.pop(ctx);
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primary,
                        foregroundColor: Colors.white,
                        minimumSize: const Size.fromHeight(44),
                      ),
                      child: const Text('Done'),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Future<void> _pickMedia() async {
    if (_postType == 'REEL') {
      final picked = await _picker.pickVideo(source: ImageSource.gallery);
      if (picked != null) {
        setState(() {
          _media = File(picked.path);
          _mediaIsVideo = true;
        });
      }
      return;
    }

    final choice = await AppTheme.showBottomSheet<String>(
      context,
      (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.image_outlined),
              title: const Text('Photo'),
              onTap: () => Navigator.pop(ctx, 'image'),
            ),
            if (_postType != 'STORY')
              ListTile(
                leading: const Icon(Icons.videocam_outlined),
                title: const Text('Video'),
                onTap: () => Navigator.pop(ctx, 'video'),
              ),
          ],
        ),
      ),
    );
    if (choice == null || !mounted) return;

    if (choice == 'video') {
      final picked = await _picker.pickVideo(source: ImageSource.gallery);
      if (picked != null) {
        setState(() {
          _media = File(picked.path);
          _mediaIsVideo = true;
        });
      }
    } else {
      final picked = await _picker.pickImage(source: ImageSource.gallery, imageQuality: 85);
      if (picked != null) {
        setState(() {
          _media = File(picked.path);
          _mediaIsVideo = false;
        });
      }
    }
  }

  Future<void> _submit() async {
    final content = _contentCtrl.text.trim();
    if (content.isEmpty && _media == null) {
      AppTheme.showError(context, 'Add content or media');
      return;
    }
    if (_postType == 'REEL' && _media == null) {
      AppTheme.showError(context, 'Reels need a video');
      return;
    }

    setState(() => _submitting = true);
    try {
      MultipartFile? file;
      if (_media != null) {
        file = await MultipartFile.fromFile(
          _media!.path,
          filename: _media!.path.split(Platform.pathSeparator).last,
        );
      }
      var hashtags = _hashtagsCtrl.text.trim();
      // Normalize: ensure each tag starts with #
      if (hashtags.isNotEmpty) {
        hashtags = hashtags
            .split(RegExp(r'[\s,]+'))
            .where((s) => s.isNotEmpty)
            .map((s) => s.startsWith('#') ? s : '#$s')
            .join(' ');
      }
      final collaborators = _collaboratorsCtrl.text.trim().isNotEmpty
          ? _collaboratorsCtrl.text.trim()
          : (_selectedCollabs.isEmpty ? null : _selectedCollabs.join(','));

      await AppApi.createPost(
        content: content.isEmpty ? ' ' : content,
        postType: _postType,
        hashtags: hashtags.isEmpty ? null : hashtags,
        category: _category,
        collaborators: collaborators,
        bgColor: _postType == 'STORY' && _media == null ? _hex(_storyBg) : null,
        textColor: _postType == 'STORY' && _media == null ? _hex(_storyText) : null,
        file: file,
      );
      if (!mounted) return;
      AppTheme.showSuccess(
        context,
        _postType == 'STORY' ? 'Story posted' : _postType == 'REEL' ? 'Reel posted' : 'Post created',
      );
      Navigator.pop(context);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  String get _title {
    switch (_postType) {
      case 'STORY':
        return 'Create Story';
      case 'REEL':
        return 'Create Reel';
      default:
        return 'Create Post';
    }
  }

  @override
  Widget build(BuildContext context) {
    final isTextStory = _postType == 'STORY' && _media == null;

    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text(_title, style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (isTextStory) ...[
              Container(
                width: double.infinity,
                height: 180,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: _storyBg,
                  borderRadius: BorderRadius.circular(16),
                ),
                alignment: Alignment.center,
                child: Text(
                  _contentCtrl.text.trim().isEmpty ? 'Your story text…' : _contentCtrl.text,
                  textAlign: TextAlign.center,
                  style: GoogleFonts.outfit(
                    color: _storyText,
                    fontWeight: FontWeight.w700,
                    fontSize: 22,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Text('Background', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: _storyBgs
                    .map((c) => GestureDetector(
                          onTap: () => setState(() => _storyBg = c),
                          child: Container(
                            width: 32,
                            height: 32,
                            decoration: BoxDecoration(
                              color: c,
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: _storyBg == c ? AppTheme.primary : Colors.white,
                                width: 2,
                              ),
                            ),
                          ),
                        ))
                    .toList(),
              ),
              const SizedBox(height: 12),
              Text('Text color', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: _storyTexts
                    .map((c) => GestureDetector(
                          onTap: () => setState(() => _storyText = c),
                          child: Container(
                            width: 32,
                            height: 32,
                            decoration: BoxDecoration(
                              color: c,
                              shape: BoxShape.circle,
                              border: Border.all(color: Colors.black26, width: 1),
                            ),
                          ),
                        ))
                    .toList(),
              ),
              const SizedBox(height: 16),
            ],
            TextField(
              controller: _contentCtrl,
              style: const TextStyle(color: AppTheme.fieldText, fontSize: 15),
              decoration: AppTheme.dashboardInput(
                _postType == 'STORY' ? 'Story text…' : "What's on your mind? Use @username to mention",
              ),
              maxLines: isTextStory ? 3 : 5,
              onChanged: (_) {
                if (isTextStory) setState(() {});
              },
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              value: _postType,
              style: const TextStyle(color: AppTheme.fieldText, fontSize: 15),
              dropdownColor: Colors.white,
              decoration: AppTheme.dashboardInput('Post type'),
              items: const [
                DropdownMenuItem(value: 'POST', child: Text('Post', style: TextStyle(color: AppTheme.fieldText))),
                DropdownMenuItem(value: 'STORY', child: Text('Story', style: TextStyle(color: AppTheme.fieldText))),
                DropdownMenuItem(value: 'REEL', child: Text('Reel', style: TextStyle(color: AppTheme.fieldText))),
              ],
              onChanged: (v) {
                setState(() {
                  _postType = v ?? 'POST';
                  if (_postType == 'REEL' && _media != null && !_mediaIsVideo) {
                    _media = null;
                  }
                });
              },
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              value: _categories.contains(_category) ? _category : 'General',
              style: const TextStyle(color: AppTheme.fieldText, fontSize: 15),
              dropdownColor: Colors.white,
              decoration: AppTheme.dashboardInput('Category'),
              items: _categories
                  .map((c) => DropdownMenuItem(
                        value: c,
                        child: Text(c, style: const TextStyle(color: AppTheme.fieldText)),
                      ))
                  .toList(),
              onChanged: (v) => setState(() => _category = v ?? 'General'),
            ),
            const SizedBox(height: 16),
            Text('Hashtags', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _suggestedTags.map((t) {
                final on = _hashtagsCtrl.text.toLowerCase().contains(t.toLowerCase());
                return FilterChip(
                  label: Text(t),
                  selected: on,
                  onSelected: (_) => _toggleTag(t),
                  selectedColor: AppTheme.primary.withValues(alpha: 0.2),
                );
              }).toList(),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _hashtagsCtrl,
              style: const TextStyle(color: AppTheme.fieldText, fontSize: 15),
              decoration: AppTheme.dashboardInput('Or type hashtags…'),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 16),
            Text('Collaborators', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: _pickCollabs,
              icon: const Icon(Icons.person_add_alt_1_outlined),
              label: Text(
                _selectedCollabs.isEmpty
                    ? 'Tag people'
                    : '${_selectedCollabs.length} tagged: ${_selectedCollabs.join(', ')}',
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _collaboratorsCtrl,
              style: const TextStyle(color: AppTheme.fieldText, fontSize: 15),
              decoration: AppTheme.dashboardInput('Or type usernames (comma-separated)'),
            ),
            const SizedBox(height: 16),
            if (_media != null)
              Stack(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: _mediaIsVideo
                        ? Container(
                            height: 180,
                            width: double.infinity,
                            color: Colors.black87,
                            alignment: Alignment.center,
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                const Icon(Icons.videocam, color: Colors.white70, size: 40),
                                const SizedBox(height: 8),
                                Text(
                                  _media!.path.split(Platform.pathSeparator).last,
                                  style: GoogleFonts.inter(color: Colors.white70, fontSize: 12),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          )
                        : Image.file(_media!, height: 180, width: double.infinity, fit: BoxFit.cover),
                  ),
                  Positioned(
                    top: 8,
                    right: 8,
                    child: IconButton(
                      onPressed: () => setState(() {
                        _media = null;
                        _mediaIsVideo = false;
                      }),
                      icon: const Icon(Icons.close, color: Colors.white),
                      style: IconButton.styleFrom(backgroundColor: Colors.black54),
                    ),
                  ),
                ],
              ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _pickMedia,
              icon: Icon(_postType == 'REEL' ? Icons.videocam_outlined : Icons.perm_media_outlined),
              label: Text(_postType == 'REEL'
                  ? 'Add video'
                  : _postType == 'STORY'
                      ? 'Add photo (optional)'
                      : 'Add photo / video'),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _submitting ? null : _submit,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primary,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
                child: _submitting
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : Text(
                        _postType == 'STORY' ? 'Share Story' : _postType == 'REEL' ? 'Post Reel' : 'Post',
                        style: GoogleFonts.inter(fontWeight: FontWeight.bold, color: Colors.white),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
