import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:image_picker/image_picker.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class CreatePostPage extends StatefulWidget {
  const CreatePostPage({super.key});

  @override
  State<CreatePostPage> createState() => _CreatePostPageState();
}

class _CreatePostPageState extends State<CreatePostPage> {
  final _contentCtrl = TextEditingController();
  final _picker = ImagePicker();
  File? _image;
  String _postType = 'POST';
  bool _submitting = false;

  @override
  void dispose() {
    _contentCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickImage() async {
    final picked = await _picker.pickImage(source: ImageSource.gallery, imageQuality: 85);
    if (picked != null) {
      setState(() => _image = File(picked.path));
    }
  }

  Future<void> _submit() async {
    final content = _contentCtrl.text.trim();
    if (content.isEmpty && _image == null) {
      AppTheme.showError(context, 'Add content or an image');
      return;
    }

    setState(() => _submitting = true);
    try {
      MultipartFile? file;
      if (_image != null) {
        file = await MultipartFile.fromFile(_image!.path, filename: _image!.path.split(Platform.pathSeparator).last);
      }
      await AppApi.createPost(content: content, postType: _postType, file: file);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Post created');
      Navigator.pop(context);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Create Post', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _contentCtrl,
              decoration: AppTheme.dashboardInput('What\'s on your mind?'),
              maxLines: 5,
            ),
            const SizedBox(height: 16),
            DropdownButtonFormField<String>(
              value: _postType,
              decoration: AppTheme.dashboardInput('Post type'),
              items: const [
                DropdownMenuItem(value: 'POST', child: Text('Post')),
                DropdownMenuItem(value: 'STORY', child: Text('Story')),
              ],
              onChanged: (v) => setState(() => _postType = v ?? 'POST'),
            ),
            const SizedBox(height: 16),
            if (_image != null)
              Stack(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: Image.file(_image!, height: 180, width: double.infinity, fit: BoxFit.cover),
                  ),
                  Positioned(
                    top: 8,
                    right: 8,
                    child: IconButton(
                      onPressed: () => setState(() => _image = null),
                      icon: const Icon(Icons.close, color: Colors.white),
                      style: IconButton.styleFrom(backgroundColor: Colors.black54),
                    ),
                  ),
                ],
              ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _pickImage,
              icon: const Icon(Icons.image),
              label: const Text('Add image'),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _submitting ? null : _submit,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blueAccent,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
                child: _submitting
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : Text('Post', style: GoogleFonts.inter(fontWeight: FontWeight.bold, color: Colors.white)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
