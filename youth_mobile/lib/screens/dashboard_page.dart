import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../widgets/dashboard/story_section.dart';
import '../widgets/dashboard/trending_events_card.dart';
import '../widgets/dashboard/suggested_users_card.dart';
import '../widgets/dashboard/vote_card.dart';
import '../widgets/dashboard/share_thought_card.dart';
import '../widgets/dashboard/feed_section.dart';
import '../widgets/sidebar_drawer.dart';

class DashboardPage extends StatelessWidget {
  const DashboardPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF0F4F8), // Light blue-grey background
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: Builder(
          builder: (context) => IconButton(
            icon: const Icon(Icons.menu, color: Colors.black87),
            onPressed: () {
              Scaffold.of(context).openDrawer(); 
            },
          ),
        ),
        title: Text('Zentrix', style: GoogleFonts.outfit(fontWeight: FontWeight.w900, color: Colors.blueAccent)),
        actions: [
          Container(
            margin: const EdgeInsets.symmetric(vertical: 12),
            padding: const EdgeInsets.symmetric(horizontal: 8),
            decoration: BoxDecoration(
              color: Colors.yellowAccent.withOpacity(0.2),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                const Icon(Icons.monetization_on, color: Colors.amber, size: 16),
                const SizedBox(width: 4),
                Text('60', style: GoogleFonts.inter(color: Colors.amber, fontWeight: FontWeight.bold, fontSize: 12)),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.shopping_bag, color: Colors.blueAccent, size: 20),
            onPressed: () {},
          ),
          IconButton(
            icon: const Icon(Icons.notifications, color: Colors.blueAccent, size: 20),
            onPressed: () {},
          ),
          const Padding(
            padding: EdgeInsets.only(right: 16.0, left: 8.0),
            child: CircleAvatar(
               radius: 14,
              backgroundImage: NetworkImage('https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150'),
            ),
          )
        ],
      ),
      drawer: const SidebarDrawer(activePage: 'home'),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            StorySection(),
            TrendingEventsCard(),
            SuggestedUsersCard(),
            VoteCard(),
            ShareThoughtCard(),
            FeedSection(),
            SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}
