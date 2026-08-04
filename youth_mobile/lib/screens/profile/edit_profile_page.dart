import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

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
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _bioCtrl = TextEditingController(text: widget.user.bio ?? '');
    _collegeCtrl = TextEditingController(text: widget.user.collegeName ?? '');
    _aboutCtrl = TextEditingController(text: widget.user.aboutMe ?? '');
    _skillsCtrl = TextEditingController(text: widget.user.skills ?? '');
  }

  @override
  void dispose() {
    _bioCtrl.dispose();
    _collegeCtrl.dispose();
    _aboutCtrl.dispose();
    _skillsCtrl.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      await AppApi.updateProfile({
        'bio': _bioCtrl.text.trim(),
        'collegeName': _collegeCtrl.text.trim(),
        'aboutMe': _aboutCtrl.text.trim(),
        'skills': _skillsCtrl.text.trim(),
      });
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
          children: [
            TextField(controller: _bioCtrl, decoration: AppTheme.dashboardInput('Bio'), maxLines: 2),
            const SizedBox(height: 12),
            TextField(controller: _collegeCtrl, decoration: AppTheme.dashboardInput('College')),
            const SizedBox(height: 12),
            TextField(controller: _aboutCtrl, decoration: AppTheme.dashboardInput('About me'), maxLines: 3),
            const SizedBox(height: 12),
            TextField(controller: _skillsCtrl, decoration: AppTheme.dashboardInput('Skills')),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _saving ? null : _save,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blueAccent,
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
