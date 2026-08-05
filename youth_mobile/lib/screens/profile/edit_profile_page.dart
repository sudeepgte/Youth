import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';

import '../../config/api_config.dart';
import '../../models/user_model.dart';
import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class EditProfilePage extends StatefulWidget {
  const EditProfilePage({super.key, required this.user});

  final AppUser user;

  @override
  State<EditProfilePage> createState() => _EditProfilePageState();
}

class _EditProfilePageState extends State<EditProfilePage> {
  late final TextEditingController _bioCtrl;
  late final TextEditingController _collegeCtrl;
  late final TextEditingController _aboutCtrl;
  late final TextEditingController _skillsCtrl;
  late final TextEditingController _photoCtrl;
  late final TextEditingController _dobCtrl;
  String? _gender;
  bool _privateAccount = false;
  bool _saving = false;

  static const _genders = ['Male', 'Female', 'Other', 'Prefer not to say'];

  @override
  void initState() {
    super.initState();
    _bioCtrl = TextEditingController(text: widget.user.bio ?? '');
    _collegeCtrl = TextEditingController(text: widget.user.collegeName ?? '');
    _aboutCtrl = TextEditingController(text: widget.user.aboutMe ?? '');
    _skillsCtrl = TextEditingController(text: widget.user.skills ?? '');
    _photoCtrl = TextEditingController(text: widget.user.profilePhotoUrl ?? '');
    _dobCtrl = TextEditingController(text: widget.user.dob ?? '');
    _privateAccount = widget.user.privateAccount;
    final g = widget.user.gender;
    _gender = (g != null && _genders.contains(g)) ? g : (g?.isNotEmpty == true ? g : null);
  }

  @override
  void dispose() {
    _bioCtrl.dispose();
    _collegeCtrl.dispose();
    _aboutCtrl.dispose();
    _skillsCtrl.dispose();
    _photoCtrl.dispose();
    _dobCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDob() async {
    DateTime initial = DateTime(2000, 1, 1);
    if (_dobCtrl.text.trim().isNotEmpty) {
      initial = DateTime.tryParse(_dobCtrl.text.trim()) ?? initial;
    }
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(1950),
      lastDate: DateTime.now(),
    );
    if (picked != null) {
      setState(() {
        _dobCtrl.text =
            '${picked.year.toString().padLeft(4, '0')}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}';
      });
    }
  }

  Future<void> _pickPhoto() async {
    final file = await ImagePicker().pickImage(source: ImageSource.gallery, imageQuality: 85);
    if (file == null) return;
    setState(() => _saving = true);
    try {
      final url = await AppApi.uploadChatMedia(file.path, filename: file.name);
      if (!mounted) return;
      setState(() => _photoCtrl.text = url);
      AppTheme.showSuccess(context, 'Photo ready — tap Save to apply');
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      final body = <String, dynamic>{
        'bio': _bioCtrl.text.trim(),
        'collegeName': _collegeCtrl.text.trim(),
        'aboutMe': _aboutCtrl.text.trim(),
        'skills': _skillsCtrl.text.trim(),
        'privateAccount': _privateAccount,
        'profilePhotoUrl': _photoCtrl.text.trim(),
      };
      if (_gender != null) body['gender'] = _gender;
      final dob = _dobCtrl.text.trim();
      if (dob.isNotEmpty) body['dob'] = dob;

      await AppApi.updateProfile(body);
      if (!mounted) return;
      await context.read<AuthProvider>().refreshMe();
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Profile updated');
      Navigator.pop(context);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Edit Profile', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Email', style: GoogleFonts.inter(color: Colors.black45, fontSize: 12)),
            const SizedBox(height: 4),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey.shade300),
              ),
              child: Text(
                widget.user.email?.isNotEmpty == true ? widget.user.email! : 'Not set',
                style: GoogleFonts.inter(color: Colors.black54),
              ),
            ),
            const SizedBox(height: 12),
            TextField(controller: _bioCtrl, decoration: AppTheme.dashboardInput('Bio'), maxLines: 2),
            const SizedBox(height: 12),
            TextField(controller: _collegeCtrl, decoration: AppTheme.dashboardInput('College')),
            const SizedBox(height: 12),
            TextField(controller: _aboutCtrl, decoration: AppTheme.dashboardInput('About me'), maxLines: 3),
            const SizedBox(height: 12),
            TextField(controller: _skillsCtrl, decoration: AppTheme.dashboardInput('Skills')),
            const SizedBox(height: 12),
            Row(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundColor: AppTheme.secondary.withValues(alpha: 0.35),
                  backgroundImage: _photoCtrl.text.trim().isNotEmpty
                      ? NetworkImage(ApiConfig.mediaUrl(_photoCtrl.text.trim()))
                      : null,
                  child: _photoCtrl.text.trim().isEmpty
                      ? const Icon(Icons.person, color: AppTheme.primary)
                      : null,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _saving ? null : _pickPhoto,
                    icon: const Icon(Icons.photo_library_outlined),
                    label: const Text('Choose from gallery'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _photoCtrl,
              decoration: AppTheme.dashboardInput('Or paste photo URL'),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _gender != null && _genders.contains(_gender) ? _gender : null,
              decoration: AppTheme.dashboardInput('Gender'),
              items: _genders.map((g) => DropdownMenuItem(value: g, child: Text(g))).toList(),
              onChanged: (v) => setState(() => _gender = v),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _dobCtrl,
              readOnly: true,
              decoration: AppTheme.dashboardInput('Date of birth (YYYY-MM-DD)').copyWith(
                suffixIcon: IconButton(
                  icon: const Icon(Icons.calendar_today_outlined),
                  onPressed: _pickDob,
                ),
              ),
              onTap: _pickDob,
            ),
            const SizedBox(height: 8),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text('Private account', style: GoogleFonts.inter(fontWeight: FontWeight.w600)),
              subtitle: Text(
                'Follow requests required to see your posts',
                style: GoogleFonts.inter(color: Colors.black45, fontSize: 12),
              ),
              value: _privateAccount,
              activeThumbColor: AppTheme.primary,
              onChanged: (v) => setState(() => _privateAccount = v),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _saving ? null : _save,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primary,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
                child: _saving
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : Text('Save', style: GoogleFonts.inter(fontWeight: FontWeight.bold, color: Colors.white)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
