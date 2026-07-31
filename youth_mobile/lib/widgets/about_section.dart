import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:cached_network_image/cached_network_image.dart';

class AboutSection extends StatelessWidget {
  const AboutSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.blueAccent.withOpacity(0.15),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const FaIcon(FontAwesomeIcons.circleInfo, color: Colors.blueAccent, size: 12),
                const SizedBox(width: 6),
                Text(
                  'About Us',
                  style: GoogleFonts.inter(
                    color: Colors.blueAccent,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'Providing the best experiences.',
            style: GoogleFonts.outfit(
              fontSize: 32,
              fontWeight: FontWeight.w900,
              color: Colors.white,
              height: 1.1,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'Zentrix is a modern, feature-rich platform designed to bring people together through communication, entertainment, and interactive experiences.',
            style: GoogleFonts.inter(
              color: Colors.white70,
              fontSize: 15,
              height: 1.6,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            'Our mission is to build a dynamic platform that enhances digital interaction while maintaining simplicity and innovation.',
            style: GoogleFonts.inter(
              color: Colors.white70,
              fontSize: 15,
              height: 1.6,
            ),
          ),
          const SizedBox(height: 24),
          ClipRRect(
            borderRadius: BorderRadius.circular(24),
            child: CachedNetworkImage(
              imageUrl: 'https://images.openai.com/static-rsc-4/QsTjw6zTxJImq5FwCkYrx7APzbFBLizzIdysGwAuR8F8ku5xuaOawqLZpZfox0gO6MK4qc_Ts7L3kWrnEk5-2MkIdL5i5n_gqDZPaJK8OoLSl_5yFzFCOhKrP5ztM3ROpx7O5TJ5jknmYvbBsnL2etTI4eCyDQLePlvOkWv4A8S7XK8I52uQGIotSlS-RadM?purpose=fullsize',
              height: 250,
              width: double.infinity,
              fit: BoxFit.cover,
            ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () {},
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blueAccent,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
              child: Text(
                'Join Zentrix Now',
                style: GoogleFonts.inter(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
