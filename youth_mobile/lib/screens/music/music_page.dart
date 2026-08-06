import 'dart:io';

import 'package:dio/dio.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:video_player/video_player.dart';

import '../../config/api_config.dart';
import '../../services/app_api.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import 'music_leaderboard_page.dart';
import 'music_room_live_page.dart';

class MusicPage extends StatefulWidget {
  const MusicPage({super.key});

  @override
  State<MusicPage> createState() => _MusicPageState();
}

class _MusicPageState extends State<MusicPage> with SingleTickerProviderStateMixin {
  late TabController _tabCtrl;
  List<Map<String, dynamic>> _tracks = [];
  List<Map<String, dynamic>> _rooms = [];
  bool _loading = true;
  String? _error;
  final Set<int> _liking = {};
  VideoPlayerController? _player;
  int? _playingId;
  bool _playerReady = false;
  DateTime? _listenStarted;

  @override
  void initState() {
    super.initState();
    _tabCtrl = TabController(length: 2, vsync: this);
    _load();
  }

  @override
  void dispose() {
    _flushListen();
    _tabCtrl.dispose();
    _player?.dispose();
    super.dispose();
  }

  Future<void> _flushListen() async {
    final id = _playingId;
    final started = _listenStarted;
    if (id == null || started == null) return;
    final secs = DateTime.now().difference(started).inSeconds;
    _listenStarted = null;
    if (secs < 3) return;
    try {
      await AppApi.recordMusicListen(id, secs.clamp(3, 300));
    } catch (_) {}
  }

  Future<void> _playTrack(Map<String, dynamic> track) async {
    final id = (track['id'] as num?)?.toInt();
    final raw = track['streamUrl']?.toString() ??
        track['audioUrl']?.toString() ??
        track['url']?.toString() ??
        track['fileUrl']?.toString() ??
        track['mediaUrl']?.toString();
    if (raw == null || raw.isEmpty) {
      AppTheme.showError(context, 'No audio URL for this track');
      return;
    }
    if (_playingId == id && _player != null && _playerReady) {
      if (_player!.value.isPlaying) {
        await _player!.pause();
        await _flushListen();
      } else {
        _listenStarted = DateTime.now();
        await _player!.play();
      }
      setState(() {});
      return;
    }
    await _flushListen();
    await _player?.dispose();
    setState(() {
      _player = null;
      _playerReady = false;
      _playingId = id;
    });
    final url = ApiConfig.mediaUrl(raw);
    final ctrl = VideoPlayerController.networkUrl(Uri.parse(url));
    try {
      await ctrl.initialize();
      await ctrl.setLooping(false);
      await ctrl.play();
      if (!mounted) {
        await ctrl.dispose();
        return;
      }
      _listenStarted = DateTime.now();
      setState(() {
        _player = ctrl;
        _playerReady = true;
      });
    } catch (e) {
      await ctrl.dispose();
      if (mounted) AppTheme.showError(context, 'Could not play track');
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([AppApi.musicTracks(), AppApi.musicRooms()]);
      if (!mounted) return;
      setState(() {
        _tracks = results[0];
        _rooms = results[1];
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

  Future<void> _likeTrack(Map<String, dynamic> track, int index) async {
    final id = (track['id'] as num?)?.toInt();
    if (id == null || _liking.contains(id)) return;
    setState(() => _liking.add(id));
    try {
      await AppApi.likeMusicTrack(id);
      if (!mounted) return;
      setState(() {
        final updated = Map<String, dynamic>.from(_tracks[index]);
        final wasLiked = updated['liked'] == true;
        updated['liked'] = !wasLiked;
        final count = (updated['likeCount'] as num?)?.toInt() ?? 0;
        updated['likeCount'] = wasLiked ? (count > 0 ? count - 1 : 0) : count + 1;
        _tracks[index] = updated;
      });
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _liking.remove(id));
    }
  }

  Future<void> _uploadTrack() async {
    final titleCtrl = TextEditingController();
    final artistCtrl = TextEditingController();
    final licenseCtrl = TextEditingController(text: 'All Rights Reserved');
    final picked = await FilePicker.platform.pickFiles(type: FileType.audio, withData: false);
    if (picked == null || picked.files.isEmpty || picked.files.first.path == null) return;
    final path = picked.files.first.path!;

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Upload track', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(picked.files.first.name, style: GoogleFonts.inter(fontSize: 12, color: Colors.black54)),
              const SizedBox(height: 12),
              TextField(controller: titleCtrl, decoration: AppTheme.dashboardInput('Title')),
              const SizedBox(height: 10),
              TextField(controller: artistCtrl, decoration: AppTheme.dashboardInput('Artist')),
              const SizedBox(height: 10),
              TextField(controller: licenseCtrl, decoration: AppTheme.dashboardInput('License')),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primary, foregroundColor: Colors.white),
            child: const Text('Upload'),
          ),
        ],
      ),
    );
    final title = titleCtrl.text.trim();
    final artist = artistCtrl.text.trim();
    final license = licenseCtrl.text.trim();
    titleCtrl.dispose();
    artistCtrl.dispose();
    licenseCtrl.dispose();
    if (ok != true || title.isEmpty) return;

    try {
      final file = await MultipartFile.fromFile(path, filename: path.split(Platform.pathSeparator).last);
      await AppApi.uploadMusicTrack(
        title: title,
        artistName: artist.isEmpty ? 'Unknown' : artist,
        licenseName: license.isEmpty ? 'All Rights Reserved' : license,
        file: file,
      );
      if (!mounted) return;
      AppTheme.showSuccess(context, 'Uploaded — pending approval');
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _createRoom() async {
    final nameCtrl = TextEditingController();
    final categoryCtrl = TextEditingController();
    final titleCtrl = TextEditingController();
    final artistCtrl = TextEditingController();
    int? selectedTrackId;
    String? localPath;
    String? localName;
    var acceptTerms = false;
    var mode = 'none'; // none | catalog | local

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocal) => AlertDialog(
          title: const Text('Create Music Room'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(controller: nameCtrl, decoration: AppTheme.dashboardInput('Room name')),
                const SizedBox(height: 12),
                TextField(controller: categoryCtrl, decoration: AppTheme.dashboardInput('Category')),
                const SizedBox(height: 12),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text('Optional opening track', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                ),
                RadioListTile<String>(
                  dense: true,
                  title: const Text('None'),
                  value: 'none',
                  groupValue: mode,
                  onChanged: (v) => setLocal(() {
                    mode = v!;
                    selectedTrackId = null;
                    localPath = null;
                  }),
                ),
                RadioListTile<String>(
                  dense: true,
                  title: const Text('Approved catalog track'),
                  value: 'catalog',
                  groupValue: mode,
                  onChanged: (v) => setLocal(() => mode = v!),
                ),
                if (mode == 'catalog')
                  DropdownButtonFormField<int>(
                    decoration: AppTheme.dashboardInput('Track'),
                    items: _tracks
                        .map((t) {
                          final id = (t['id'] as num?)?.toInt();
                          if (id == null) return null;
                          return DropdownMenuItem(
                            value: id,
                            child: Text(t['title']?.toString() ?? 'Track $id'),
                          );
                        })
                        .whereType<DropdownMenuItem<int>>()
                        .toList(),
                    onChanged: (v) => setLocal(() => selectedTrackId = v),
                  ),
                RadioListTile<String>(
                  dense: true,
                  title: const Text('Upload local audio'),
                  value: 'local',
                  groupValue: mode,
                  onChanged: (v) => setLocal(() => mode = v!),
                ),
                if (mode == 'local') ...[
                  OutlinedButton.icon(
                    onPressed: () async {
                      final picked = await FilePicker.platform.pickFiles(type: FileType.audio);
                      if (picked == null || picked.files.isEmpty || picked.files.first.path == null) return;
                      setLocal(() {
                        localPath = picked.files.first.path;
                        localName = picked.files.first.name;
                      });
                    },
                    icon: const Icon(Icons.audio_file),
                    label: Text(localName ?? 'Pick audio file'),
                  ),
                  TextField(controller: titleCtrl, decoration: AppTheme.dashboardInput('Track title')),
                  const SizedBox(height: 8),
                  TextField(controller: artistCtrl, decoration: AppTheme.dashboardInput('Artist')),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    value: acceptTerms,
                    onChanged: (v) => setLocal(() => acceptTerms = v ?? false),
                    title: Text('I have rights to upload this audio', style: GoogleFonts.inter(fontSize: 13)),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Create')),
          ],
        ),
      ),
    );
    final name = nameCtrl.text.trim();
    final category = categoryCtrl.text.trim();
    final localTitle = titleCtrl.text.trim();
    final localArtist = artistCtrl.text.trim();
    nameCtrl.dispose();
    categoryCtrl.dispose();
    titleCtrl.dispose();
    artistCtrl.dispose();
    if (ok != true || name.isEmpty) return;
    if (mode == 'local' && (localPath == null || !acceptTerms)) {
      AppTheme.showError(context, 'Pick a file and accept upload terms');
      return;
    }

    try {
      MultipartFile? file;
      if (mode == 'local' && localPath != null) {
        file = await MultipartFile.fromFile(
          localPath!,
          filename: localName ?? localPath!.split(Platform.pathSeparator).last,
        );
      }
      final res = await AppApi.createMusicRoom(
        name: name,
        category: category.isEmpty ? null : category,
        trackId: mode == 'catalog' ? selectedTrackId : null,
        localFile: file,
        localTitle: localTitle.isEmpty ? null : localTitle,
        localArtistName: localArtist.isEmpty ? null : localArtist,
      );
      if (!mounted) return;
      final code = (res['code'] ?? res['roomCode'])?.toString();
      AppTheme.showSuccess(context, 'Room created${code != null ? ': $code' : ''}');
      await _load();
      if (code != null && code.isNotEmpty && mounted) {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => MusicRoomLivePage(code: code, name: name)),
        );
      }
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Future<void> _joinByCode() async {
    final codeCtrl = TextEditingController();
    final titleCtrl = TextEditingController();
    final artistCtrl = TextEditingController();
    int? selectedTrackId;
    String? localPath;
    String? localName;
    var acceptTerms = false;
    var mode = 'none';

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocal) => AlertDialog(
          title: const Text('Join Music Room'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: codeCtrl,
                  decoration: AppTheme.dashboardInput('Room code'),
                  textCapitalization: TextCapitalization.characters,
                ),
                const SizedBox(height: 12),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text('Optional track on join', style: GoogleFonts.outfit(fontWeight: FontWeight.w700)),
                ),
                RadioListTile<String>(
                  dense: true,
                  title: const Text('None'),
                  value: 'none',
                  groupValue: mode,
                  onChanged: (v) => setLocal(() => mode = v!),
                ),
                RadioListTile<String>(
                  dense: true,
                  title: const Text('Catalog track'),
                  value: 'catalog',
                  groupValue: mode,
                  onChanged: (v) => setLocal(() => mode = v!),
                ),
                if (mode == 'catalog')
                  DropdownButtonFormField<int>(
                    decoration: AppTheme.dashboardInput('Track'),
                    items: _tracks
                        .map((t) {
                          final id = (t['id'] as num?)?.toInt();
                          if (id == null) return null;
                          return DropdownMenuItem(value: id, child: Text(t['title']?.toString() ?? 'Track'));
                        })
                        .whereType<DropdownMenuItem<int>>()
                        .toList(),
                    onChanged: (v) => setLocal(() => selectedTrackId = v),
                  ),
                RadioListTile<String>(
                  dense: true,
                  title: const Text('Local audio upload'),
                  value: 'local',
                  groupValue: mode,
                  onChanged: (v) => setLocal(() => mode = v!),
                ),
                if (mode == 'local') ...[
                  OutlinedButton.icon(
                    onPressed: () async {
                      final picked = await FilePicker.platform.pickFiles(type: FileType.audio);
                      if (picked?.files.first.path == null) return;
                      setLocal(() {
                        localPath = picked!.files.first.path;
                        localName = picked.files.first.name;
                      });
                    },
                    icon: const Icon(Icons.audio_file),
                    label: Text(localName ?? 'Pick audio'),
                  ),
                  TextField(controller: titleCtrl, decoration: AppTheme.dashboardInput('Title')),
                  TextField(controller: artistCtrl, decoration: AppTheme.dashboardInput('Artist')),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    value: acceptTerms,
                    onChanged: (v) => setLocal(() => acceptTerms = v ?? false),
                    title: Text('I have rights to upload', style: GoogleFonts.inter(fontSize: 13)),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Join')),
          ],
        ),
      ),
    );
    final code = codeCtrl.text.trim().toUpperCase();
    final localTitle = titleCtrl.text.trim();
    final localArtist = artistCtrl.text.trim();
    codeCtrl.dispose();
    titleCtrl.dispose();
    artistCtrl.dispose();
    if (ok != true || code.isEmpty) return;
    if (mode == 'local' && (localPath == null || !acceptTerms)) {
      AppTheme.showError(context, 'Pick a file and accept upload terms');
      return;
    }

    try {
      MultipartFile? file;
      if (mode == 'local' && localPath != null) {
        file = await MultipartFile.fromFile(localPath!, filename: localName);
      }
      final res = await AppApi.joinMusicRoom(
        code: code,
        trackId: mode == 'catalog' ? selectedTrackId : null,
        localFile: file,
        localTitle: localTitle.isEmpty ? null : localTitle,
        localArtistName: localArtist.isEmpty ? null : localArtist,
      );
      if (!mounted) return;
      final roomCode = (res['code'] ?? res['roomCode'] ?? code).toString();
      AppTheme.showSuccess(context, 'Joined room');
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => MusicRoomLivePage(code: roomCode, name: res['name']?.toString() ?? 'Music Room'),
        ),
      );
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    }
  }

  Widget _tracksList() {
    if (_tracks.isEmpty) {
      return Center(child: Text('No tracks', style: GoogleFonts.inter(color: Colors.grey)));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _tracks.length,
      itemBuilder: (context, index) {
        final t = _tracks[index];
        final id = (t['id'] as num?)?.toInt();
        final liked = t['liked'] == true;
        final likeCount = (t['likeCount'] as num?)?.toInt() ?? 0;
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: AppTheme.primary.withValues(alpha: 0.12),
              child: const Icon(Icons.music_note, color: AppTheme.primary),
            ),
            title: Text(
              t['title']?.toString() ?? 'Track',
              style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
            ),
            subtitle: Text(t['artist']?.toString() ?? ''),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  tooltip: _playingId == id && (_player?.value.isPlaying ?? false) ? 'Pause' : 'Play',
                  onPressed: () => _playTrack(t),
                  icon: Icon(
                    _playingId == id && (_player?.value.isPlaying ?? false)
                        ? Icons.pause_circle_filled
                        : Icons.play_circle_filled,
                    color: AppTheme.primary,
                    size: 32,
                  ),
                ),
                Text('$likeCount', style: GoogleFonts.inter(fontSize: 12, color: Colors.grey)),
                IconButton(
                  tooltip: 'Like',
                  onPressed: id == null || _liking.contains(id) ? null : () => _likeTrack(t, index),
                  icon: Icon(
                    liked ? Icons.favorite : Icons.favorite_border,
                    color: liked ? Colors.redAccent : Colors.grey.shade600,
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _roomsList() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
          child: Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _createRoom,
                  icon: const Icon(Icons.add),
                  label: const Text('Create'),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: _joinByCode,
                  icon: const Icon(Icons.login),
                  label: const Text('Join code'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primary,
                    foregroundColor: Colors.white,
                  ),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: _rooms.isEmpty
              ? Center(child: Text('No music rooms', style: GoogleFonts.inter(color: Colors.grey)))
              : ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: _rooms.length,
                  itemBuilder: (context, index) {
                    final r = _rooms[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: Colors.deepPurple.withValues(alpha: 0.12),
                          child: const Icon(Icons.headphones, color: Colors.deepPurple),
                        ),
                        title: Text(
                          r['name']?.toString() ?? 'Room',
                          style: GoogleFonts.outfit(fontWeight: FontWeight.w700),
                        ),
                        subtitle: Text(
                          [
                            if ((r['category']?.toString() ?? '').isNotEmpty) r['category'],
                            'Code: ${r['code'] ?? '—'}',
                            if (r['hostUsername'] != null) 'Host ${r['hostUsername']}',
                          ].join(' · '),
                        ),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () {
                          final code = r['code']?.toString();
                          if (code == null || code.isEmpty) return;
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => MusicRoomLivePage(
                                code: code,
                                name: r['name']?.toString() ?? 'Music Room',
                              ),
                            ),
                          );
                        },
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      drawer: const AppDrawer(active: AppDrawerItem.music),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _uploadTrack,
        backgroundColor: AppTheme.primary,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.upload_file),
        label: const Text('Upload'),
      ),
      appBar: AppBar(
        title: Text('Music', style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
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
          IconButton(
            tooltip: 'Leaderboard',
            icon: const Icon(Icons.leaderboard_outlined),
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const MusicLeaderboardPage()),
            ),
          ),
          IconButton(
            tooltip: 'Back',
            icon: const Icon(Icons.arrow_back_rounded),
            onPressed: () => Navigator.maybePop(context),
          ),
        ],
        bottom: TabBar(
          controller: _tabCtrl,
          labelColor: Colors.blueAccent,
          tabs: [
            Tab(text: 'Tracks (${_tracks.length})'),
            Tab(text: 'Rooms (${_rooms.length})'),
          ],
        ),
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
              : RefreshIndicator(
                  onRefresh: _load,
                  child: TabBarView(
                    controller: _tabCtrl,
                    children: [_tracksList(), _roomsList()],
                  ),
                ),
    );
  }
}
