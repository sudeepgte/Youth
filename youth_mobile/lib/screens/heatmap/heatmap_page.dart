import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class HeatmapPage extends StatefulWidget {
  const HeatmapPage({super.key});

  @override
  State<HeatmapPage> createState() => _HeatmapPageState();
}

class _HeatmapPageState extends State<HeatmapPage> {
  List<Map<String, dynamic>> _events = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final events = await AppApi.heatmapEvents();
      if (!mounted) return;
      setState(() {
        _events = events;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppTheme.extractError(e);
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Heat Map', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(_error!, style: const TextStyle(color: Colors.red)),
                      ElevatedButton(onPressed: _load, child: const Text('Retry')),
                    ],
                  ),
                )
              : _events.isEmpty
                  ? Center(child: Text('No heat map events', style: GoogleFonts.inter(color: Colors.grey)))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: _events.length,
                        itemBuilder: (context, index) {
                          final e = _events[index];
                          final lat = e['latitude'] ?? e['lat'];
                          final lng = e['longitude'] ?? e['lng'] ?? e['lon'];
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(
                                backgroundColor: Colors.red.withValues(alpha: 0.15),
                                child: const Icon(Icons.local_fire_department, color: Colors.red),
                              ),
                              title: Text(
                                e['title']?.toString() ?? e['name']?.toString() ?? 'Event',
                                style: GoogleFonts.outfit(fontWeight: FontWeight.bold),
                              ),
                              subtitle: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  if (e['venue'] != null) Text(e['venue'].toString()),
                                  if (e['category'] != null) Text('Category: ${e['category']}'),
                                  if (lat != null && lng != null) Text('📍 $lat, $lng'),
                                  if (e['dateTime'] != null) Text(e['dateTime'].toString()),
                                  if (e['heatLevel'] != null || e['intensity'] != null)
                                    Text('Heat: ${e['heatLevel'] ?? e['intensity']}'),
                                ],
                              ),
                              isThreeLine: true,
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
