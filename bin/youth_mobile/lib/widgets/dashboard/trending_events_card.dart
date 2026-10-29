import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class TrendingEventsCard extends StatelessWidget {
  const TrendingEventsCard({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
      padding: const EdgeInsets.all(20.0),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Trending Events',
                style: GoogleFonts.inter(fontSize: 16, fontWeight: FontWeight.bold, color: const Color(0xFF0F172A)),
              ),
              Text(
                'See All',
                style: GoogleFonts.inter(fontSize: 14, color: Colors.blueAccent, fontWeight: FontWeight.w600),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _buildEventItem('Neon Lights Fest', 'AUG 24', 'https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=150'),
          const SizedBox(height: 16),
          _buildEventItem('Tech Summit 24', 'SEP 10', 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=150'),
        ],
      ),
    );
  }

  Widget _buildEventItem(String title, String date, String imageUrl) {
    return Row(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Image.network(imageUrl, width: 48, height: 48, fit: BoxFit.cover),
        ),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: GoogleFonts.inter(fontSize: 14, fontWeight: FontWeight.bold, color: const Color(0xFF0F172A)),
            ),
            const SizedBox(height: 4),
            Text(
              date,
              style: GoogleFonts.inter(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.cyan),
            ),
          ],
        ),
      ],
    );
  }
}
