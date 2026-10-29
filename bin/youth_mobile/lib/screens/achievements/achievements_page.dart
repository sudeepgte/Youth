import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';

class AchievementsPage extends StatefulWidget {
  const AchievementsPage({super.key});

  @override
  State<AchievementsPage> createState() => _AchievementsPageState();
}

class _AchievementsPageState extends State<AchievementsPage> {
  int _xp = 0;
  String _level = 'Novice';
  List<Map<String, dynamic>> _badges = [];
  List<Map<String, dynamic>> _leaderboard = [];
  final _couponCtrl = TextEditingController();
  bool _loading = true;
  bool _redeeming = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _couponCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await AppApi.achievements();
      if (!mounted) return;
      setState(() {
        _xp = (data['xp'] as num?)?.toInt() ?? context.read<AuthProvider>().user?.xp ?? 0;
        _level = data['level']?.toString() ?? context.read<AuthProvider>().user?.level ?? 'Novice';
        final rawBadges = data['badges'] as List? ?? data['achievements'] as List? ?? [];
        _badges = rawBadges.map((e) {
          if (e is Map) return Map<String, dynamic>.from(e);
          return <String, dynamic>{'title': e.toString(), 'unlocked': true};
        }).toList();
        final lb = data['leaderboard'] as List? ?? [];
        _leaderboard = lb.map((e) => Map<String, dynamic>.from(e as Map)).toList();
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

  Future<void> _redeem() async {
    final code = _couponCtrl.text.trim();
    if (code.isEmpty) return;
    setState(() => _redeeming = true);
    try {
      final res = await AppApi.redeemCoupon(code);
      if (!mounted) return;
      await context.read<AuthProvider>().refreshMe();
      if (!mounted) return;
      AppTheme.showSuccess(context, res['message']?.toString() ?? 'Coupon redeemed');
      _couponCtrl.clear();
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _redeeming = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return DashboardScaffold(
      active: AppDrawerItem.achievements,
      title: 'Achievements',
      body: _loading
          ? const Center(child: CircularProgressIndicator())
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
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            children: [
                              const Icon(Icons.emoji_events, size: 48, color: Colors.amber),
                              const SizedBox(height: 12),
                              Text('Level $_level', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 24)),
                              const SizedBox(height: 4),
                              Text('$_xp XP', style: GoogleFonts.inter(color: Colors.grey.shade700, fontSize: 16)),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('Redeem coupon', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16)),
                              const SizedBox(height: 8),
                              Row(
                                children: [
                                  Expanded(
                                    child: TextField(
                                      controller: _couponCtrl,
                                      decoration: AppTheme.dashboardInput('Enter code'),
                                      textCapitalization: TextCapitalization.characters,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  ElevatedButton(
                                    onPressed: _redeeming ? null : _redeem,
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: AppTheme.primary,
                                      foregroundColor: Colors.white,
                                    ),
                                    child: _redeeming
                                        ? const SizedBox(
                                            width: 18,
                                            height: 18,
                                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                          )
                                        : const Text('Redeem'),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      Text('Badges', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18)),
                      const SizedBox(height: 8),
                      if (_badges.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text('No badges yet', style: GoogleFonts.inter(color: Colors.grey))),
                        )
                      else
                        GridView.builder(
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: 2,
                            childAspectRatio: 1.4,
                            crossAxisSpacing: 10,
                            mainAxisSpacing: 10,
                          ),
                          itemCount: _badges.length,
                          itemBuilder: (context, index) {
                            final badge = _badges[index];
                            final unlocked = badge['unlocked'] == true || badge['earned'] == true;
                            return Card(
                              color: unlocked ? Colors.white : Colors.grey.shade100,
                              child: Padding(
                                padding: const EdgeInsets.all(12),
                                child: Column(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Icon(
                                      unlocked ? Icons.military_tech : Icons.lock,
                                      color: unlocked ? Colors.amber : Colors.grey,
                                      size: 32,
                                    ),
                                    const SizedBox(height: 8),
                                    Text(
                                      badge['name']?.toString() ?? badge['title']?.toString() ?? 'Badge',
                                      style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 13),
                                      textAlign: TextAlign.center,
                                    ),
                                    if (badge['description'] != null)
                                      Text(
                                        badge['description'].toString(),
                                        style: GoogleFonts.inter(fontSize: 11, color: Colors.grey),
                                        textAlign: TextAlign.center,
                                        maxLines: 2,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                      if (_leaderboard.isNotEmpty) ...[
                        const SizedBox(height: 24),
                        Text('XP leaderboard', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18)),
                        const SizedBox(height: 8),
                        ..._leaderboard.asMap().entries.map((e) {
                          final i = e.key;
                          final row = e.value;
                          return Card(
                            child: ListTile(
                              leading: CircleAvatar(
                                backgroundColor: AppTheme.primary.withValues(alpha: 0.12),
                                child: Text('${i + 1}', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary)),
                              ),
                              title: Text(
                                row['username']?.toString() ?? 'User',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
                              ),
                              subtitle: Text(row['level']?.toString() ?? ''),
                              trailing: Text(
                                '${row['xp'] ?? 0} XP',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.w800),
                              ),
                            ),
                          );
                        }),
                      ],
                    ],
                  ),
                ),
    );
  }
}
