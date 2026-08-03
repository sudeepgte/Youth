import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:cached_network_image/cached_network_image.dart';

class CategoriesSection extends StatelessWidget {
  const CategoriesSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Browse by Category',
            style: GoogleFonts.outfit(
              fontSize: 32,
              fontWeight: FontWeight.w900,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Find exactly what you\'re looking for with our curated interest groups.',
            style: GoogleFonts.inter(
              color: Colors.white70,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 24),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            clipBehavior: Clip.none,
            child: Row(
              children: [
                SizedBox(
                  width: 300,
                  child: _buildCategoryCard(
                    imageUrl: 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?q=80&w=800&auto=format&fit=crop',
                    title: 'Music',
                    description: 'Concerts, jam sessions, open mics, and music battles.',
                    meta: 'Live • Rooms • Playlists',
                    icon: FontAwesomeIcons.music,
                    color: Colors.pinkAccent,
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 300,
                  child: _buildCategoryCard(
                    imageUrl: 'https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=800&auto=format&fit=crop',
                    title: 'Tech',
                    description: 'Hack nights, workshops, talks, and creator meetups.',
                    meta: 'Build • Learn • Ship',
                    icon: FontAwesomeIcons.microchip,
                    color: Colors.blueAccent,
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 300,
                  child: _buildCategoryCard(
                    imageUrl: 'https://images.openai.com/static-rsc-4/7vJOijI2a89kUbkkKGPE1civqgvqnF-D_w13F4AHwlfKmMTxi-A_i4ejlU66hYVhgT1juw-DIWDZ3X3xgaBx-iGqApFRFkBKauFlli5PpyHYPDuAJjbH2xIPkzbFki3iYukgMsVDBSE5RXf9I1SzoD39VDXjb20YPoYhQ4B0Fp_joULvx53arGpUO164Ft6e?purpose=fullsize',
                    title: 'Comedy',
                    description: 'Standup shows, improv nights, and fun community gigs.',
                    meta: 'Laugh • Meet • Chill',
                    icon: FontAwesomeIcons.faceLaugh,
                    color: Colors.yellowAccent,
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 300,
                  child: _buildCategoryCard(
                    imageUrl: 'https://images.unsplash.com/photo-1521412644187-c49fa049e84d?q=80&w=800&auto=format&fit=crop',
                    title: 'Sports',
                    description: 'Tournaments, watch parties, and friendly challenges.',
                    meta: 'Compete • Team up',
                    icon: FontAwesomeIcons.trophy,
                    color: Colors.purpleAccent,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCategoryCard({
    required String imageUrl,
    required String title,
    required String description,
    required String meta,
    required dynamic icon,
    required Color color,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: color.withOpacity(0.05),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: color.withOpacity(0.2)),
      ),
      padding: const EdgeInsets.all(16.0),
      child: Column(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(20),
            child: CachedNetworkImage(
              imageUrl: imageUrl,
              height: 140,
              width: 140,
              fit: BoxFit.cover,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            title,
            style: GoogleFonts.inter(
              color: Colors.white,
              fontSize: 22,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            description,
            textAlign: TextAlign.center,
            style: GoogleFonts.inter(
              color: Colors.white70,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              FaIcon(icon, size: 14, color: color),
              const SizedBox(width: 8),
              Text(
                meta,
                style: GoogleFonts.inter(
                  color: color,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
