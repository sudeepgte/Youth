import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class StorySection extends StatelessWidget {
  const StorySection({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16.0, horizontal: 16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _buildStoryItem(isUser: true, name: 'Your Story', imageUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150'),
              // Could add more stories here in a ListView if needed, but the image just shows 'Your Story'
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStoryItem({required bool isUser, required String name, required String imageUrl}) {
    return Column(
      children: [
        Stack(
          children: [
            Container(
              padding: const EdgeInsets.all(2),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(color: Colors.blueAccent, width: 2),
              ),
              child: CircleAvatar(
                radius: 28,
                backgroundImage: NetworkImage(imageUrl),
              ),
            ),
            if (isUser)
              Positioned(
                bottom: 0,
                right: 0,
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.blueAccent,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 2),
                  ),
                  child: const Icon(Icons.add, color: Colors.white, size: 14),
                ),
              ),
          ],
        ),
        const SizedBox(height: 6),
        Text(
          name,
          style: GoogleFonts.inter(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: const Color(0xFF1E293B),
          ),
        ),
      ],
    );
  }
}
