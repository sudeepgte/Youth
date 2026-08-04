import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../screens/achievements/achievements_page.dart';
import '../screens/battles/battles_page.dart';
import '../screens/events/bookings_page.dart';
import '../screens/games/games_page.dart';
import '../screens/heatmap/heatmap_page.dart';
import '../screens/music/music_page.dart';
import '../screens/profile/create_post_page.dart';
import '../screens/reels/reels_page.dart';
import '../screens/rewards/rewards_page.dart';
import '../screens/shop/shop_page.dart';
import '../screens/social/requests_page.dart';
import '../screens/wallet/wallet_page.dart';

typedef ShellNavigate = void Function(int index);

class AppDrawer extends StatelessWidget {
  const AppDrawer({
    super.key,
    required this.onNavigate,
    this.activeIndex = 0,
  });

  final ShellNavigate onNavigate;
  final int activeIndex;

  @override
  Widget build(BuildContext context) {
    final username = context.watch<AuthProvider>().user?.username ?? '';

    return Drawer(
      backgroundColor: Colors.white,
      child: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(vertical: 8),
          children: [
            UserAccountsDrawerHeader(
              decoration: const BoxDecoration(color: Color(0xFFF0F4F8)),
              accountName: Text(
                username,
                style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.black87),
              ),
              accountEmail: Text(
                'Zentrix Youth',
                style: GoogleFonts.inter(color: Colors.black54, fontSize: 13),
              ),
              currentAccountPicture: CircleAvatar(
                backgroundColor: Colors.blueAccent,
                child: Text(
                  username.isNotEmpty ? username[0].toUpperCase() : '?',
                  style: GoogleFonts.outfit(fontWeight: FontWeight.bold, color: Colors.white),
                ),
              ),
            ),
            _item(context, Icons.home, 'Home', () => onNavigate(0), activeIndex == 0),
            _item(context, Icons.explore, 'Explore', () => onNavigate(1), activeIndex == 1),
            _item(context, Icons.event, 'Events', () => onNavigate(2), activeIndex == 2),
            _item(context, Icons.sports_martial_arts, 'Battles', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const BattlesPage()));
            }, false),
            _item(context, Icons.emoji_events, 'Achievements', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const AchievementsPage()));
            }, false),
            _item(context, Icons.chat, 'Chat', () => onNavigate(3), activeIndex == 3),
            _item(context, Icons.mark_email_unread, 'Requests', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const RequestsPage()));
            }, false),
            _item(context, Icons.movie, 'Reels', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const ReelsPage()));
            }, false),
            _item(context, Icons.person, 'Profile', () => onNavigate(4), activeIndex == 4),
            _item(context, Icons.videogame_asset, 'Games', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const GamesPage()));
            }, false),
            _item(context, Icons.music_note, 'Music', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const MusicPage()));
            }, false),
            _item(context, Icons.account_balance_wallet, 'Wallet', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const WalletPage()));
            }, false),
            _item(context, Icons.shopping_bag, 'Shop', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const ShopPage()));
            }, false),
            _item(context, Icons.confirmation_number, 'Bookings', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const BookingsPage()));
            }, false),
            _item(context, Icons.add_box, 'Create Post', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const CreatePostPage()));
            }, false),
            _item(context, Icons.card_giftcard, 'Rewards', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const RewardsPage()));
            }, false),
            _item(context, Icons.map, 'Heat Map', () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => const HeatmapPage()));
            }, false),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.logout, color: Colors.red),
              title: Text('Logout', style: GoogleFonts.inter(fontWeight: FontWeight.w600)),
              onTap: () async {
                Navigator.pop(context);
                await context.read<AuthProvider>().logout();
                if (!context.mounted) return;
                Navigator.of(context).popUntil((route) => route.isFirst);
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _item(
    BuildContext context,
    IconData icon,
    String label,
    VoidCallback onTap,
    bool selected,
  ) {
    return ListTile(
      leading: Icon(icon, color: selected ? Colors.blueAccent : Colors.black54),
      title: Text(
        label,
        style: GoogleFonts.inter(
          fontWeight: selected ? FontWeight.bold : FontWeight.w500,
          color: selected ? Colors.blueAccent : Colors.black87,
        ),
      ),
      selected: selected,
      onTap: () {
        Navigator.pop(context);
        onTap();
      },
    );
  }
}
