import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/coin_history_sheet.dart';
import '../../widgets/shell_nav.dart';
import '../chat/chat_list_page.dart';
import '../explore/explore_page.dart';
import '../home/home_feed_page.dart';
import '../notifications/notifications_page.dart';
import '../profile/create_post_page.dart';
import '../profile/profile_page.dart';
import '../shop/shop_page.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _index = 0;
  int _unreadNotifications = 0;
  int _unreadChats = 0;

  late final List<Widget> _pages = [
    const HomeFeedPage(),
    const ExplorePage(),
    const ChatListPage(),
    const ProfilePage(),
  ];

  @override
  void initState() {
    super.initState();
    ShellNav.tabRequest.addListener(_onTabRequest);
    _refreshUnread();
  }

  void _onTabRequest() {
    final tab = ShellNav.tabRequest.value;
    if (tab == null) return;
    ShellNav.clear();
    if (!mounted) return;
    _goTo(tab);
  }

  @override
  void dispose() {
    ShellNav.tabRequest.removeListener(_onTabRequest);
    super.dispose();
  }

  Future<void> _refreshUnread() async {
    try {
      final results = await Future.wait([
        AppApi.notifications(),
        AppApi.unreadChatCount(),
        AppApi.followRequests().catchError((_) => <Map<String, dynamic>>[]),
        AppApi.collaborationRequests().catchError((_) => <Map<String, dynamic>>[]),
      ]);
      if (!mounted) return;
      final list = results[0] as List<Map<String, dynamic>>;
      final chatUnread = results[1] as int;
      final follows = results[2] as List<Map<String, dynamic>>;
      final collabs = results[3] as List<Map<String, dynamic>>;
      final unread = list.where((n) => n['read'] != true && n['isRead'] != true).length;
      setState(() {
        _unreadNotifications = unread + follows.length + collabs.length;
        _unreadChats = chatUnread;
      });
    } catch (_) {
      // Badge is optional — ignore failures
    }
  }

  void _goTo(int index) {
    setState(() => _index = index);
    if (index == 2) _refreshUnread();
  }

  Future<void> _openNotifications() async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const NotificationsPage()),
    );
    if (mounted) _refreshUnread();
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().user;

    return Theme(
      data: AppTheme.dashboardTheme(),
      child: Container(
        decoration: const BoxDecoration(gradient: AppTheme.bgGradient),
        child: Scaffold(
          backgroundColor: Colors.transparent,
          appBar: AppBar(
            backgroundColor: const Color(0xE6FFFFFF),
            elevation: 0,
            surfaceTintColor: Colors.transparent,
            leading: Builder(
              builder: (ctx) => IconButton(
                icon: const Icon(Icons.menu, color: AppTheme.textPrimary),
                onPressed: () => Scaffold.of(ctx).openDrawer(),
              ),
            ),
            title: Image.asset('assets/images/youthian_logo.png', height: 34),
            actions: [
              GestureDetector(
                onTap: () => CoinHistorySheet.show(context),
                child: Container(
                  margin: const EdgeInsets.symmetric(vertical: 12),
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  decoration: BoxDecoration(
                    gradient: AppTheme.brandGradient,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.monetization_on, color: Colors.white, size: 16),
                      const SizedBox(width: 4),
                      Text(
                        '${user?.coins ?? 0}',
                        style: GoogleFonts.outfit(
                          color: Colors.white,
                          fontWeight: FontWeight.w700,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              IconButton(
                tooltip: 'Shop',
                icon: const Icon(Icons.shopping_bag_outlined, color: AppTheme.primary, size: 22),
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const ShopPage()),
                ),
              ),
              IconButton(
                icon: Badge(
                  isLabelVisible: _unreadNotifications > 0,
                  label: Text(
                    _unreadNotifications > 99 ? '99+' : '$_unreadNotifications',
                    style: const TextStyle(fontSize: 10),
                  ),
                  child: const Icon(Icons.notifications_outlined, color: AppTheme.primary, size: 22),
                ),
                onPressed: _openNotifications,
              ),
              Padding(
                padding: const EdgeInsets.only(right: 10),
                child: GestureDetector(
                  onTap: () => _goTo(3),
                  child: CircleAvatar(
                    radius: 17,
                    backgroundColor: AppTheme.secondary.withValues(alpha: 0.35),
                    child: Text(
                      (user?.username.isNotEmpty == true) ? user!.username[0].toUpperCase() : 'U',
                      style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: AppTheme.primary),
                    ),
                  ),
                ),
              ),
            ],
          ),
          drawer: AppDrawer(
            active: AppDrawer.fromShellIndex(_index),
            onShellNavigate: _goTo,
          ),
          body: IndexedStack(index: _index, children: _pages),
          floatingActionButtonLocation: FloatingActionButtonLocation.centerDocked,
          floatingActionButton: Container(
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: AppTheme.brandGradient,
              boxShadow: [
                BoxShadow(
                  color: AppTheme.primary.withValues(alpha: 0.35),
                  blurRadius: 16,
                  offset: const Offset(0, 6),
                ),
              ],
            ),
            child: FloatingActionButton(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const CreatePostPage()),
              ),
              backgroundColor: Colors.transparent,
              elevation: 0,
              child: const Icon(Icons.add, size: 30, color: Colors.white),
            ),
          ),
          bottomNavigationBar: BottomAppBar(
            color: const Color(0xF2FFFFFF),
            surfaceTintColor: Colors.transparent,
            elevation: 8,
            shape: const CircularNotchedRectangle(),
            notchMargin: 6,
            child: SizedBox(
              height: 64,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _navItem(0, Icons.home_outlined, Icons.home_rounded, 'Home'),
                  _navItem(1, Icons.explore_outlined, Icons.explore_rounded, 'Explore'),
                  const SizedBox(width: 40),
                  _navItem(2, Icons.chat_bubble_outline, Icons.chat_bubble_rounded, 'Chat', badge: _unreadChats),
                  _navItem(3, Icons.person_outline, Icons.person_rounded, 'Profile'),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _navItem(int idx, IconData outline, IconData filled, String label, {int badge = 0}) {
    final selected = _index == idx;
    final color = selected ? AppTheme.primary : AppTheme.textMuted;
    return InkWell(
      onTap: () => _goTo(idx),
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Badge(
              isLabelVisible: badge > 0,
              label: Text(badge > 99 ? '99+' : '$badge', style: const TextStyle(fontSize: 10)),
              child: Icon(selected ? filled : outline, color: color, size: 24),
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: GoogleFonts.outfit(
                fontSize: 11,
                color: color,
                fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
