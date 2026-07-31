import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../screens/dashboard_page.dart';

class SidebarDrawer extends StatelessWidget {
  final String activePage;

  const SidebarDrawer({super.key, required this.activePage});

  @override
  Widget build(BuildContext context) {
    return Drawer(
      backgroundColor: const Color(0xFFE5F6FF), // Light blue background
      child: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 16.0),
          children: [
            _buildDrawerItem(context, Icons.home, 'Home', 'home'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.explore, 'Explore', 'explore'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.event, 'Events', 'events'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.local_fire_department, 'Battle Arena', 'battles'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.emoji_events, 'Achievements', 'achievements'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.chat, 'Chat', 'chat'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.video_library, 'Reels', 'reels'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.person, 'Profile', 'profile'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.sports_esports, 'Games', 'games'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.account_balance_wallet, 'Wallet', 'wallet'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.book_online, 'My Bookings', 'bookings'),
            const SizedBox(height: 8),
            _buildDrawerItem(context, Icons.add_circle, 'Create Post', 'create_post', iconColor: Colors.cyan),
            const SizedBox(height: 32),
            _buildDrawerItem(context, Icons.logout, 'Logout', 'logout'),
          ],
        ),
      ),
    );
  }

  Widget _buildDrawerItem(BuildContext context, IconData icon, String title, String id, {Color? iconColor}) {
    bool isSelected = activePage == id;
    return Container(
      decoration: isSelected
          ? BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF4361EE), Color(0xFF4CC9F0)],
                begin: Alignment.centerLeft,
                end: Alignment.centerRight,
              ),
              borderRadius: BorderRadius.circular(24),
            )
          : null,
      child: ListTile(
        leading: Icon(
          icon,
          color: isSelected ? Colors.white : (iconColor ?? const Color(0xFF334155)),
        ),
        title: Text(
          title,
          style: GoogleFonts.inter(
            color: isSelected ? Colors.white : const Color(0xFF334155),
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(24),
        ),
        onTap: () {
          Navigator.pop(context); // Close drawer
          if (id == 'logout') {
             // Handle logout here and pop to login/landing
             Navigator.of(context).popUntil((route) => route.isFirst);
             return;
          }
          if (id == 'home' && activePage != 'home') {
             Navigator.pushReplacement(
                context,
                MaterialPageRoute(builder: (context) => const DashboardPage()),
             );
          } else if (activePage != id) {
             // For unimplemented pages, just show a snackbar or push a placeholder
             ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('$title page coming soon!')),
             );
          }
        },
      ),
    );
  }
}
