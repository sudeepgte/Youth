import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../config/api_config.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../profile/profile_page.dart';

class ExplorePage extends StatefulWidget {
  const ExplorePage({super.key});

  @override
  State<ExplorePage> createState() => _ExplorePageState();
}

class _ExplorePageState extends State<ExplorePage> {
  final _searchCtrl = TextEditingController();
  List<Map<String, dynamic>> _users = [];
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _search([String? query]) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final users = await AppApi.exploreUsers(name: query ?? _searchCtrl.text);
      if (!mounted) return;
      setState(() {
        _users = users;
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

  @override
  void initState() {
    super.initState();
    _search('');
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: TextField(
            controller: _searchCtrl,
            decoration: AppTheme.dashboardInput('Search users...').copyWith(
              suffixIcon: IconButton(
                icon: const Icon(Icons.search),
                onPressed: () => _search(),
              ),
            ),
            onSubmitted: _search,
          ),
        ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _error != null
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(_error!, style: const TextStyle(color: Colors.red)),
                          ElevatedButton(onPressed: () => _search(), child: const Text('Retry')),
                        ],
                      ),
                    )
                  : _users.isEmpty
                      ? Center(child: Text('No users found', style: GoogleFonts.inter(color: Colors.grey)))
                      : ListView.builder(
                          itemCount: _users.length,
                          itemBuilder: (context, index) {
                            final u = _users[index];
                            final username = u['username']?.toString() ?? '';
                            final photo = u['profilePhotoUrl']?.toString();
                            return Card(
                              margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                              child: ListTile(
                                leading: CircleAvatar(
                                  backgroundImage: photo != null && photo.isNotEmpty
                                      ? CachedNetworkImageProvider(ApiConfig.mediaUrl(photo))
                                      : null,
                                  child: photo == null || photo.isEmpty
                                      ? Text(username.isNotEmpty ? username[0].toUpperCase() : '?')
                                      : null,
                                ),
                                title: Text(username, style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
                                subtitle: Text(
                                  u['collegeName']?.toString() ?? u['bio']?.toString() ?? '',
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                trailing: Text(
                                  'Lv ${u['level'] ?? ''}',
                                  style: GoogleFonts.inter(fontSize: 12, color: Colors.blueAccent),
                                ),
                                onTap: () => Navigator.push(
                                  context,
                                  MaterialPageRoute(builder: (_) => ProfilePage(username: username)),
                                ),
                              ),
                            );
                          },
                        ),
        ),
      ],
    );
  }
}
