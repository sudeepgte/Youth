import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../screens/achievements/achievements_page.dart';
import '../screens/battles/battles_page.dart';
import '../screens/events/bookings_page.dart';
import '../screens/events/events_page.dart';
import '../screens/games/games_page.dart';
import '../screens/heatmap/heatmap_page.dart';
import '../screens/music/music_page.dart';
import '../screens/profile/create_post_page.dart';
import '../screens/reels/reels_page.dart';
import '../screens/rewards/rewards_page.dart';
import '../screens/shop/shop_page.dart';
import '../screens/social/requests_page.dart';
import '../screens/wallet/wallet_page.dart';
import '../theme/app_theme.dart';
import 'shell_nav.dart';

/// Matches web dashboard sidebar (`dashboard.html` + `.sidebar-link`).
enum AppDrawerItem {
  home,
  explore,
  events,
  battles,
  achievements,
  chat,
  reels,
  profile,
  games,
  music,
  wallet,
  shop,
  bookings,
  rewards,
  heatmap,
  requests,
  createPost,
  none,
}

typedef ShellNavigate = void Function(int index);

/// Shared slide-out menu — same look & order on Home, Games, and every dashboard.
class AppDrawer extends StatelessWidget {
  const AppDrawer({
    super.key,
    this.active = AppDrawerItem.home,
    this.onShellNavigate,
  });

  final AppDrawerItem active;

  /// When provided (MainShell), switches bottom-nav tabs without rebuilding the shell.
  final ShellNavigate? onShellNavigate;

  static AppDrawerItem fromShellIndex(int index) {
    switch (index) {
      case 1:
        return AppDrawerItem.explore;
      case 2:
        return AppDrawerItem.chat;
      case 3:
        return AppDrawerItem.profile;
      default:
        return AppDrawerItem.home;
    }
  }

  void _close(BuildContext context) {
    if (Navigator.of(context).canPop()) {
      Navigator.pop(context);
    }
  }

  /// Pop nested routes back to [MainShell], then switch tab.
  void _goShellTab(BuildContext context, int tab) {
    _close(context);
    Navigator.of(context).popUntil((route) => route.isFirst);
    if (onShellNavigate != null) {
      onShellNavigate!(tab);
    } else {
      ShellNav.requestTab(tab);
    }
  }

  void _openPage(BuildContext context, Widget page) {
    _close(context);
    Navigator.push(context, MaterialPageRoute(builder: (_) => page));
  }

  @override
  Widget build(BuildContext context) {
    return Drawer(
      backgroundColor: const Color(0xFFF0F9FF),
      width: MediaQuery.sizeOf(context).width * 0.78,
      child: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(12, 12, 12, 24),
          children: [
            _link(
              context,
              icon: Icons.home_rounded,
              label: 'Home',
              item: AppDrawerItem.home,
              onTap: () => _goShellTab(context, 0),
            ),
            _link(
              context,
              icon: Icons.explore_rounded,
              label: 'Explore',
              item: AppDrawerItem.explore,
              onTap: () => _goShellTab(context, 1),
            ),
            _link(
              context,
              icon: Icons.calendar_month_rounded,
              label: 'Events',
              item: AppDrawerItem.events,
              onTap: () {
                if (active == AppDrawerItem.events) {
                  _close(context);
                  return;
                }
                _openPage(context, const EventsPage());
              },
            ),
            _link(
              context,
              icon: Icons.local_fire_department_rounded,
              label: 'Battle Arena',
              item: AppDrawerItem.battles,
              onTap: () {
                if (active == AppDrawerItem.battles) {
                  _close(context);
                  return;
                }
                _openPage(context, const BattlesPage());
              },
            ),
            _link(
              context,
              icon: Icons.emoji_events_rounded,
              label: 'Achievements',
              item: AppDrawerItem.achievements,
              onTap: () {
                if (active == AppDrawerItem.achievements) {
                  _close(context);
                  return;
                }
                _openPage(context, const AchievementsPage());
              },
            ),
            _link(
              context,
              icon: Icons.chat_bubble_rounded,
              label: 'Chat',
              item: AppDrawerItem.chat,
              onTap: () => _goShellTab(context, 2),
            ),
            _link(
              context,
              icon: Icons.movie_rounded,
              label: 'Reels',
              item: AppDrawerItem.reels,
              onTap: () {
                if (active == AppDrawerItem.reels) {
                  _close(context);
                  return;
                }
                _openPage(context, const ReelsPage());
              },
            ),
            _link(
              context,
              icon: Icons.person_rounded,
              label: 'Profile',
              item: AppDrawerItem.profile,
              onTap: () => _goShellTab(context, 3),
            ),
            _link(
              context,
              icon: Icons.sports_esports_rounded,
              label: 'Games',
              item: AppDrawerItem.games,
              onTap: () {
                if (active == AppDrawerItem.games) {
                  _close(context);
                  return;
                }
                _openPage(context, const GamesPage());
              },
            ),
            _link(
              context,
              icon: Icons.library_music_rounded,
              label: 'Music',
              item: AppDrawerItem.music,
              onTap: () {
                if (active == AppDrawerItem.music) {
                  _close(context);
                  return;
                }
                _openPage(context, const MusicPage());
              },
            ),
            _link(
              context,
              icon: Icons.account_balance_wallet_rounded,
              label: 'Wallet',
              item: AppDrawerItem.wallet,
              onTap: () {
                if (active == AppDrawerItem.wallet) {
                  _close(context);
                  return;
                }
                _openPage(context, const WalletPage());
              },
            ),
            _link(
              context,
              icon: Icons.shopping_bag_rounded,
              label: 'Shop',
              item: AppDrawerItem.shop,
              onTap: () {
                if (active == AppDrawerItem.shop) {
                  _close(context);
                  return;
                }
                _openPage(context, const ShopPage());
              },
            ),
            _link(
              context,
              icon: Icons.confirmation_number_rounded,
              label: 'My Bookings',
              item: AppDrawerItem.bookings,
              onTap: () {
                if (active == AppDrawerItem.bookings) {
                  _close(context);
                  return;
                }
                _openPage(context, const BookingsPage());
              },
            ),
            _link(
              context,
              icon: Icons.card_giftcard_rounded,
              label: 'Rewards',
              item: AppDrawerItem.rewards,
              onTap: () {
                if (active == AppDrawerItem.rewards) {
                  _close(context);
                  return;
                }
                _openPage(context, const RewardsPage());
              },
            ),
            _link(
              context,
              icon: Icons.map_rounded,
              label: 'Heat Map',
              item: AppDrawerItem.heatmap,
              onTap: () {
                if (active == AppDrawerItem.heatmap) {
                  _close(context);
                  return;
                }
                _openPage(context, const HeatmapPage());
              },
            ),
            _link(
              context,
              icon: Icons.mark_email_unread_rounded,
              label: 'Requests',
              item: AppDrawerItem.requests,
              onTap: () {
                if (active == AppDrawerItem.requests) {
                  _close(context);
                  return;
                }
                _openPage(context, const RequestsPage());
              },
            ),
            _link(
              context,
              icon: Icons.add_circle_rounded,
              label: 'Create Post',
              item: AppDrawerItem.createPost,
              accentIcon: true,
              onTap: () => _openPage(context, const CreatePostPage()),
            ),
            _link(
              context,
              icon: Icons.logout_rounded,
              label: 'Logout',
              item: AppDrawerItem.none,
              onTap: () async {
                _close(context);
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

  Widget _link(
    BuildContext context, {
    required IconData icon,
    required String label,
    required AppDrawerItem item,
    required VoidCallback onTap,
    bool accentIcon = false,
  }) {
    final selected = active == item && item != AppDrawerItem.none;
    final Color iconColor;
    final Color textColor;
    if (selected) {
      iconColor = Colors.white;
      textColor = Colors.white;
    } else if (accentIcon) {
      iconColor = AppTheme.accent;
      textColor = AppTheme.textSecondary;
    } else {
      iconColor = AppTheme.textSecondary;
      textColor = AppTheme.textSecondary;
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 220),
            curve: Curves.easeOutCubic,
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(20),
              gradient: selected ? AppTheme.brandGradient : null,
              boxShadow: selected
                  ? [
                      BoxShadow(
                        color: AppTheme.primary.withValues(alpha: 0.35),
                        blurRadius: 16,
                        offset: const Offset(0, 6),
                      ),
                    ]
                  : null,
            ),
            child: Row(
              children: [
                Icon(icon, size: 20, color: iconColor),
                const SizedBox(width: 14),
                Expanded(
                  child: Text(
                    label,
                    style: GoogleFonts.outfit(
                      fontWeight: FontWeight.w600,
                      fontSize: 15,
                      color: textColor,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Scaffold with the shared Youthian slide menu (for Games, Events, etc.).
class DashboardScaffold extends StatelessWidget {
  const DashboardScaffold({
    super.key,
    required this.body,
    this.active = AppDrawerItem.none,
    this.title,
    this.actions,
    this.floatingActionButton,
    this.backgroundColor,
    this.extendBodyBehindAppBar = false,
    this.showBack = true,
  });

  final Widget body;
  final AppDrawerItem active;
  final String? title;
  final List<Widget>? actions;
  final Widget? floatingActionButton;
  final Color? backgroundColor;
  final bool extendBodyBehindAppBar;
  final bool showBack;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: backgroundColor ?? AppTheme.dashboardBg,
      extendBodyBehindAppBar: extendBodyBehindAppBar,
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
        title: title != null
            ? Text(title!, style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: AppTheme.textPrimary))
            : Image.asset('assets/images/youthian_logo.png', height: 34),
        actions: [
          if (showBack)
            IconButton(
              tooltip: 'Back',
              icon: const Icon(Icons.arrow_back_rounded, color: AppTheme.textPrimary),
              onPressed: () => Navigator.maybePop(context),
            ),
          ...?actions,
        ],
      ),
      drawer: AppDrawer(active: active),
      floatingActionButton: floatingActionButton,
      body: body,
    );
  }
}
