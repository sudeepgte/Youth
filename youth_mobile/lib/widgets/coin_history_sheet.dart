import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';

import '../services/app_api.dart';
import '../theme/app_theme.dart';

/// Coin earning history (matches web GameRewards.showHistory).
class CoinHistorySheet extends StatefulWidget {
  const CoinHistorySheet({super.key});

  static Future<void> show(BuildContext context) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => const CoinHistorySheet(),
    );
  }

  @override
  State<CoinHistorySheet> createState() => _CoinHistorySheetState();
}

class _CoinHistorySheetState extends State<CoinHistorySheet> {
  List<Map<String, dynamic>> _items = [];
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
      final list = await AppApi.gameCoinHistory();
      if (!mounted) return;
      setState(() {
        _items = list;
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
  Widget build(BuildContext context) {
    final h = MediaQuery.sizeOf(context).height * 0.65;
    return SizedBox(
      height: h,
      child: Column(
        children: [
          const SizedBox(height: 8),
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(color: Colors.black26, borderRadius: BorderRadius.circular(4)),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 16, 12, 8),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    'Coin earnings',
                    style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 20),
                  ),
                ),
                IconButton(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.close)),
              ],
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(_error!, style: const TextStyle(color: Colors.red)),
                            TextButton(onPressed: _load, child: const Text('Retry')),
                          ],
                        ),
                      )
                    : _items.isEmpty
                        ? Center(
                            child: Text(
                              'No coin activity yet — play games or earn rewards.',
                              style: GoogleFonts.inter(color: AppTheme.textMuted),
                              textAlign: TextAlign.center,
                            ),
                          )
                        : ListView.separated(
                            padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                            itemCount: _items.length,
                            separatorBuilder: (_, __) => const Divider(height: 1),
                            itemBuilder: (_, i) {
                              final t = _items[i];
                              final amount = (t['amount'] as num?)?.toInt() ?? 0;
                              final source = t['source']?.toString() ?? 'Activity';
                              final reason = t['reason']?.toString() ?? '';
                              final ts = t['timestamp']?.toString() ?? '';
                              String when = ts;
                              final parsed = DateTime.tryParse(ts);
                              if (parsed != null) {
                                when = DateFormat('MMM d, h:mm a').format(parsed.toLocal());
                              }
                              final positive = amount >= 0;
                              return ListTile(
                                contentPadding: EdgeInsets.zero,
                                leading: CircleAvatar(
                                  backgroundColor: positive
                                      ? Colors.green.withValues(alpha: 0.12)
                                      : Colors.red.withValues(alpha: 0.12),
                                  child: Icon(
                                    positive ? Icons.arrow_upward_rounded : Icons.arrow_downward_rounded,
                                    color: positive ? Colors.green : Colors.red,
                                    size: 18,
                                  ),
                                ),
                                title: Text(source, style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                                subtitle: Text(
                                  [reason, when].where((s) => s.isNotEmpty).join(' · '),
                                  style: GoogleFonts.inter(fontSize: 12, color: AppTheme.textMuted),
                                ),
                                trailing: Text(
                                  '${positive ? '+' : ''}$amount',
                                  style: GoogleFonts.outfit(
                                    fontWeight: FontWeight.w800,
                                    color: positive ? Colors.green.shade700 : Colors.red.shade700,
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
