import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class ShareThoughtCard extends StatelessWidget {
  const ShareThoughtCard({super.key});

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
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(colors: [Colors.lightBlueAccent, Colors.blueAccent]),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Center(
                  child: Icon(Icons.format_quote, color: Colors.white, size: 28),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Share Your Thought',
                      style: GoogleFonts.inter(fontSize: 16, fontWeight: FontWeight.bold, color: const Color(0xFF0F172A)),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Your words will appear on the public home page for everyone to see!',
                      style: GoogleFonts.inter(fontSize: 12, color: Colors.grey, height: 1.4),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ElevatedButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.send, size: 16),
            label: const Text('Share Thought'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.lightBlueAccent,
              foregroundColor: Colors.white,
              elevation: 0,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              textStyle: GoogleFonts.inter(fontWeight: FontWeight.bold),
            ),
          )
        ],
      ),
    );
  }
}
