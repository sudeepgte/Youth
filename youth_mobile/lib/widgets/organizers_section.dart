import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:cached_network_image/cached_network_image.dart';

class OrganizersSection extends StatelessWidget {
  const OrganizersSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 24.0),
      child: Container(
        padding: const EdgeInsets.all(24.0),
        decoration: BoxDecoration(
          color: const Color(0xFF1E1B2E),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: Colors.blueAccent.withOpacity(0.1)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.purpleAccent.withOpacity(0.15),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const FaIcon(FontAwesomeIcons.usersGear, color: Colors.purpleAccent, size: 12),
                  const SizedBox(width: 6),
                  Text(
                    'Partners',
                    style: GoogleFonts.inter(
                      color: Colors.purpleAccent,
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Become an Organizer',
              style: GoogleFonts.outfit(
                fontSize: 32,
                fontWeight: FontWeight.w900,
                color: Colors.white,
                height: 1.1,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Are you a campus leader or event creator? Zentrix provides the tools you need to reach thousands of students and manage events seamlessly.',
              style: GoogleFonts.inter(
                color: Colors.white70,
                fontSize: 15,
                height: 1.6,
              ),
            ),
            const SizedBox(height: 20),
            _buildCheckmark('Event Management'),
            const SizedBox(height: 10),
            _buildCheckmark('Digital Ticketing'),
            const SizedBox(height: 10),
            _buildCheckmark('Real-time Analytics'),
            const SizedBox(height: 10),
            _buildCheckmark('Engagement Tools'),
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
                  'Contact for Partnership',
                  style: GoogleFonts.inter(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),
            ClipRRect(
              borderRadius: BorderRadius.circular(24),
              child: CachedNetworkImage(
                imageUrl: 'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?q=80&w=800&auto=format&fit=crop',
                height: 220,
                width: double.infinity,
                fit: BoxFit.cover,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCheckmark(String text) {
    return Row(
      children: [
        const FaIcon(FontAwesomeIcons.circleCheck, color: Colors.blueAccent, size: 14),
        const SizedBox(width: 10),
        Text(
          text,
          style: GoogleFonts.inter(
            color: Colors.white,
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
        ),
      ],
    );
  }
}
