import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class FeaturesSection extends StatelessWidget {
  const FeaturesSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Platform Features',
            style: GoogleFonts.outfit(
              fontSize: 32,
              fontWeight: FontWeight.w900,
              color: Colors.white,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Everything you need to thrive in the Zentrix community.',
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
                  width: 320,
                  child: _buildFeatureCard(
                    imageUrl: 'https://images.openai.com/static-rsc-4/8f5hqzs1t1WYBdG1_Qh2J9hf4rpwZcjV-2dsnlHCqPRQ8CpeB9QCyGYLUWrlalbsvZBcMYNixaLW2VVWxtJtBy9FhP7UPyuxdYr8lb70QnGxC2oYVM9KjX7QKJIDyBzbjfWS4C5we5XcDwd78boKd75QItf8oIagKepnamSDpLQdxWlUGSeypNfPeksqQEMr?purpose=fullsize',
                    icon: FontAwesomeIcons.solidComments,
                    iconColor: Colors.blueAccent,
                    tag: 'Social',
                    title: 'Real-time Chat',
                    description: 'Connect instantly with friends and groups. Pulse-synced, secure, and always active.',
                    actionText: 'Open Messages',
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 320,
                  child: _buildFeatureCard(
                    imageUrl: 'https://images.openai.com/static-rsc-4/I2yVlMtcIKk4BAdQBKnkmCJA7yxYhCYrHQDtMyN4d_00YE_Y8iOpmV0e6U1R_OcMcj5LGKASruMRDgrU08RQosTJamwcjVFPlYTilt3HdW0DHphHwOtCqZkkrkJCgpjRpg6TM9utFk_FkM6t3k7stdwhXXQhEBGuINrrGCNF5igWAXCqv_2zAW3iOgK66lLC?purpose=fullsize',
                    icon: FontAwesomeIcons.rss,
                    iconColor: Colors.purpleAccent,
                    tag: 'Community',
                    title: 'Discovery Feed',
                    description: 'Explore trending stories, campus news, and community milestones.',
                    actionText: 'Explore Feed',
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 320,
                  child: _buildFeatureCard(
                    imageUrl: 'https://images.openai.com/static-rsc-4/9AJw0q6urbrbCbdwHdnLA3z9FSezNyKiYI8Bn8e9y1pBjquKHxfTgILbCGcizmMG7mv3PenxlbZcLxOEeb0a655_E6DK_k39UGJiWNXjdUNgzkdKYQtVJao8kkbyuFFQ3QZoikkwwpMNT2_U35MCUPab9akPduI56kgPglW2t-PmlXf7Kv9D_t18fUxw67TH?purpose=fullsize',
                    icon: FontAwesomeIcons.clapperboard,
                    iconColor: Colors.pinkAccent,
                    tag: 'Entertainment',
                    title: 'Reels',
                    description: 'Watch and capture short-form cinematic moments.',
                    actionText: 'Watch Now',
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFeatureCard({
    required String imageUrl,
    required dynamic icon,
    required Color iconColor,
    required String tag,
    required String title,
    required String description,
    required String actionText,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF1E1B2E),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CachedNetworkImage(
            imageUrl: imageUrl,
            height: 200,
            width: double.infinity,
            fit: BoxFit.cover,
            placeholder: (context, url) => Container(color: Colors.white10),
            errorWidget: (context, url, error) => Container(color: Colors.white10, child: const Icon(Icons.error)),
          ),
          Padding(
            padding: const EdgeInsets.all(24.0),
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
                      FaIcon(icon, color: iconColor, size: 12),
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
                const SizedBox(height: 12),
                Text(
                  title,
                  style: GoogleFonts.inter(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  description,
                  style: GoogleFonts.inter(
                    color: Colors.white70,
                    fontSize: 13,
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    FaIcon(FontAwesomeIcons.circleArrowRight, color: iconColor, size: 16),
                    const SizedBox(width: 8),
                    Text(
                      actionText,
                      style: GoogleFonts.inter(
                        color: iconColor,
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
