import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class SuggestedUsersCard extends StatelessWidget {
  const SuggestedUsersCard({super.key});

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
                'Suggested for you',
                style: GoogleFonts.inter(fontSize: 16, fontWeight: FontWeight.bold, color: const Color(0xFF0F172A)),
              ),
              Text(
                'See All',
                style: GoogleFonts.inter(fontSize: 14, color: Colors.blueAccent, fontWeight: FontWeight.w600),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _buildUserItem('Aasif Khan', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', hasImage: true),
          const SizedBox(height: 16),
          _buildUserItem('Prakruthi', 'PR', color: Colors.pinkAccent),
          const SizedBox(height: 16),
          _buildUserItem('Syed', 'SY', color: Colors.pinkAccent),
          const SizedBox(height: 16),
          _buildUserItem('azhar', 'AZ', color: Colors.redAccent),
          const SizedBox(height: 16),
          _buildUserItem('anwar', 'AN', color: Colors.redAccent),
        ],
      ),
    );
  }

  Widget _buildUserItem(String name, String avatarData, {bool hasImage = false, Color color = Colors.grey}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Row(
          children: [
            CircleAvatar(
              radius: 16,
              backgroundColor: hasImage ? Colors.transparent : color,
              backgroundImage: hasImage ? NetworkImage(avatarData) : null,
              child: !hasImage 
                ? Text(avatarData, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold))
                : null,
            ),
            const SizedBox(width: 12),
            Text(
              name,
              style: GoogleFonts.inter(fontSize: 13, color: Colors.blueAccent, fontWeight: FontWeight.w500),
            ),
          ],
        ),
        Text(
          'Follow',
          style: GoogleFonts.inter(fontSize: 13, color: Colors.cyan, fontWeight: FontWeight.bold),
        ),
      ],
    );
  }
}
