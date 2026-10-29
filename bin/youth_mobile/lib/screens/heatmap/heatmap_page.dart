import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';

class HeatmapPage extends StatefulWidget {
  const HeatmapPage({super.key});

  @override
  State<HeatmapPage> createState() => _HeatmapPageState();
}

class _HeatmapPageState extends State<HeatmapPage> {
  static const _filters = [
    ('all', 'All'),
    ('live', 'Live'),
    ('today', 'Today'),
    ('tomorrow', 'Tomorrow'),
    ('week', 'Week'),
    ('month', 'Month'),
  ];

  static const _defaultCenter = LatLng(20.5937, 78.9629);

  String _filter = 'all';
  /// markers | heat | both
  String _layer = 'both';
  Map<String, dynamic> _stats = {};
  List<Map<String, dynamic>> _markersData = [];
  Set<Marker> _markers = {};
  Set<Circle> _circles = {};
  bool _loading = true;
  String? _error;
  GoogleMapController? _mapController;
  LatLng _center = _defaultCenter;
  double _zoom = 5;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _mapController?.dispose();
    super.dispose();
  }

  double? _toDouble(dynamic v) {
    if (v is num) return v.toDouble();
    if (v is String) return double.tryParse(v);
    return null;
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final payload = await AppApi.heatmapPayload(filter: _filter);
      if (!mounted) return;

      final markersRaw = (payload['markers'] as List? ?? [])
          .map((e) => Map<String, dynamic>.from(e as Map))
          .toList();
      final heatRaw = (payload['heatPoints'] as List? ?? []);
      final heatPoints = <List<num>>[];
      for (final p in heatRaw) {
        if (p is List && p.length >= 3) {
          final lat = _toDouble(p[0]);
          final lng = _toDouble(p[1]);
          final intensity = _toDouble(p[2]) ?? 1;
          if (lat != null && lng != null) heatPoints.add([lat, lng, intensity]);
        }
      }
      final stats = payload['stats'] is Map
          ? Map<String, dynamic>.from(payload['stats'] as Map)
          : <String, dynamic>{};

      final points = <LatLng>[];
      final markers = <Marker>{};
      final circles = <Circle>{};

      for (var i = 0; i < markersRaw.length; i++) {
        final e = markersRaw[i];
        final lat = _toDouble(e['lat'] ?? e['latitude']);
        final lng = _toDouble(e['lng'] ?? e['longitude'] ?? e['lon']);
        if (lat == null || lng == null) continue;
        final pos = LatLng(lat, lng);
        points.add(pos);
        final title = e['location']?.toString() ?? e['title']?.toString() ?? 'Event hotspot';
        final total = e['total']?.toString() ?? '';
        final live = e['live']?.toString();
        final snippet = [
          if (total.isNotEmpty) '$total events',
          if (live != null && live != '0') '$live live',
        ].join(' · ');

        markers.add(
          Marker(
            markerId: MarkerId('m_$i'),
            position: pos,
            infoWindow: InfoWindow(title: title, snippet: snippet.isEmpty ? null : snippet),
            icon: BitmapDescriptor.defaultMarkerWithHue(
              (live != null && live != '0') ? BitmapDescriptor.hueRed : BitmapDescriptor.hueAzure,
            ),
            onTap: () => _showMarkerSheet(e, title, pos),
          ),
        );
      }

      for (var i = 0; i < heatPoints.length; i++) {
        final p = heatPoints[i];
        final intensity = p[2].toDouble().clamp(1, 40);
        final radius = 400.0 + intensity * 180.0;
        final alpha = (0.18 + (intensity / 40) * 0.45).clamp(0.15, 0.65);
        circles.add(
          Circle(
            circleId: CircleId('h_$i'),
            center: LatLng(p[0].toDouble(), p[1].toDouble()),
            radius: radius,
            fillColor: const Color(0xFFEF4444).withValues(alpha: alpha),
            strokeColor: const Color(0xFFDC2626).withValues(alpha: 0.35),
            strokeWidth: 1,
          ),
        );
      }

      LatLng center = _defaultCenter;
      double zoom = 5;
      if (points.isNotEmpty) {
        final avgLat = points.map((p) => p.latitude).reduce((a, b) => a + b) / points.length;
        final avgLng = points.map((p) => p.longitude).reduce((a, b) => a + b) / points.length;
        center = LatLng(avgLat, avgLng);
        zoom = points.length == 1 ? 12 : 6.5;
      }

      setState(() {
        _stats = stats;
        _markersData = markersRaw;
        _markers = markers;
        _circles = circles;
        _center = center;
        _zoom = zoom;
        _loading = false;
      });

      await _mapController?.animateCamera(
        CameraUpdate.newCameraPosition(CameraPosition(target: center, zoom: zoom)),
      );
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = AppTheme.extractError(e);
        _loading = false;
      });
    }
  }

  void _onFilter(String value) {
    if (_filter == value) return;
    setState(() => _filter = value);
    _load();
  }

  void _showMarkerSheet(Map<String, dynamic> e, String title, LatLng pos) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
            const SizedBox(height: 8),
            if (e['total'] != null) Text('Events here: ${e['total']}', style: GoogleFonts.inter()),
            if (e['live'] != null) Text('Live: ${e['live']}', style: GoogleFonts.inter()),
            if (e['today'] != null) Text('Today: ${e['today']}', style: GoogleFonts.inter()),
            if (e['week'] != null) Text('This week: ${e['week']}', style: GoogleFonts.inter()),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.pop(ctx);
                  _mapController?.animateCamera(
                    CameraUpdate.newCameraPosition(CameraPosition(target: pos, zoom: 14)),
                  );
                },
                icon: const Icon(Icons.my_location),
                label: const Text('Zoom here'),
                style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _statChip(String label, dynamic value) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.blue.shade50),
      ),
      child: Column(
        children: [
          Text('${value ?? 0}', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, color: AppTheme.primary)),
          Text(label, style: GoogleFonts.inter(fontSize: 10, color: Colors.black45)),
        ],
      ),
    );
  }

  Set<Marker> get _visibleMarkers =>
      (_layer == 'markers' || _layer == 'both') ? _markers : {};

  Set<Circle> get _visibleCircles =>
      (_layer == 'heat' || _layer == 'both') ? _circles : {};

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      drawer: const AppDrawer(active: AppDrawerItem.heatmap),
      appBar: AppBar(
        title: Text('Heat Map', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        leading: Builder(
          builder: (ctx) => IconButton(
            icon: const Icon(Icons.menu, color: AppTheme.textPrimary),
            onPressed: () => Scaffold.of(ctx).openDrawer(),
          ),
        ),
        actions: [
          PopupMenuButton<String>(
            tooltip: 'Map layer',
            initialValue: _layer,
            onSelected: (v) => setState(() => _layer = v),
            itemBuilder: (_) => const [
              PopupMenuItem(value: 'markers', child: Text('Markers')),
              PopupMenuItem(value: 'heat', child: Text('Density')),
              PopupMenuItem(value: 'both', child: Text('Both')),
            ],
            icon: const Icon(Icons.layers_outlined),
          ),
        ],
      ),
      body: Column(
        children: [
          if (_stats.isNotEmpty)
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 4),
              child: Row(
                children: [
                  _statChip('Live', _stats['live']),
                  const SizedBox(width: 8),
                  _statChip('Today', _stats['today']),
                  const SizedBox(width: 8),
                  _statChip('Tomorrow', _stats['tomorrow']),
                  const SizedBox(width: 8),
                  _statChip('Week', _stats['week']),
                  const SizedBox(width: 8),
                  _statChip('Month', _stats['month']),
                  const SizedBox(width: 8),
                  _statChip('Shown', _markersData.length),
                ],
              ),
            ),
          SizedBox(
            height: 44,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              itemCount: _filters.length,
              separatorBuilder: (_, _) => const SizedBox(width: 8),
              itemBuilder: (_, i) {
                final f = _filters[i];
                final selected = _filter == f.$1;
                return ChoiceChip(
                  label: Text(f.$2),
                  selected: selected,
                  onSelected: (_) => _onFilter(f.$1),
                  selectedColor: AppTheme.primary.withValues(alpha: 0.18),
                  labelStyle: GoogleFonts.outfit(
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                    color: selected ? AppTheme.primary : AppTheme.textSecondary,
                  ),
                );
              },
            ),
          ),
          Expanded(
            child: _loading && _markers.isEmpty && _circles.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : _error != null && _markers.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(_error!, style: const TextStyle(color: Colors.red)),
                            ElevatedButton(onPressed: _load, child: const Text('Retry')),
                          ],
                        ),
                      )
                    : Stack(
                        children: [
                          GoogleMap(
                            initialCameraPosition: CameraPosition(target: _center, zoom: _zoom),
                            markers: _visibleMarkers,
                            circles: _visibleCircles,
                            myLocationButtonEnabled: false,
                            compassEnabled: true,
                            onMapCreated: (c) => _mapController = c,
                          ),
                          if (_loading)
                            const Positioned(
                              top: 0,
                              left: 0,
                              right: 0,
                              child: LinearProgressIndicator(minHeight: 2, color: AppTheme.primary),
                            ),
                          Positioned(
                            right: 12,
                            bottom: 16,
                            child: FloatingActionButton.small(
                              heroTag: 'heat-refresh',
                              onPressed: _load,
                              backgroundColor: Colors.white,
                              child: const Icon(Icons.refresh, color: AppTheme.primary),
                            ),
                          ),
                        ],
                      ),
          ),
        ],
      ),
    );
  }
}
