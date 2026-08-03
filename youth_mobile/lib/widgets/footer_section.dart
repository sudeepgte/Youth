import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class FooterSection extends StatelessWidget {
  const FooterSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 40.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Zentrix',
            style: GoogleFonts.outfit(
              fontSize: 28,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF0F172A),
              letterSpacing: -0.5,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            'Everything you need, all in one place.\nDiscover events, play games, share music,\nand build connections.',
            style: GoogleFonts.inter(
              fontSize: 14,
              color: const Color(0xFF475569),
              height: 1.6,
            ),
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              const FaIcon(FontAwesomeIcons.phone, color: Colors.blueAccent, size: 14),
              const SizedBox(width: 12),
              Text(
                '+91 98765 43210',
                style: GoogleFonts.inter(
                  fontSize: 14,
                  color: const Color(0xFF475569),
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              const FaIcon(FontAwesomeIcons.envelope, color: Colors.blueAccent, size: 14),
              const SizedBox(width: 12),
              Text(
                'support@zentrix.com',
                style: GoogleFonts.inter(
                  fontSize: 14,
                  color: const Color(0xFF475569),
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
          const SizedBox(height: 40),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Navigation',
                      style: GoogleFonts.inter(
                        fontSize: 16,
                        fontWeight: FontWeight.w900,
                        color: const Color(0xFF0F172A),
                      ),
                    ),
                    const SizedBox(height: 16),
                    _buildFooterLink('About Us'),
                    _buildFooterLink('Careers'),
                    _buildFooterLink('Events'),
                    _buildFooterLink('Organizers'),
                    _buildFooterLink('Games Arena'),
                  ],
                ),
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Support',
                      style: GoogleFonts.inter(
                        fontSize: 16,
                        fontWeight: FontWeight.w900,
                        color: const Color(0xFF0F172A),
                      ),
                    ),
                    const SizedBox(height: 16),
                    _buildFooterLink('Contact Us'),
                    _buildFooterLink('FAQs'),
                    _buildFooterLink('Terms of Service'),
                    _buildFooterLink('Privacy Policy'),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 40),
          Text(
            'Connect With Us',
            style: GoogleFonts.inter(
              fontSize: 16,
              fontWeight: FontWeight.w900,
              color: const Color(0xFF0F172A),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            'Follow our socials for community updates, hackathon releases, and gaming fests!',
            style: GoogleFonts.inter(
              fontSize: 13,
              color: const Color(0xFF475569),
              height: 1.5,
            ),
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              _buildSocialIcon(FontAwesomeIcons.instagram, Colors.pinkAccent),
              const SizedBox(width: 12),
              _buildSocialIcon(FontAwesomeIcons.xTwitter, Colors.black),
              const SizedBox(width: 12),
              _buildSocialIcon(FontAwesomeIcons.facebookF, Colors.blue),
            ],
          ),
          const SizedBox(height: 60),
          Center(
            child: Text(
              '© 2026 Zentrix. All rights reserved.',
              style: GoogleFonts.inter(
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: const Color(0xFF475569),
              ),
            ),
          ),
          const SizedBox(height: 12),
          Center(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'Made with ',
                  style: GoogleFonts.inter(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF475569),
                  ),
                ),
                const FaIcon(FontAwesomeIcons.solidHeart, color: Colors.pinkAccent, size: 12),
                Text(
                  ' for youth globally.',
                  style: GoogleFonts.inter(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: const Color(0xFF475569),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFooterLink(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12.0),
      child: Text(
        text,
        style: GoogleFonts.inter(
          fontSize: 14,
          color: const Color(0xFF475569),
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }

  Widget _buildSocialIcon(dynamic icon, Color iconColor) {
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: Colors.blueAccent.withOpacity(0.05),
        shape: BoxShape.circle,
        border: Border.all(color: Colors.black.withOpacity(0.05)),
      ),
      child: Center(
        child: FaIcon(icon, color: iconColor, size: 18),
      ),
    );
  }
}
