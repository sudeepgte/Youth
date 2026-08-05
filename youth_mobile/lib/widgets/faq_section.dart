import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class FaqSection extends StatefulWidget {
  const FaqSection({super.key});

  @override
  State<FaqSection> createState() => _FaqSectionState();
}

class _FaqSectionState extends State<FaqSection> {
  int? expandedIndex;

  void togglePanel(int index) {
    setState(() {
      if (expandedIndex == index) {
        expandedIndex = null;
      } else {
        expandedIndex = index;
      }
    });
  }

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
              color: Colors.purpleAccent.withOpacity(0.15),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const FaIcon(FontAwesomeIcons.circleQuestion, color: Colors.purpleAccent, size: 12),
                const SizedBox(width: 6),
                Text(
                  'FAQ',
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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Frequently Asked\nQuestions',
                style: GoogleFonts.outfit(
                  fontSize: 28,
                  fontWeight: FontWeight.w900,
                  color: Colors.white,
                  height: 1.1,
                ),
              ),
              Text(
                'View All >',
                style: GoogleFonts.inter(
                  color: Colors.blueAccent,
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            'Quick answers to common questions about the Youthian platform.',
            style: GoogleFonts.inter(
              color: Colors.white70,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 24),
          _buildFaqItem(
            index: 0,
            question: 'What is Youthian?',
            answer: 'Youthian is a comprehensive community platform for students and creators. It offers real-time chat, event management, social features like reels and posts, and a variety of online games to keep the community engaged.',
          ),
          _buildFaqItem(
            index: 1,
            question: 'How do I earn Youthian Coins?',
            answer: 'You can earn Youthian Coins by daily login, participating in events, voting on polls, and engaging with the community. These coins can be spent in the Reward Shop to unlock exclusive features and badges.',
          ),
          _buildFaqItem(
            index: 2,
            question: 'How can I register for an event?',
            answer: 'Navigate to the Events page, find an event you\'re interested in, and click "Book Now". Follow the registration steps, and you\'ll receive a digital ticket once confirmed.',
          ),
        ],
      ),
    );
  }

  Widget _buildFaqItem({required int index, required String question, required String answer}) {
    bool isExpanded = expandedIndex == index;
    
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: const Color(0xFF1E1B2E),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
      ),
      child: Theme(
        data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
        child: ExpansionTile(
          initiallyExpanded: isExpanded,
          onExpansionChanged: (expanded) {
            togglePanel(index);
          },
          title: Text(
            question,
            style: GoogleFonts.inter(
              fontWeight: FontWeight.w800,
              fontSize: 15,
              color: Colors.white,
            ),
          ),
          trailing: FaIcon(
            isExpanded ? FontAwesomeIcons.chevronUp : FontAwesomeIcons.chevronDown,
            size: 14,
            color: Colors.white54,
          ),
          children: [
            Padding(
              padding: const EdgeInsets.only(left: 16.0, right: 16.0, bottom: 16.0),
              child: Text(
                answer,
                style: GoogleFonts.inter(
                  color: Colors.white70,
                  fontSize: 14,
                  height: 1.5,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
