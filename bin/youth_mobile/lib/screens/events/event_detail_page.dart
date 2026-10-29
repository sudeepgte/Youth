import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../config/api_config.dart';
import '../../models/event_model.dart';
import '../../providers/auth_provider.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import 'ticket_page.dart';

class EventDetailPage extends StatefulWidget {
  const EventDetailPage({super.key, required this.eventId});

  final int eventId;

  @override
  State<EventDetailPage> createState() => _EventDetailPageState();
}

class _EventDetailPageState extends State<EventDetailPage> {
  final _formKey = GlobalKey<FormState>();
  final _fullNameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _collegeCtrl = TextEditingController();
  final _ageCtrl = TextEditingController();
  final _cityCtrl = TextEditingController();
  final _emergencyNameCtrl = TextEditingController();
  final _emergencyPhoneCtrl = TextEditingController();

  EventModel? _event;
  bool _loading = true;
  bool _registering = false;
  String? _error;
  String? _yearOfStudy;
  String? _gender;
  String? _diet;
  int _quantity = 1;
  String _tier = 'REGULAR';
  final Set<int> _selectedSeatIds = {};
  bool _prefilled = false;
  bool _joiningOnline = false;

  static const _years = [
    '1st Year',
    '2nd Year',
    '3rd Year',
    '4th Year',
    'Postgraduate',
    'Working Professional',
    'Other',
  ];
  static const _genders = ['Male', 'Female', 'Other'];
  static const _diets = ['Veg', 'Non-Veg', 'Vegan', 'No Preference'];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _fullNameCtrl.dispose();
    _emailCtrl.dispose();
    _phoneCtrl.dispose();
    _collegeCtrl.dispose();
    _ageCtrl.dispose();
    _cityCtrl.dispose();
    _emergencyNameCtrl.dispose();
    _emergencyPhoneCtrl.dispose();
    super.dispose();
  }

  void _prefillFromAuth() {
    if (_prefilled) return;
    final user = context.read<AuthProvider>().user;
    if (user == null) return;
    _fullNameCtrl.text = user.username;
    _emailCtrl.text = user.email ?? '';
    _collegeCtrl.text = user.collegeName ?? '';
    if (user.gender != null && _genders.contains(user.gender)) {
      _gender = user.gender;
    }
    _prefilled = true;
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final event = await AppApi.eventDetail(widget.eventId);
      if (!mounted) return;
      _prefillFromAuth();
      setState(() {
        _event = event;
        if (event.vipPrice != null && (event.regularPrice == null || event.vipPrice! > 0)) {
          // keep REGULAR default
        }
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

  bool get _isHouseParty => (_event?.category ?? '').toLowerCase().contains('house');

  List<Map<String, dynamic>> get _availableSeats =>
      (_event?.seats ?? []).where((s) => (s['status']?.toString().toUpperCase() == 'AVAILABLE')).toList();

  double get _estimatedTotal {
    final e = _event;
    if (e == null) return 0;
    if (e.hasFreeEntry) return 0;
    if (_selectedSeatIds.isNotEmpty) {
      double sum = 0;
      for (final s in e.seats) {
        final id = (s['id'] as num?)?.toInt();
        if (id != null && _selectedSeatIds.contains(id)) {
          sum += (s['price'] as num?)?.toDouble() ?? e.effectiveUnitPrice(_tier);
        }
      }
      return e.hasDiscount ? sum * 0.5 : sum;
    }
    final unit = e.effectiveUnitPrice(_tier);
    final total = unit * _quantity;
    return e.hasDiscount ? total * 0.5 : total;
  }

  Future<void> _joinOnline() async {
    setState(() => _joiningOnline = true);
    try {
      final res = await AppApi.joinOnlineEvent(widget.eventId);
      final link = res['meetingLink']?.toString().trim();
      if (link != null && link.isNotEmpty) {
        final uri = Uri.tryParse(link);
        if (uri != null && await canLaunchUrl(uri)) {
          await launchUrl(uri, mode: LaunchMode.externalApplication);
        } else if (mounted) {
          AppTheme.showError(context, 'Could not open meeting link');
        }
      } else if (mounted) {
        AppTheme.showSuccess(context, res['message']?.toString() ?? 'Attendance marked');
      }
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _joiningOnline = false);
    }
  }

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;
    final e = _event;
    if (e == null) return;
    if (e.isRegistered) {
      AppTheme.showError(context, 'Already registered');
      return;
    }
    if (_isHouseParty) {
      if (_ageCtrl.text.trim().isEmpty || _cityCtrl.text.trim().isEmpty) {
        AppTheme.showError(context, 'Age and city are required for House Party');
        return;
      }
    }
    if (e.hasSeats && _availableSeats.isNotEmpty && _selectedSeatIds.isEmpty) {
      AppTheme.showError(context, 'Select at least one seat');
      return;
    }

    final total = _estimatedTotal;
    if (total > 0) {
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text('Confirm payment', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
          content: Text(
            'Pay ₹${total.toStringAsFixed(0)} from your wallet to register?'
            '${e.hasDiscount ? '\n(50% discount applied)' : ''}',
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
              child: Text('Pay ₹${total.toStringAsFixed(0)}'),
            ),
          ],
        ),
      );
      if (ok != true) return;
    }

    setState(() => _registering = true);
    try {
      final fields = <String, dynamic>{
        'fullName': _fullNameCtrl.text.trim(),
        'email': _emailCtrl.text.trim(),
        'phone': _phoneCtrl.text.trim(),
        'college': _collegeCtrl.text.trim(),
        if (_yearOfStudy != null) 'yearOfStudy': _yearOfStudy,
        if (_gender != null) 'gender': _gender,
        'quantity': _selectedSeatIds.isNotEmpty ? _selectedSeatIds.length : _quantity,
        'selectedTier': _tier,
        if (_selectedSeatIds.isNotEmpty) 'selectedSeatIds': _selectedSeatIds.toList(),
        if (_isHouseParty) ...{
          'lp_age': _ageCtrl.text.trim(),
          'lp_city': _cityCtrl.text.trim(),
          if (_emergencyNameCtrl.text.trim().isNotEmpty) 'lp_emergencyContactName': _emergencyNameCtrl.text.trim(),
          if (_emergencyPhoneCtrl.text.trim().isNotEmpty) 'lp_emergencyContactMobile': _emergencyPhoneCtrl.text.trim(),
          if (_diet != null) 'lp_dietaryPreference': _diet,
        },
      };
      final res = await AppApi.registerEventWithFields(widget.eventId, fields);
      if (!mounted) return;
      AppTheme.showSuccess(context, res['message']?.toString() ?? 'Registered successfully');
      final ticketId = res['ticketId']?.toString();
      if (ticketId != null && ticketId.isNotEmpty) {
        await Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => TicketPage(ticketId: ticketId)),
        );
      }
      await _load();
    } catch (err) {
      if (mounted) AppTheme.showError(context, err);
    } finally {
      if (mounted) setState(() => _registering = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text('Event Details', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
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
              : _event == null
                  ? const Center(child: Text('Event not found'))
                  : SingleChildScrollView(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (_event!.imageUrl != null && _event!.imageUrl!.isNotEmpty)
                            ClipRRect(
                              borderRadius: BorderRadius.circular(16),
                              child: CachedNetworkImage(
                                imageUrl: ApiConfig.mediaUrl(_event!.imageUrl),
                                width: double.infinity,
                                height: 200,
                                fit: BoxFit.cover,
                              ),
                            ),
                          const SizedBox(height: 16),
                          if (_event!.category != null)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                              decoration: BoxDecoration(
                                color: AppTheme.primary.withValues(alpha: 0.12),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                _event!.category!,
                                style: GoogleFonts.outfit(fontWeight: FontWeight.w700, color: AppTheme.primary, fontSize: 12),
                              ),
                            ),
                          const SizedBox(height: 8),
                          Text(
                            _event!.title ?? 'Untitled',
                            style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 24),
                          ),
                          const SizedBox(height: 8),
                          if (_event!.organizer != null)
                            Text('Organizer: ${_event!.organizer}', style: GoogleFonts.inter(color: Colors.grey.shade700)),
                          if (_event!.venue != null) ...[
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                const Icon(Icons.location_on, size: 16, color: AppTheme.primary),
                                const SizedBox(width: 4),
                                Expanded(child: Text(_event!.venue!, style: GoogleFonts.inter())),
                              ],
                            ),
                          ],
                          if (_event!.dateTime != null) ...[
                            const SizedBox(height: 4),
                            Row(
                              children: [
                                const Icon(Icons.schedule, size: 16, color: AppTheme.primary),
                                const SizedBox(width: 4),
                                Expanded(child: Text(_event!.dateTime!, style: GoogleFonts.inter())),
                              ],
                            ),
                          ],
                          const SizedBox(height: 10),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: [
                              _infoChip(_event!.priceLabel(tier: _tier)),
                              if (_event!.registeredCount != null)
                                _infoChip('${_event!.registeredCount} going'),
                              if (_event!.spotsLeft != null) _infoChip('${_event!.spotsLeft} spots left'),
                              if (_event!.eventMode != null) _infoChip(_event!.eventMode!),
                              if (_event!.status != null) _infoChip(_event!.status!),
                            ],
                          ),
                          if (_event!.enableSecretRewards) ...[
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                Icon(Icons.card_giftcard, size: 16, color: Colors.amber.shade700),
                                const SizedBox(width: 4),
                                Text(
                                  'Secret rewards enabled',
                                  style: GoogleFonts.inter(color: Colors.amber.shade800, fontWeight: FontWeight.w600),
                                ),
                              ],
                            ),
                          ],
                          const SizedBox(height: 16),
                          if (_event!.description != null)
                            Text(_event!.description!, style: GoogleFonts.inter(height: 1.5)),
                          const SizedBox(height: 24),
                          if (_event!.isRegistered) ...[
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.all(14),
                              decoration: BoxDecoration(
                                color: const Color(0xFFECFDF5),
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(color: const Color(0xFFA7F3D0)),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('You’re registered', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
                                  const SizedBox(height: 8),
                                  Wrap(
                                    spacing: 8,
                                    runSpacing: 8,
                                    children: [
                                      if (_event!.myTicketId != null)
                                        ElevatedButton(
                                          onPressed: () => Navigator.push(
                                            context,
                                            MaterialPageRoute(
                                              builder: (_) => TicketPage(ticketId: _event!.myTicketId!),
                                            ),
                                          ),
                                          style: ElevatedButton.styleFrom(
                                            backgroundColor: AppTheme.primary,
                                            foregroundColor: Colors.white,
                                          ),
                                          child: const Text('View E-Ticket'),
                                        ),
                                      OutlinedButton.icon(
                                        onPressed: _joiningOnline ? null : _joinOnline,
                                        icon: _joiningOnline
                                            ? const SizedBox(
                                                width: 14,
                                                height: 14,
                                                child: CircularProgressIndicator(strokeWidth: 2),
                                              )
                                            : const Icon(Icons.videocam_outlined, size: 18),
                                        label: Text(
                                          (_event!.meetingLink ?? '').trim().isNotEmpty
                                              ? 'Join Online'
                                              : 'Check in / Join',
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          ] else if ((_event!.status ?? '').toUpperCase() == 'COMPLETED' ||
                              (_event!.status ?? '').toUpperCase() == 'CANCELLED') ...[
                            Text(
                              'Registration closed for this event.',
                              style: GoogleFonts.inter(color: AppTheme.textMuted),
                            ),
                          ] else ...[
                            Text('Registration', style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 18)),
                            const SizedBox(height: 12),
                            if ((_event!.vipPrice ?? 0) > 0 || (_event!.regularPrice ?? 0) > 0) ...[
                              Text('Ticket tier', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                              const SizedBox(height: 8),
                              Row(
                                children: [
                                  Expanded(
                                    child: _tierCard(
                                      'REGULAR',
                                      _event!.priceLabel(tier: 'REGULAR'),
                                      selected: _tier == 'REGULAR',
                                    ),
                                  ),
                                  if ((_event!.vipPrice ?? 0) > 0) ...[
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: _tierCard(
                                        'VIP',
                                        _event!.priceLabel(tier: 'VIP'),
                                        selected: _tier == 'VIP',
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                              const SizedBox(height: 12),
                            ],
                            if (_event!.hasSeats && _availableSeats.isNotEmpty) ...[
                              Text('Select seats', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                              const SizedBox(height: 8),
                              Wrap(
                                spacing: 6,
                                runSpacing: 6,
                                children: _availableSeats.map((s) {
                                  final id = (s['id'] as num?)?.toInt();
                                  final label = s['label']?.toString() ?? '?';
                                  final type = s['seatType']?.toString() ?? 'REGULAR';
                                  final selected = id != null && _selectedSeatIds.contains(id);
                                  return FilterChip(
                                    label: Text('$label · $type'),
                                    selected: selected,
                                    onSelected: id == null
                                        ? null
                                        : (v) => setState(() {
                                              if (v) {
                                                _selectedSeatIds.add(id);
                                                _tier = type;
                                              } else {
                                                _selectedSeatIds.remove(id);
                                              }
                                            }),
                                    selectedColor: const Color(0xFFFEF3C7),
                                  );
                                }).toList(),
                              ),
                              const SizedBox(height: 12),
                            ],
                            Form(
                              key: _formKey,
                              child: Column(
                                children: [
                                  TextFormField(
                                    controller: _fullNameCtrl,
                                    decoration: AppTheme.dashboardInput('Full name *'),
                                    validator: (v) =>
                                        (v == null || v.trim().isEmpty) ? 'Full name is required' : null,
                                  ),
                                  const SizedBox(height: 10),
                                  TextFormField(
                                    controller: _emailCtrl,
                                    keyboardType: TextInputType.emailAddress,
                                    decoration: AppTheme.dashboardInput('Email *'),
                                    validator: (v) {
                                      if (v == null || v.trim().isEmpty) return 'Email is required';
                                      if (!v.contains('@')) return 'Enter a valid email';
                                      return null;
                                    },
                                  ),
                                  const SizedBox(height: 10),
                                  TextFormField(
                                    controller: _phoneCtrl,
                                    keyboardType: TextInputType.phone,
                                    decoration: AppTheme.dashboardInput('Phone *'),
                                    validator: (v) {
                                      if (v == null || v.trim().isEmpty) return 'Phone is required';
                                      final digits = v.replaceAll(RegExp(r'[^0-9+]'), '');
                                      if (digits.length < 10) return 'Enter a valid phone';
                                      return null;
                                    },
                                  ),
                                  const SizedBox(height: 10),
                                  TextFormField(
                                    controller: _collegeCtrl,
                                    decoration: AppTheme.dashboardInput('College / Organization'),
                                  ),
                                  const SizedBox(height: 10),
                                  DropdownButtonFormField<String>(
                                    initialValue: _yearOfStudy,
                                    decoration: AppTheme.dashboardInput('Year of study'),
                                    items: _years.map((y) => DropdownMenuItem(value: y, child: Text(y))).toList(),
                                    onChanged: (v) => setState(() => _yearOfStudy = v),
                                  ),
                                  const SizedBox(height: 10),
                                  DropdownButtonFormField<String?>(
                                    initialValue: _gender,
                                    decoration: AppTheme.dashboardInput('Gender'),
                                    items: [
                                      const DropdownMenuItem<String?>(value: null, child: Text('Prefer not to say')),
                                      ..._genders.map((g) => DropdownMenuItem<String?>(value: g, child: Text(g))),
                                    ],
                                    onChanged: (v) => setState(() => _gender = v),
                                  ),
                                  if (_selectedSeatIds.isEmpty) ...[
                                    const SizedBox(height: 10),
                                    DropdownButtonFormField<int>(
                                      initialValue: _quantity,
                                      decoration: AppTheme.dashboardInput('Quantity'),
                                      items: List.generate(
                                        10,
                                        (i) => DropdownMenuItem(value: i + 1, child: Text('${i + 1}')),
                                      ),
                                      onChanged: (v) {
                                        if (v != null) setState(() => _quantity = v);
                                      },
                                    ),
                                  ],
                                  if (_isHouseParty) ...[
                                    const SizedBox(height: 16),
                                    Align(
                                      alignment: Alignment.centerLeft,
                                      child: Text('House Party details', style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
                                    ),
                                    const SizedBox(height: 8),
                                    TextFormField(
                                      controller: _ageCtrl,
                                      keyboardType: TextInputType.number,
                                      decoration: AppTheme.dashboardInput('Age *'),
                                    ),
                                    const SizedBox(height: 10),
                                    TextFormField(
                                      controller: _cityCtrl,
                                      decoration: AppTheme.dashboardInput('City *'),
                                    ),
                                    const SizedBox(height: 10),
                                    TextFormField(
                                      controller: _emergencyNameCtrl,
                                      decoration: AppTheme.dashboardInput('Emergency contact name'),
                                    ),
                                    const SizedBox(height: 10),
                                    TextFormField(
                                      controller: _emergencyPhoneCtrl,
                                      keyboardType: TextInputType.phone,
                                      decoration: AppTheme.dashboardInput('Emergency contact mobile'),
                                    ),
                                    const SizedBox(height: 10),
                                    DropdownButtonFormField<String>(
                                      initialValue: _diet,
                                      decoration: AppTheme.dashboardInput('Dietary preference'),
                                      items: _diets.map((d) => DropdownMenuItem(value: d, child: Text(d))).toList(),
                                      onChanged: (v) => setState(() => _diet = v),
                                    ),
                                  ],
                                  const SizedBox(height: 16),
                                  if (_estimatedTotal > 0)
                                    Padding(
                                      padding: const EdgeInsets.only(bottom: 10),
                                      child: Text(
                                        'Total: ₹${_estimatedTotal.toStringAsFixed(0)}'
                                        '${_event!.hasDiscount ? ' (50% off)' : ''}',
                                        style: GoogleFonts.outfit(fontWeight: FontWeight.w800, fontSize: 16),
                                      ),
                                    ),
                                  SizedBox(
                                    width: double.infinity,
                                    child: ElevatedButton(
                                      onPressed: _registering ? null : _register,
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: AppTheme.primary,
                                        padding: const EdgeInsets.symmetric(vertical: 14),
                                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                                      ),
                                      child: _registering
                                          ? const SizedBox(
                                              height: 20,
                                              width: 20,
                                              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                            )
                                          : Text(
                                              _estimatedTotal > 0
                                                  ? 'Pay ₹${_estimatedTotal.toStringAsFixed(0)} & Register'
                                                  : 'Register Free',
                                              style: GoogleFonts.inter(fontWeight: FontWeight.bold, color: Colors.white),
                                            ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
    );
  }

  Widget _infoChip(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(text, style: GoogleFonts.outfit(fontSize: 12, fontWeight: FontWeight.w700)),
    );
  }

  Widget _tierCard(String tier, String price, {required bool selected}) {
    return InkWell(
      onTap: () => setState(() => _tier = tier),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: selected ? AppTheme.primary.withValues(alpha: 0.1) : Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: selected ? AppTheme.primary : const Color(0xFFE2E8F0)),
        ),
        child: Column(
          children: [
            Text(tier, style: GoogleFonts.outfit(fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            Text(price, style: GoogleFonts.inter(color: const Color(0xFF059669), fontWeight: FontWeight.w700)),
          ],
        ),
      ),
    );
  }
}
