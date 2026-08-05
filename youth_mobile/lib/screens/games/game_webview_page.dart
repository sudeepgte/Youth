import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_android/webview_flutter_android.dart';
import 'package:webview_flutter_wkwebview/webview_flutter_wkwebview.dart';

import '../../config/api_config.dart';
import '../../services/api_client.dart';
import '../../theme/app_theme.dart';
import '../../widgets/app_drawer.dart';
import 'game_room_live_page.dart';

/// Loads the exact web game pages (same HTML/JS/WebSocket flow as desktop).
class GameWebViewPage extends StatefulWidget {
  const GameWebViewPage({
    super.key,
    required this.title,
    required this.path,
    this.query = const {},
    this.gameKey,
    this.roomId,
  });

  final String title;
  /// App path e.g. `/games`, `/play-chess`, `/play-mario`
  final String path;
  final Map<String, String> query;
  final String? gameKey;
  final String? roomId;

  @override
  State<GameWebViewPage> createState() => _GameWebViewPageState();
}

class _GameWebViewPageState extends State<GameWebViewPage> {
  WebViewController? _controller;
  var _loading = true;
  var _progress = 0;
  String? _error;
  String? _currentUrl;

  /// Hide the web dashboard sidebar so only the shared Flutter drawer is used.
  static const _hideWebChromeJs = '''
(function(){
  try {
    var style = document.getElementById('youthian-flutter-hide');
    if (!style) {
      style = document.createElement('style');
      style.id = 'youthian-flutter-hide';
      style.textContent = `
        .sidebar, .mobile-menu-btn, .mobile-menu-overlay,
        .dashboard-nav, header.dashboard-header, .top-navbar {
          display: none !important;
        }
        .app-container, .dashboard-layout, .main-content, .content-wrapper {
          margin-left: 0 !important;
          padding-left: 0 !important;
          width: 100% !important;
          max-width: 100% !important;
        }
      `;
      document.head.appendChild(style);
    }
    document.querySelectorAll('.sidebar.mobile-open').forEach(function(el){
      el.classList.remove('mobile-open');
    });
  } catch (e) {}
})();
''';

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _hideWebChrome(WebViewController controller) async {
    try {
      await controller.runJavaScript(_hideWebChromeJs);
    } catch (_) {}
  }

  Future<void> _init() async {
    try {
      final token = await ApiClient.instance.getToken();
      final base = Uri.parse(ApiConfig.baseUrl);
      final path = widget.path.startsWith('/') ? widget.path : '/${widget.path}';
      final qp = <String, String>{...widget.query};
      if (token != null && token.isNotEmpty) {
        qp['auth'] = token;
      }
      final url = Uri(
        scheme: base.scheme,
        host: base.host,
        port: base.hasPort ? base.port : null,
        path: path,
        queryParameters: qp.isEmpty ? null : qp,
      );
      _currentUrl = url.toString();

      if (token != null && token.isNotEmpty) {
        final cookieManager = WebViewCookieManager();
        await cookieManager.setCookie(
          WebViewCookie(
            name: 'jwtToken',
            value: token,
            domain: base.host,
            path: '/',
          ),
        );
      }

      late final PlatformWebViewControllerCreationParams params;
      if (WebViewPlatform.instance is WebKitWebViewPlatform) {
        params = WebKitWebViewControllerCreationParams(
          allowsInlineMediaPlayback: true,
          mediaTypesRequiringUserAction: const <PlaybackMediaTypes>{},
        );
      } else {
        params = const PlatformWebViewControllerCreationParams();
      }

      final controller = WebViewController.fromPlatformCreationParams(params);
      controller
        ..setJavaScriptMode(JavaScriptMode.unrestricted)
        ..setBackgroundColor(Colors.white)
        ..setNavigationDelegate(
          NavigationDelegate(
            onProgress: (p) {
              if (mounted) setState(() => _progress = p);
            },
            onPageStarted: (_) {
              if (mounted) setState(() => _loading = true);
            },
            onPageFinished: (_) async {
              await _hideWebChrome(controller);
              if (mounted) setState(() => _loading = false);
            },
            onWebResourceError: (err) {
              if (mounted) {
                setState(() {
                  _loading = false;
                  _error = err.description;
                });
              }
            },
            onNavigationRequest: (req) {
              final uri = Uri.tryParse(req.url);
              if (uri == null) return NavigationDecision.prevent;
              if (uri.host == base.host ||
                  uri.host.isEmpty ||
                  req.url.startsWith('about:') ||
                  req.url.startsWith('blob:') ||
                  req.url.startsWith('data:')) {
                return NavigationDecision.navigate;
              }
              return NavigationDecision.navigate;
            },
          ),
        );

      if (controller.platform is AndroidWebViewController) {
        final android = controller.platform as AndroidWebViewController;
        await android.setMediaPlaybackRequiresUserGesture(false);
      }

      await controller.loadRequest(url);
      if (!mounted) return;
      setState(() {
        _controller = controller;
        _error = null;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString();
        _loading = false;
      });
    }
  }

  Future<void> _reload() async {
    setState(() {
      _error = null;
      _loading = true;
    });
    if (_controller != null && _currentUrl != null) {
      await _controller!.loadRequest(Uri.parse(_currentUrl!));
    } else {
      await _init();
    }
  }

  @override
  Widget build(BuildContext context) {
    return DashboardScaffold(
      active: AppDrawerItem.games,
      title: widget.title,
      actions: [
        if (widget.gameKey != null && (widget.roomId ?? widget.query['room']) != null)
          IconButton(
            tooltip: 'Room live controls',
            onPressed: () {
              final room = widget.roomId ?? widget.query['room']!;
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => GameRoomLivePage(game: widget.gameKey!, roomId: room),
                ),
              );
            },
            icon: const Icon(Icons.sports_esports_outlined, color: AppTheme.textPrimary),
          ),
        IconButton(
          onPressed: _reload,
          icon: const Icon(Icons.refresh, color: AppTheme.textPrimary),
        ),
      ],
      body: Stack(
        children: [
          if (_controller != null) WebViewWidget(controller: _controller!),
          if (_loading)
            LinearProgressIndicator(
              value: _progress > 0 && _progress < 100 ? _progress / 100 : null,
              color: AppTheme.primary,
              backgroundColor: Colors.black12,
            ),
          if (_error != null)
            Positioned.fill(
              child: ColoredBox(
                color: AppTheme.dashboardBg,
                child: Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.wifi_off, size: 48, color: Colors.black45),
                        const SizedBox(height: 12),
                        Text(
                          'Could not load game.\nMake sure the server is running on ${ApiConfig.baseUrl}',
                          textAlign: TextAlign.center,
                          style: GoogleFonts.inter(),
                        ),
                        const SizedBox(height: 8),
                        Text(_error!, textAlign: TextAlign.center, style: GoogleFonts.inter(fontSize: 12, color: Colors.red)),
                        const SizedBox(height: 16),
                        ElevatedButton(onPressed: _reload, child: const Text('Retry')),
                      ],
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
