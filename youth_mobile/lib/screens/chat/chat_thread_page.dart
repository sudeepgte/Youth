import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../../services/app_api.dart';
import '../../theme/app_theme.dart';

class ChatThreadPage extends StatefulWidget {
  const ChatThreadPage({
    super.key,
    required this.conversationId,
    required this.title,
    this.otherUserId,
  });

  final int conversationId;
  final String title;
  final int? otherUserId;

  @override
  State<ChatThreadPage> createState() => _ChatThreadPageState();
}

class _ChatThreadPageState extends State<ChatThreadPage> {
  final _messageCtrl = TextEditingController();
  List<Map<String, dynamic>> _messages = [];
  bool _loading = true;
  bool _sending = false;
  String? _error;

  bool get _canSend => widget.otherUserId != null;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _messageCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await AppApi.chatHistory(widget.conversationId);
      if (!mounted) return;
      setState(() {
        _messages = list;
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

  Future<void> _send() async {
    if (!_canSend) return;
    final text = _messageCtrl.text.trim();
    if (text.isEmpty) return;

    setState(() => _sending = true);
    try {
      await AppApi.sendDirect(receiverId: widget.otherUserId!, content: text);
      _messageCtrl.clear();
      await _load();
    } catch (e) {
      if (mounted) AppTheme.showError(context, e);
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  String _messageText(Map<String, dynamic> m) =>
      m['content']?.toString() ?? m['text']?.toString() ?? m['message']?.toString() ?? '';

  String _senderName(Map<String, dynamic> m) =>
      m['senderUsername']?.toString() ?? m['username']?.toString() ?? m['sender']?.toString() ?? 'User';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.dashboardBg,
      appBar: AppBar(
        title: Text(widget.title, style: GoogleFonts.outfit(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
      ),
      body: Column(
        children: [
          if (!_canSend)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(10),
              color: Colors.amber.shade50,
              child: Text(
                'Read-only: direct messaging unavailable for this conversation.',
                style: GoogleFonts.inter(fontSize: 12, color: Colors.amber.shade900),
                textAlign: TextAlign.center,
              ),
            ),
          Expanded(
            child: _loading
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
                    : _messages.isEmpty
                        ? Center(child: Text('No messages yet', style: GoogleFonts.inter(color: Colors.grey)))
                        : ListView.builder(
                            padding: const EdgeInsets.all(12),
                            itemCount: _messages.length,
                            itemBuilder: (context, index) {
                              final m = _messages[index];
                              return Align(
                                alignment: Alignment.centerLeft,
                                child: Container(
                                  margin: const EdgeInsets.only(bottom: 8),
                                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                                  decoration: BoxDecoration(
                                    color: Colors.white,
                                    borderRadius: BorderRadius.circular(16),
                                  ),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        _senderName(m),
                                        style: GoogleFonts.outfit(fontWeight: FontWeight.bold, fontSize: 12),
                                      ),
                                      const SizedBox(height: 4),
                                      Text(_messageText(m), style: GoogleFonts.inter()),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
          ),
          if (_canSend)
            SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _messageCtrl,
                        decoration: AppTheme.dashboardInput('Type a message...'),
                        onSubmitted: (_) => _send(),
                      ),
                    ),
                    IconButton(
                      onPressed: _sending ? null : _send,
                      icon: _sending
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.send, color: Colors.blueAccent),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}
