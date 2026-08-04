import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class ShopPage extends StatefulWidget {
  const ShopPage({super.key});

  @override
  State<ShopPage> createState() => _ShopPageState();
}

class _ShopPageState extends State<ShopPage> {
  List<Map<String, dynamic>> _items = [];
  int _coins = 0;
  bool _loading = true;
  String? _error;
  final Set<String> _buying = {};
  final _couponCtrl = TextEditingController();

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
      final data = await AppApi.shop();
      if (!mounted) return;
      setState(() {
        _items = (data['items'] as List? ?? []).cast<Map<String, dynamic>>();
        _coins = (data['coins'] as num?)?.toInt() ??
            context.read<AuthProvider>().user?.coins ??
            0;
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

  Future<void> _buy(String itemId, String name) async {
    setState(() => _buying.add(itemId));
    try {
      final res = await AppApi.shopBuy(itemId);
      if (!mounted) return;
      AppTheme.showSuccess(context, res['message']?.toString() ?? 'Purchased $name');
      await context.read<AuthProvider>().refreshMe();
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _buying.remove(itemId));
    }
  }

  @override
  void dispose() {
    _couponCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Shop', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Row(
              children: [
                const Icon(Icons.monetization_on, color: Colors.amber, size: 18),
                const SizedBox(width: 4),
                Text('$_coins', style: GoogleFonts.inter(fontWeight: FontWeight.bold, color: Colors.amber.shade800)),
              ],
            ),
          ),
        ],
      ),
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
              : _items.isEmpty
                  ? Center(child: Text('Shop is empty', style: GoogleFonts.inter(color: Colors.grey)))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView(
                        padding: const EdgeInsets.all(12),
                        children: [
                          Card(
                            margin: const EdgeInsets.only(bottom: 12),
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('Redeem Coupon', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
                                  const SizedBox(height: 8),
                                  Row(
                                    children: [
                                      Expanded(
                                        child: TextField(
                                          controller: _couponCtrl,
                                          decoration: AppTheme.dashboardInput('Enter coupon code'),
                                        ),
                                      ),
                                      const SizedBox(width: 8),
                                      ElevatedButton(
                                        onPressed: () async {
                                          final code = _couponCtrl.text.trim();
                                          if (code.isEmpty) return;
                                          try {
                                            final res = await AppApi.redeemCoupon(code);
                                            if (!mounted) return;
                                            AppTheme.showSuccess(context, res['message']?.toString() ?? 'Coupon redeemed');
                                            _couponCtrl.clear();
                                            await context.read<AuthProvider>().refreshMe();
                                            await _load();
                                          } catch (e) {
                                            if (mounted) AppTheme.showError(context, e);
                                          }
                                        },
                                        child: const Text('Apply'),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          ),
                          ..._items.asMap().entries.map((entry) {
                            final index = entry.key;
                            final item = entry.value;
                            final id = item['id']?.toString() ?? '$index';
                            final name = item['name']?.toString() ?? item['title']?.toString() ?? 'Item';
                            final price = (item['price'] as num?)?.toInt() ?? (item['coinCost'] as num?)?.toInt() ?? 0;
                            return Card(
                              margin: const EdgeInsets.only(bottom: 10),
                              child: ListTile(
                                leading: CircleAvatar(
                                  backgroundColor: Colors.blueAccent.withValues(alpha: 0.15),
                                  child: const Icon(Icons.shopping_bag, color: Colors.blueAccent),
                                ),
                                title: Text(name, style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
                                subtitle: Text(item['description']?.toString() ?? '$price coins'),
                                trailing: ElevatedButton(
                                  onPressed: _buying.contains(id) ? null : () => _buy(id, name),
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: Colors.blueAccent,
                                    foregroundColor: Colors.white,
                                  ),
                                  child: _buying.contains(id)
                                      ? const SizedBox(
                                          width: 16,
                                          height: 16,
                                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                        )
                                      : Text('Buy ($price)'),
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
