import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';

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
  bool _redeeming = false;

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
      final rawItems = data['items'] as List? ?? data['shopItems'] as List? ?? [];
      setState(() {
        _items = rawItems.map((e) => Map<String, dynamic>.from(e as Map)).toList();
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

  Future<void> _applyCoupon() async {
    final code = _couponCtrl.text.trim();
    if (code.isEmpty) return;
    setState(() => _redeeming = true);
    try {
      final res = await AppApi.redeemCoupon(code);
      if (!mounted) return;
      AppTheme.showSuccess(context, res['message']?.toString() ?? 'Coupon redeemed');
      _couponCtrl.clear();
      await context.read<AuthProvider>().refreshMe();
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _redeeming = false);
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
      drawer: const AppDrawer(active: AppDrawerItem.shop),
      appBar: AppBar(
        title: Text('Shop', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        leading: Builder(
          builder: (ctx) => IconButton(
            icon: const Icon(Icons.menu, color: AppTheme.textPrimary),
            onPressed: () => Scaffold.of(ctx).openDrawer(),
          ),
        ),
        actions: [
          IconButton(
            tooltip: 'Back',
            icon: const Icon(Icons.arrow_back_rounded),
            onPressed: () => Navigator.maybePop(context),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Row(
              children: [
                const Icon(Icons.monetization_on, color: Colors.amber, size: 18),
                const SizedBox(width: 4),
                Text(
                  '$_coins',
                  style: GoogleFonts.inter(fontWeight: FontWeight.bold, color: Colors.amber.shade800),
                ),
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
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(12),
                    children: [
                      Card(
                        margin: const EdgeInsets.only(bottom: 14),
                        child: Padding(
                          padding: const EdgeInsets.all(14),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Redeem Coupon',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 16),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                'Enter a code to add coins or unlock items',
                                style: GoogleFonts.inter(fontSize: 12, color: Colors.black45),
                              ),
                              const SizedBox(height: 10),
                              Row(
                                children: [
                                  Expanded(
                                    child: TextField(
                                      controller: _couponCtrl,
                                      decoration: AppTheme.dashboardInput('Enter coupon code'),
                                      textCapitalization: TextCapitalization.characters,
                                      onSubmitted: (_) => _applyCoupon(),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  ElevatedButton(
                                    onPressed: _redeeming ? null : _applyCoupon,
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: AppTheme.primary,
                                      foregroundColor: Colors.white,
                                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                                    ),
                                    child: _redeeming
                                        ? const SizedBox(
                                            width: 16,
                                            height: 16,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                              color: Colors.white,
                                            ),
                                          )
                                        : const Text('Apply'),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                      Text(
                        'Items',
                        style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18),
                      ),
                      const SizedBox(height: 8),
                      if (_items.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(32),
                          child: Center(
                            child: Text('Shop is empty', style: GoogleFonts.inter(color: Colors.grey)),
                          ),
                        )
                      else
                        ..._items.asMap().entries.map((entry) {
                          final index = entry.key;
                          final item = entry.value;
                          final id = item['id']?.toString() ?? '$index';
                          final name =
                              item['name']?.toString() ?? item['title']?.toString() ?? 'Item';
                          final desc = item['description']?.toString() ?? item['desc']?.toString() ?? '';
                          final price = (item['price'] as num?)?.toInt() ??
                              (item['coinCost'] as num?)?.toInt() ??
                              (item['cost'] as num?)?.toInt() ??
                              0;
                          final canAfford = _coins >= price;

                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: Padding(
                              padding: const EdgeInsets.all(14),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  CircleAvatar(
                                    radius: 26,
                                    backgroundColor: AppTheme.primary.withValues(alpha: 0.12),
                                    child: const Icon(Icons.shopping_bag, color: AppTheme.primary),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          name,
                                          style: GoogleFonts.outfit(
                                            fontWeight: FontWeight.w800,
                                            fontSize: 16,
                                          ),
                                        ),
                                        if (desc.isNotEmpty) ...[
                                          const SizedBox(height: 4),
                                          Text(
                                            desc,
                                            style: GoogleFonts.inter(
                                              fontSize: 13,
                                              color: Colors.black54,
                                            ),
                                          ),
                                        ],
                                        const SizedBox(height: 8),
                                        Row(
                                          children: [
                                            const Icon(Icons.monetization_on,
                                                color: Colors.amber, size: 16),
                                            const SizedBox(width: 4),
                                            Text(
                                              '$price coins',
                                              style: GoogleFonts.inter(
                                                fontWeight: FontWeight.w700,
                                                color: Colors.amber.shade800,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  ElevatedButton(
                                    onPressed: (!canAfford || _buying.contains(id))
                                        ? null
                                        : () => _buy(id, name),
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: AppTheme.primary,
                                      foregroundColor: Colors.white,
                                    ),
                                    child: _buying.contains(id)
                                        ? const SizedBox(
                                            width: 16,
                                            height: 16,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                              color: Colors.white,
                                            ),
                                          )
                                        : Text(canAfford ? 'Buy' : 'Need coins'),
                                  ),
                                ],
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
