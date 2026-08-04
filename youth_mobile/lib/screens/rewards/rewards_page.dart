import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class RewardsPage extends StatefulWidget {
  const RewardsPage({super.key});

  @override
  State<RewardsPage> createState() => _RewardsPageState();
}

class _RewardsPageState extends State<RewardsPage> {
  List<Map<String, dynamic>> _rewards = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await AppApi.rewards();
      if (!mounted) return;
      setState(() {
        _rewards = list;
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

  Future<void> _redeem(String rewardCode) async {
    try {
      await AppApi.redeemReward(rewardCode);
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Reward redeemed');
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Rewards', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: const TextStyle(color: Colors.red)))
              : _rewards.isEmpty
                  ? Center(child: Text('No rewards yet', style: GoogleFonts.inter(color: Colors.grey)))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: _rewards.length,
                        itemBuilder: (_, i) {
                          final r = _rewards[i];
                          final code = r['rewardCode']?.toString() ?? '';
                          final status = r['status']?.toString() ?? 'UNKNOWN';
                          final canRedeem = status == 'AVAILABLE' && code.isNotEmpty;
                          return Card(
                            child: ListTile(
                              title: Text(
                                r['offerTitle']?.toString() ?? 'Reward',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.bold),
                              ),
                              subtitle: Text(
                                '${r['partnerName'] ?? ''}\nCode: ${code.isEmpty ? '-' : code}\nStatus: $status',
                                style: GoogleFonts.inter(),
                              ),
                              isThreeLine: true,
                              trailing: ElevatedButton(
                                onPressed: canRedeem ? () => _redeem(code) : null,
                                child: const Text('Redeem'),
                              ),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
