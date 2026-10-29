import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import 'chat_thread_page.dart';

class NewGroupPage extends StatefulWidget {
  const NewGroupPage({super.key});

  @override
  State<NewGroupPage> createState() => _NewGroupPageState();
}

class _NewGroupPageState extends State<NewGroupPage> {
  final _nameCtrl = TextEditingController();
  final _searchCtrl = TextEditingController();
  List<Map<String, dynamic>> _users = [];
  final Set<int> _selected = {};
  final Map<int, String> _selectedNames = {};
  bool _loading = false;
  bool _creating = false;

  @override
  void dispose() {
    _nameCtrl.dispose();
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _search(String q) async {
    final query = q.trim();
    if (query.length < 2) {
      setState(() => _users = []);
      return;
    }
    setState(() => _loading = true);
    try {
      final list = await AppApi.chatUsers(query: query);
      if (!mounted) return;
      setState(() {
        _users = list;
        _loading = false;
      });
    } catch (e) {
      if (mounted) {
        setState(() => _loading = false);
        AppTheme.showError(context, e);
      }
    }
  }

  int? _uid(Map<String, dynamic> u) {
    final id = u['id'] ?? u['userId'];
    if (id is num) return id.toInt();
    if (id is String) return int.tryParse(id);
    return null;
  }

  Future<void> _create() async {
    final name = _nameCtrl.text.trim();
    if (name.isEmpty) {
      AppTheme.showError(context, 'Enter a group name');
      return;
    }
    if (_selected.isEmpty) {
      AppTheme.showError(context, 'Select at least one friend');
      return;
    }
    setState(() => _creating = true);
    try {
      final res = await AppApi.createGroupChat(name: name, participantIds: _selected.toList());
      if (!mounted) return;
      final id = (res['id'] as num?)?.toInt();
      if (id != null) {
        await Navigator.pushReplacement(
          context,
          MaterialPageRoute(
            builder: (_) => ChatThreadPage(
              conversationId: id,
              title: name,
              isGroup: true,
            ),
          ),
        );
      } else {
        AppTheme.showSuccess(context, 'Group created');
        Navigator.pop(context, true);
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _creating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('New group', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: AppTheme.textPrimary,
        elevation: 0,
        actions: [
          TextButton(
            onPressed: _creating ? null : _create,
            child: _creating
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : Text('Create', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary)),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _nameCtrl,
              decoration: AppTheme.dashboardInput('Group name *'),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: TextField(
              controller: _searchCtrl,
              decoration: AppTheme.dashboardInput('Search friends…').copyWith(
                prefixIcon: const Icon(Icons.search),
              ),
              onChanged: _search,
            ),
          ),
          if (_selected.isNotEmpty)
            SizedBox(
              height: 44,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                children: _selected.map((id) {
                  return Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: InputChip(
                      label: Text(_selectedNames[id] ?? '$id'),
                      onDeleted: () => setState(() {
                        _selected.remove(id);
                        _selectedNames.remove(id);
                      }),
                    ),
                  );
                }).toList(),
              ),
            ),
          if (_loading) const LinearProgressIndicator(minHeight: 2, color: AppTheme.primary),
          Expanded(
            child: ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
              itemCount: _users.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (_, i) {
                final u = _users[i];
                final id = _uid(u);
                final name = u['username']?.toString() ?? 'User';
                final photo = u['profilePhotoUrl']?.toString();
                final selected = id != null && _selected.contains(id);
                return Material(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(14),
                  child: ListTile(
                    onTap: id == null
                        ? null
                        : () => setState(() {
                              if (selected) {
                                _selected.remove(id);
                                _selectedNames.remove(id);
                              } else {
                                _selected.add(id);
                                _selectedNames[id] = name;
                              }
                            }),
                    leading: CircleAvatar(
                      backgroundColor: AppTheme.primary.withValues(alpha: 0.15),
                      backgroundImage: photo != null && photo.isNotEmpty
                          ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                          : null,
                      child: photo == null || photo.isEmpty
                          ? Text(name.isNotEmpty ? name[0].toUpperCase() : '?')
                          : null,
                    ),
                    title: Text(name, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                    trailing: Icon(
                      selected ? Icons.check_circle : Icons.circle_outlined,
                      color: selected ? AppTheme.primary : Colors.black26,
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
