import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import '../chat/chat_list_page.dart';
import '../events/events_page.dart';
import '../explore/explore_page.dart';
import '../home/home_feed_page.dart';
import '../notifications/notifications_page.dart';
import '../profile/profile_page.dart';
import '../shop/shop_page.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _index = 0;

  late final List<Widget> _pages = [
    const HomeFeedPage(),
    const ExplorePage(),
    const EventsPage(),
    const ChatListPage(),
    const ProfilePage(),
  ];

  void _goTo(int index) => setState(() => _index = index);

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().user;

    return Theme(
      data: AppTheme.dashboardTheme(),
      child: Scaffold(
        backgroundColor: AppTheme.dashboardBg,
        appBar: AppBar(
          leading: Builder(
            builder: (ctx) => IconButton(
              icon: const Icon(Icons.menu, color: Colors.black87),
              onPressed: () => Scaffold.of(ctx).openDrawer(),
            ),
          ),
          title: Text('Zentrix', style: GoogleFonts.outfit(fontWeight: FontWeight.w900, color: Colors.blueAccent)),
          actions: [
            Container(
              margin: const EdgeInsets.symmetric(vertical: 12),
              padding: const EdgeInsets.symmetric(horizontal: 10),
              decoration: BoxDecoration(
                color: Colors.yellowAccent.withValues(alpha: 0.2),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  const Icon(Icons.monetization_on, color: Colors.amber, size: 16),
                  const SizedBox(width: 4),
                  Text(
                    '${user?.coins ?? 0}',
                    style: GoogleFonts.inter(color: Colors.amber, fontWeight: FontWeight.bold, fontSize: 12),
                  ),
                ],
              ),
            ),
            IconButton(
              icon: const Icon(Icons.shopping_bag, color: Colors.blueAccent, size: 22),
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const ShopPage()),
              ),
            ),
            IconButton(
              icon: const Icon(Icons.notifications, color: Colors.blueAccent, size: 22),
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const NotificationsPage()),
              ),
            ),
            const SizedBox(width: 8),
          ],
        ),
        drawer: AppDrawer(onNavigate: _goTo, activeIndex: _index),
        body: IndexedStack(index: _index, children: _pages),
        bottomNavigationBar: NavigationBar(
          selectedIndex: _index,
          onDestinationSelected: _goTo,
          destinations: const [
            NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
            NavigationDestination(icon: Icon(Icons.explore_outlined), selectedIcon: Icon(Icons.explore), label: 'Explore'),
            NavigationDestination(icon: Icon(Icons.event_outlined), selectedIcon: Icon(Icons.event), label: 'Events'),
            NavigationDestination(icon: Icon(Icons.chat_outlined), selectedIcon: Icon(Icons.chat), label: 'Chat'),
            NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profile'),
          ],
        ),
      ),
    );
  }
}
