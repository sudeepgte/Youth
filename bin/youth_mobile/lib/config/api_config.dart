import 'dart:io';

class ApiConfig {
  /// Android emulator → host machine. Physical device: set your PC LAN IP.
  static String get baseUrl {
    if (Platform.isAndroid) {
      return const String.fromEnvironment(
        'API_BASE',
        defaultValue: 'http://10.0.2.2:8009',
      );
    }
    return const String.fromEnvironment(
      'API_BASE',
      defaultValue: 'http://localhost:8009',
    );
  }

  static String mediaUrl(String? path) {
    if (path == null || path.isEmpty) return '';
    if (path.startsWith('http')) return path;
    return '$baseUrl$path';
  }

  static String get wsBaseUrl {
    if (baseUrl.startsWith('https://')) {
      return 'wss://${baseUrl.substring('https://'.length)}';
    }
    if (baseUrl.startsWith('http://')) {
      return 'ws://${baseUrl.substring('http://'.length)}';
    }
    return baseUrl;
  }
}
