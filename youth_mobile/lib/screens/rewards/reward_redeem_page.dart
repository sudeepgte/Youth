import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

/// Partner / staff confirm screen for a scanned or entered reward code.
class RewardRedeemPage extends StatefulWidget {
  const RewardRedeemPage({super.key, required this.rewardCode});

  final String rewardCode;

  @override
  State<RewardRedeemPage> createState() => _RewardRedeemPageState();
}

class _RewardRedeemPageState extends State<RewardRedeemPage> {
  Map<String, dynamic>? _reward;
  bool _loading = true;
  bool _redeeming = false;
  String? _error;
  String? _success;

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
      final data = await AppApi.rewardByCode(widget.rewardCode.trim());
      if (!mounted) return;
      setState(() {
        _reward = data;
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

  Future<void> _confirm() async {
    setState(() => _redeeming = true);
    try {
      final res = await AppApi.redeemReward(widget.rewardCode.trim());
      if (!mounted) return;
      setState(() {
        _success = res['message']?.toString() ?? 'Reward redeemed';
        if (res['reward'] is Map) {
          _reward = Map<String, dynamic>.from(res['reward'] as Map);
        } else {
          _reward = {...?_reward, 'status': 'REDEEMED'};
        }
      });
      AppTheme.showSuccess(context, _success!);
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _redeeming = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = _reward;
    final status = r?['status']?.toString() ?? '';
    final available = status == 'AVAILABLE';

    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Redeem reward', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(_error!, style: const TextStyle(color: Colors.red), textAlign: TextAlign.center),
                        const SizedBox(height: 12),
                        ElevatedButton(onPressed: _load, child: const Text('Retry')),
                      ],
                    ),
                  ),
                )
              : ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
                    if (_success != null)
                      Container(
                        padding: const EdgeInsets.all(12),
                        margin: const EdgeInsets.only(bottom: 16),
                        decoration: BoxDecoration(
                          color: const Color(0xFFECFDF5),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: const Color(0xFFA7F3D0)),
                        ),
                        child: Text(_success!, style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: Colors.teal.shade800)),
                      ),
                    Text(
                      r?['offerTitle']?.toString() ?? 'Reward',
                      style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 22),
                    ),
                    const SizedBox(height: 6),
                    Text(r?['partnerName']?.toString() ?? '', style: GoogleFonts.inter(color: Colors.black54)),
                    const SizedBox(height: 12),
                    Text('Code: ${widget.rewardCode}', style: GoogleFonts.outfit(fontWeight: FontWeight.w700, letterSpacing: 1)),
                    Text('Status: $status', style: GoogleFonts.inter(color: AppTheme.primary)),
                    if ((r?['offerDescription']?.toString() ?? '').isNotEmpty) ...[
                      const SizedBox(height: 12),
                      Text(r!['offerDescription'].toString(), style: GoogleFonts.inter(height: 1.4)),
                    ],
                    if ((r?['eventTitle']?.toString() ?? '').isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text('Event: ${r!['eventTitle']}', style: GoogleFonts.inter(color: Colors.black54)),
                    ],
                    const SizedBox(height: 24),
                    if (available)
                      ElevatedButton(
                        onPressed: _redeeming ? null : _confirm,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.primary,
                          foregroundColor: Colors.white,
                          minimumSize: const Size.fromHeight(48),
                        ),
                        child: _redeeming
                            ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                            : const Text('Confirm redeem (partner)'),
                      )
                    else
                      Text(
                        'This reward cannot be redeemed.',
                        style: GoogleFonts.inter(color: Colors.black45),
                      ),
                  ],
                ),
    );
  }
}
