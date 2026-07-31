import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:google_fonts/google_fonts.dart';

class StatsSection extends StatelessWidget {
  const StatsSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 24.0),
      child: Column(
        children: [
          _buildStatCard(
            icon: FontAwesomeIcons.solidCalendarDays,
            iconColor: Colors.purpleAccent,
            title: 'Find Events',
            description: 'Explore workshops, cultural fests, and local meetups tailored for you.',
            tag: 'Discovery',
          ),
          const SizedBox(height: 16),
          _buildStatCard(
            icon: FontAwesomeIcons.gamepad,
            iconColor: Colors.blueAccent,
            title: 'Play & Compete',
            description: 'Jump into quick-play games and climb the global leaderboards with friends.',
            tag: 'Gaming',
          ),
          const SizedBox(height: 16),
          _buildStatCard(
            icon: FontAwesomeIcons.headphones,
            iconColor: Colors.pinkAccent,
            title: 'Share Vibes',
            description: 'Join collaborative music rooms and discover new tracks with your squad.',
            tag: 'Music',
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard({
    required dynamic icon,
    required Color iconColor,
    required String title,
    required String description,
    required String tag,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: const Color(0xFF1E1B2E),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: iconColor.withOpacity(0.15),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                FaIcon(icon, color: iconColor, size: 14),
                const SizedBox(width: 6),
                Text(
                  tag,
                  style: GoogleFonts.inter(
                    color: iconColor,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Text(
            title,
            style: GoogleFonts.inter(
              color: Colors.white,
              fontSize: 24,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            description,
            style: GoogleFonts.inter(
              color: Colors.white70,
              fontSize: 14,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}
