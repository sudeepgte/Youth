import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../config/api_config.dart';
import 'api_client.dart';

typedef JsonHandler = void Function(Map<String, dynamic> data);

class RealtimeService {
  RealtimeService._();
  static final RealtimeService instance = RealtimeService._();

  StompClient? _client;
  bool _connecting = false;
  final Map<String, List<JsonHandler>> _handlers = {};

  Future<void> connect() async {
    if (_client?.connected == true || _connecting) return;
    _connecting = true;
    final token = await ApiClient.instance.getToken();
    final authQuery = token == null ? '' : '?auth=$token';
    final url = '${ApiConfig.baseUrl}/ws-chat$authQuery';

    _client = StompClient(
      config: StompConfig.sockJS(
        url: url,
        stompConnectHeaders: token == null ? {} : {'Authorization': 'Bearer $token'},
        webSocketConnectHeaders: token == null ? {} : {'Authorization': 'Bearer $token'},
        onConnect: (_) {
          for (final entry in _handlers.entries) {
            _client?.subscribe(
              destination: entry.key,
              callback: (frame) {
                final payload = frame.body;
                if (payload == null || payload.isEmpty) return;
                try {
                  final decoded = jsonDecode(payload);
                  Map<String, dynamic>? map;
                  if (decoded is Map<String, dynamic>) {
                    map = decoded;
                  } else if (decoded is Map) {
                    map = Map<String, dynamic>.from(decoded);
                  } else if (decoded is List) {
                    map = {'entries': decoded};
                  }
                  if (map != null) {
                    for (final fn in entry.value) {
                      fn(map);
                    }
                  }
                } catch (_) {
                  // Ignore non-json payloads
                }
              },
            );
          }
        },
        onWebSocketError: (dynamic error) {
          debugPrint('WS error: $error');
        },
        onStompError: (frame) {
          debugPrint('STOMP error: ${frame.body}');
        },
      ),
    );
    _client?.activate();
    _connecting = false;
  }

  Future<void> subscribeJson(String destination, JsonHandler handler) async {
    _handlers.putIfAbsent(destination, () => []).add(handler);
    await connect();
    if (_client?.connected == true) {
      _client?.subscribe(
        destination: destination,
        callback: (frame) {
          final payload = frame.body;
          if (payload == null || payload.isEmpty) return;
          try {
            final decoded = jsonDecode(payload);
            if (decoded is Map<String, dynamic>) {
              handler(decoded);
            } else if (decoded is Map) {
              handler(Map<String, dynamic>.from(decoded));
            } else if (decoded is List) {
              handler({'entries': decoded});
            }
          } catch (_) {}
        },
      );
    }
  }

  void send(String destination, Map<String, dynamic> body) {
    if (_client?.connected != true) {
      connect().then((_) {
        _client?.send(destination: destination, body: jsonEncode(body));
      });
      return;
    }
    _client?.send(destination: destination, body: jsonEncode(body));
  }

  void disconnect() {
    _client?.deactivate();
    _client = null;
    _handlers.clear();
  }
}
