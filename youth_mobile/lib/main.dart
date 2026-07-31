import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

// Sections
import 'widgets/hero_section.dart';
import 'widgets/stats_section.dart';
import 'widgets/features_section.dart';
import 'widgets/events_section.dart';
import 'widgets/categories_section.dart';
import 'widgets/testimonials_section.dart';
import 'widgets/about_section.dart';
import 'widgets/organizers_section.dart';
import 'widgets/contact_section.dart';
import 'widgets/faq_section.dart';
import 'widgets/footer_section.dart';

// Screens
import 'screens/login_page.dart';
import 'screens/register_page.dart';

void main() {
  runApp(const YouthApp());
}

class YouthApp extends StatelessWidget {
  const YouthApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Zentrix Youth',
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF0F172A),
        colorScheme: const ColorScheme.dark(
          primary: Color(0xFF3B82F6),
          secondary: Color(0xFF22D3EE),
        ),
        textTheme: GoogleFonts.interTextTheme(ThemeData.dark().textTheme),
        useMaterial3: true,
      ),
      home: const LandingPage(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class LandingPage extends StatelessWidget {
  const LandingPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Row(
          children: [
            const Icon(Icons.bolt, color: Color(0xFF22D3EE)),
            const SizedBox(width: 8),
            Text(
              'ZENTRIX',
              style: GoogleFonts.outfit(
                fontWeight: FontWeight.w900,
                fontSize: 20,
                letterSpacing: 1.5,
              ),
            ),
          ],
        ),
        actions: [
          Builder(
            builder: (context) => IconButton(
              icon: const Icon(Icons.menu),
              onPressed: () {
                Scaffold.of(context).openEndDrawer();
              },
            ),
          ),
        ],
      ),
      endDrawer: Drawer(
        backgroundColor: const Color(0xFF0F172A),
        child: SafeArea(
          child: Column(
            children: [
              Align(
                alignment: Alignment.topRight,
                child: IconButton(
                  icon: const Icon(Icons.close, color: Colors.white54),
                  onPressed: () => Navigator.pop(context),
                ),
              ),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
                  children: [
                    _buildDrawerItem('Home'),
                    const Divider(color: Colors.white10, height: 32),
                    _buildDrawerItem('Events'),
                    const Divider(color: Colors.white10, height: 32),
                    _buildDrawerItem('Games'),
                    const Divider(color: Colors.white10, height: 32),
                    _buildDrawerItem('About Us'),
                    const Divider(color: Colors.white10, height: 32),
                    _buildDrawerItem('Careers'),
                    const Divider(color: Colors.white10, height: 32),
                    _buildDrawerItem('Contact'),
                    const Divider(color: Colors.white10, height: 32),
                    _buildDrawerItem('FAQs'),
                    const Divider(color: Colors.white10, height: 32),
                    _buildDrawerItem('Heat Map'),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  children: [
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton(
                        onPressed: () {
                          Navigator.pop(context); // close drawer
                          Navigator.push(
                            context,
                            MaterialPageRoute(builder: (context) => const LoginPage()),
                          );
                        },
                        style: OutlinedButton.styleFrom(
                          side: BorderSide(color: Colors.white.withOpacity(0.2)),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: Text(
                          'Login',
                          style: GoogleFonts.inter(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: () {
                          Navigator.pop(context); // close drawer
                          Navigator.push(
                            context,
                            MaterialPageRoute(builder: (context) => const RegisterPage()),
                          );
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.blueAccent, // Use gradient in future if needed
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        child: Text(
                          'Register',
                          style: GoogleFonts.inter(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            const HeroSection(),
            Container(
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [Color(0xFF0F172A), Color(0xFF1E1B2E)],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
              ),
              child: const Column(
                children: [
                  StatsSection(),
                  FeaturesSection(),
                  EventsSection(),
                  CategoriesSection(),
                  TestimonialsSection(),
                  AboutSection(),
                  OrganizersSection(),
                  ContactSection(),
                  FaqSection(),
                  FooterSection(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDrawerItem(String title) {
    return Text(
      title,
      style: GoogleFonts.inter(
        color: Colors.white,
        fontSize: 16,
        fontWeight: FontWeight.bold,
      ),
    );
  }
}
