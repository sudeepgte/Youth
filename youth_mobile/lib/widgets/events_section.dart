import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:cached_network_image/cached_network_image.dart';

class EventsSection extends StatelessWidget {
  const EventsSection({super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Featured Events',
                      style: GoogleFonts.outfit(
                        fontSize: 32,
                        fontWeight: FontWeight.w900,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Created by our community and administrators.',
                      style: GoogleFonts.inter(
                        color: Colors.white70,
                        fontSize: 14,
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                'Explore All >',
                style: GoogleFonts.inter(
                  color: Colors.blueAccent,
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            clipBehavior: Clip.none,
            child: Row(
              children: [
                SizedBox(
                  width: 320,
                  child: _buildEventCard(
                    imageUrl: 'https://images.unsplash.com/photo-1511512578047-dfb367046420?w=800&auto=format&fit=crop',
                    category: 'Gaming',
                    title: 'ZentrixTestEvent',
                    date: 'Fri, July 31, 2026',
                    time: '06:17 PM',
                    location: 'btm',
                    organizer: 'By Zentrix Admin',
                    description: 'qwer',
                    price: '₹1000',
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 320,
                  child: _buildEventCard(
                    imageUrl: 'https://youth.chethancodehub.com/uploads/109d07c2-a7ed-445d-80d6-2dfe095a046a.jpg',
                    category: 'Gaming',
                    title: 'cod',
                    date: 'Fri, July 31, 2026',
                    time: '12:30 PM',
                    location: 'Location TBA',
                    organizer: 'By Zentrix Admin',
                    description: 'dare',
                    price: '₹500',
                  ),
                ),
                const SizedBox(width: 16),
                SizedBox(
                  width: 320,
                  child: _buildEventCard(
                    imageUrl: 'https://youth.chethancodehub.com/uploads/1e9a7767-7351-415e-a13b-26c61131875e.png',
                    category: 'Talent',
                    title: 'india\'s got latent',
                    date: 'Fri, July 17, 2026',
                    time: '03:30 PM',
                    location: 'Bengaluru',
                    organizer: 'By Zentrix Admin',
                    description: 'samay raina,aasif khan,prakash jadhav',
                    price: '₹4500',
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEventCard({
    required String imageUrl,
    required String category,
    required String title,
    required String date,
    required String time,
    required String location,
    required String organizer,
    required String description,
    required String price,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.black.withOpacity(0.06)),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Stack(
            children: [
              CachedNetworkImage(
                imageUrl: imageUrl,
                height: 150,
                width: double.infinity,
                fit: BoxFit.cover,
                placeholder: (context, url) => Container(color: Colors.grey[200]),
                errorWidget: (context, url, error) => Container(color: Colors.grey[200], child: const Icon(Icons.error, color: Colors.grey)),
              ),
              Positioned(
                top: 12,
                left: 12,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(100),
                  ),
                  child: Text(
                    category.toUpperCase(),
                    style: GoogleFonts.inter(
                      color: Colors.blueAccent,
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0.5,
                    ),
                  ),
                ),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: GoogleFonts.inter(
                    color: Colors.black,
                    fontSize: 19,
                    fontWeight: FontWeight.w900,
                    height: 1.2,
                  ),
                ),
                const SizedBox(height: 12),
                _buildInfoRow(FontAwesomeIcons.calendar, date),
                const SizedBox(height: 8),
                _buildInfoRow(FontAwesomeIcons.clock, time),
                const SizedBox(height: 8),
                _buildInfoRow(FontAwesomeIcons.locationDot, location),
                const SizedBox(height: 8),
                _buildInfoRow(FontAwesomeIcons.userTie, organizer),
                const SizedBox(height: 16),
                Text(
                  description,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: GoogleFonts.inter(
                    color: Colors.grey[700],
                    fontSize: 13,
                    height: 1.6,
                  ),
                ),
                const SizedBox(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      price,
                      style: GoogleFonts.inter(
                        color: Colors.black,
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    ElevatedButton.icon(
                      onPressed: () {},
                      icon: const FaIcon(FontAwesomeIcons.ticket, size: 14),
                      label: Text(
                        'Book Now',
                        style: GoogleFonts.inter(
                          fontWeight: FontWeight.w800,
                          fontSize: 13,
                        ),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.blueAccent,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        elevation: 4,
                        shadowColor: Colors.blueAccent.withOpacity(0.5),
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

  Widget _buildInfoRow(dynamic icon, String text) {
    return Row(
      children: [
        FaIcon(icon, color: Colors.blueAccent, size: 13),
        const SizedBox(width: 10),
        Text(
          text,
          style: GoogleFonts.inter(
            color: Colors.grey[700],
            fontSize: 12,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }
}
