import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class FeedSection extends StatelessWidget {
  const FeedSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: Colors.grey.withOpacity(0.2)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.filter_list, size: 16, color: Colors.black87),
                const SizedBox(width: 8),
                Text('Filter', style: GoogleFonts.inter(fontSize: 14, fontWeight: FontWeight.w600)),
                const Icon(Icons.keyboard_arrow_down, size: 16),
              ],
            ),
          ),
        ),
        
        // Post Card
        Container(
          margin: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
          padding: const EdgeInsets.all(16.0),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const CircleAvatar(
                    radius: 20,
                    backgroundImage: NetworkImage('https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150'),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Aasif Khan', style: GoogleFonts.inter(fontWeight: FontWeight.bold, fontSize: 14)),
                        const SizedBox(height: 2),
                        Row(
                          children: [
                            Text('02-07-2026 10:15', style: GoogleFonts.inter(color: Colors.grey, fontSize: 11)),
                            const SizedBox(width: 8),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: Colors.pinkAccent.withOpacity(0.1),
                                borderRadius: BorderRadius.circular(10),
                                border: Border.all(color: Colors.pinkAccent.withOpacity(0.5)),
                              ),
                              child: Text('REEL', style: GoogleFonts.inter(color: Colors.pinkAccent, fontSize: 9, fontWeight: FontWeight.bold)),
                            ),
                            const SizedBox(width: 8),
                            Text('Vlog', style: GoogleFonts.inter(color: Colors.grey, fontSize: 11)),
                          ],
                        )
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              // Dummy Video Player representation
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  height: 200,
                  width: double.infinity,
                  color: Colors.black,
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      Image.network('https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=800', fit: BoxFit.cover, opacity: const AlwaysStoppedAnimation(0.7)),
                      Positioned(
                        bottom: 8,
                        left: 8,
                        right: 8,
                        child: Row(
                          children: [
                            const Icon(Icons.play_arrow, color: Colors.white, size: 20),
                            const SizedBox(width: 8),
                            Text('0:00 / 0:21', style: GoogleFonts.inter(color: Colors.white, fontSize: 12)),
                            const Spacer(),
                            const Icon(Icons.volume_up, color: Colors.white, size: 16),
                            const SizedBox(width: 12),
                            const Icon(Icons.fullscreen, color: Colors.white, size: 16),
                            const SizedBox(width: 12),
                            const Icon(Icons.more_vert, color: Colors.white, size: 16),
                          ],
                        ),
                      ),
                      Positioned(
                        bottom: 4,
                        left: 8,
                        right: 8,
                        child: Container(
                          height: 3,
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.3),
                            borderRadius: BorderRadius.circular(2),
                          ),
                          child: FractionallySizedBox(
                            alignment: Alignment.centerLeft,
                            widthFactor: 0.3, // progress
                            child: Container(color: Colors.white),
                          ),
                        ),
                      )
                    ],
                  ),
                ),
              ),
            ],
          ),
        )
      ],
    );
  }
}
