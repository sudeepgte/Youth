import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';

class WalletPage extends StatefulWidget {
  const WalletPage({super.key});

  @override
  State<WalletPage> createState() => _WalletPageState();
}

class _WalletPageState extends State<WalletPage> {
  double _balance = 0;
  int _coins = 0;
  List<Map<String, dynamic>> _transactions = [];
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
      final data = await AppApi.wallet();
      if (!mounted) return;
      setState(() {
        _balance = (data['balance'] as num?)?.toDouble() ?? 0;
        _coins = (data['coins'] as num?)?.toInt() ?? 0;
        _transactions = (data['transactions'] as List? ?? []).cast<Map<String, dynamic>>();
        _loading = false;
      });
      context.read<AuthProvider>().refreshMe();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppTheme.extractError(e);
        _loading = false;
      });
    }
  }

  Future<void> _showAmountDialog({required bool isAdd}) async {
    final ctrl = TextEditingController();
    final result = await showDialog<double>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(isAdd ? 'Add Funds' : 'Withdraw'),
        content: TextField(
          controller: ctrl,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: AppTheme.dashboardInput('Amount'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(
            onPressed: () {
              final amount = double.tryParse(ctrl.text.trim());
              if (amount == null || amount <= 0) {
                AppTheme.showError(ctx, 'Enter a valid amount');
                return;
              }
              Navigator.pop(ctx, amount);
            },
            child: Text(isAdd ? 'Add' : 'Withdraw'),
          ),
        ],
      ),
    );
    ctrl.dispose();

    if (result == null) return;
    try {
      if (isAdd) {
        await AppApi.walletAdd(result);
        if (mounted) AppTheme.showSuccess(context, 'Added ₹${result.toStringAsFixed(2)}');
      } else {
        await AppApi.walletWithdraw(result);
        if (mounted) AppTheme.showSuccess(context, 'Withdrew ₹${result.toStringAsFixed(2)}');
      }
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return DashboardScaffold(
      active: AppDrawerItem.wallet,
      title: 'Wallet',
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
                          padding: const EdgeInsets.all(20),
                          child: Column(
                            children: [
                              Text('Balance', style: GoogleFonts.inter(color: Colors.grey)),
                              Text(
                                '₹${_balance.toStringAsFixed(2)}',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 32, color: Colors.green.shade700),
                              ),
                              const SizedBox(height: 8),
                              Text('$_coins coins', style: GoogleFonts.inter(color: Colors.amber.shade800)),
                              const SizedBox(height: 16),
                              Row(
                                children: [
                                  Expanded(
                                    child: ElevatedButton(
                                      onPressed: () => _showAmountDialog(isAdd: true),
                                      style: ElevatedButton.styleFrom(backgroundColor: Colors.blueAccent),
                                      child: const Text('Add', style: TextStyle(color: Colors.white)),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: OutlinedButton(
                                      onPressed: () => _showAmountDialog(isAdd: false),
                                      child: const Text('Withdraw'),
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      Text('Transactions', style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 18)),
                      const SizedBox(height: 8),
                      if (_transactions.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text('No transactions', style: GoogleFonts.inter(color: Colors.grey))),
                        )
                      else
                        ..._transactions.map((tx) {
                          final amount = (tx['amount'] as num?)?.toDouble() ?? 0;
                          final isCredit = tx['type']?.toString().toUpperCase().contains('DEPOSIT') == true ||
                              tx['type']?.toString().toUpperCase().contains('CREDIT') == true;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 8),
                            child: ListTile(
                              leading: Icon(
                                isCredit ? Icons.arrow_downward : Icons.arrow_upward,
                                color: isCredit ? Colors.green : Colors.red,
                              ),
                              title: Text(tx['description']?.toString() ?? tx['type']?.toString() ?? 'Transaction'),
                              subtitle: Text(tx['timestamp']?.toString() ?? ''),
                              trailing: Text(
                                '${isCredit ? '+' : '-'}₹${amount.abs().toStringAsFixed(2)}',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  color: isCredit ? Colors.green.shade700 : Colors.red.shade700,
                                ),
                              ),
                            ),
                          );
                        }),
                    ],
                  ),
                ),
    );
  }
}
