import 'package:flutter/foundation.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';

import 'realtime_service.dart';

/// WebRTC mesh for live battles — matches web `battle-live.html` signaling.
class BattleWebRtcController {
  BattleWebRtcController({
    required this.battleId,
    required this.myUserId,
    required this.isBroadcaster,
    this.onChanged,
    this.onError,
  });

  final int battleId;
  final int myUserId;
  final bool isBroadcaster;
  final VoidCallback? onChanged;
  final void Function(String message)? onError;

  static const _ice = {
    'iceServers': [
      {'urls': 'stun:stun.l.google.com:19302'},
      {'urls': 'stun:stun1.l.google.com:19302'},
      {'urls': 'stun:stun2.l.google.com:19302'},
      {'urls': 'stun:stun3.l.google.com:19302'},
      {'urls': 'stun:stun4.l.google.com:19302'},
    ],
  };

  final RTCVideoRenderer localRenderer = RTCVideoRenderer();
  final Map<int, RTCVideoRenderer> remoteRenderers = {};
  final Map<int, RTCPeerConnection> _pcs = {};
  final Map<int, List<dynamic>> _iceQueues = {};
  final List<int> _pendingViewerRequests = [];

  MediaStream? localStream;
  bool cameraOn = true;
  bool micOn = true;
  bool ready = false;
  String? mediaError;
  bool _disposed = false;
  bool _signalBound = false;

  String get _signalTopic => '/topic/battle/$battleId/signal';
  String get _signalUserTopic => '/topic/battle/$battleId/signal/$myUserId';
  String get _signalApp => '/app/battle/$battleId/signal';

  Future<void> init() async {
    await localRenderer.initialize();
    await _bindSignals();
    if (isBroadcaster) {
      await startLocalMedia();
    } else {
      _sendSignal({'type': 'who-is-broadcasting', 'userId': myUserId});
    }
  }

  Future<void> _bindSignals() async {
    if (_signalBound) return;
    _signalBound = true;
    Future<void> handle(Map<String, dynamic> data) async {
      if (_disposed) return;
      try {
        await _onSignal(data);
      } catch (e) {
        debugPrint('WebRTC signal error: $e');
      }
    }

    await RealtimeService.instance.subscribeJson(_signalTopic, handle);
    await RealtimeService.instance.subscribeJson(_signalUserTopic, handle);
  }

  Future<void> startLocalMedia() async {
    mediaError = null;
    try {
      final stream = await navigator.mediaDevices.getUserMedia({
        'audio': true,
        'video': {
          'facingMode': 'user',
          'width': {'ideal': 640, 'max': 1280},
          'height': {'ideal': 480, 'max': 720},
          'frameRate': {'ideal': 20, 'max': 24},
        },
      });
      if (_disposed) {
        await stream.dispose();
        return;
      }
      await localStream?.dispose();
      localStream = stream;
      localRenderer.srcObject = stream;
      ready = true;
      cameraOn = true;
      micOn = true;
      _notify();
      _sendSignal({'type': 'broadcaster-ready', 'userId': myUserId});
      publishMediaStatus();

      final queued = List<int>.from(_pendingViewerRequests);
      _pendingViewerRequests.clear();
      for (final requesterId in queued) {
        await _sendOfferTo(requesterId);
      }
    } catch (e) {
      mediaError = 'Could not access camera/microphone: $e';
      ready = false;
      onError?.call(mediaError!);
      _notify();
    }
  }

  void publishMediaStatus() {
    RealtimeService.instance.send('/app/battle/$battleId/media-status', {
      'camera': cameraOn,
      'mic': micOn,
      'cameraOn': cameraOn,
      'micOn': micOn,
    });
  }

  Future<void> toggleCamera() async {
    final stream = localStream;
    if (stream == null) {
      onError?.call('Camera not available');
      return;
    }
    cameraOn = !cameraOn;
    for (final t in stream.getVideoTracks()) {
      t.enabled = cameraOn;
    }
    publishMediaStatus();
    _notify();
  }

  Future<void> toggleMic() async {
    final stream = localStream;
    if (stream == null) {
      onError?.call('Microphone not available');
      return;
    }
    micOn = !micOn;
    for (final t in stream.getAudioTracks()) {
      t.enabled = micOn;
    }
    publishMediaStatus();
    _notify();
  }

  Future<void> switchCamera() async {
    final stream = localStream;
    if (stream == null) return;
    final tracks = stream.getVideoTracks();
    if (tracks.isEmpty) return;
    try {
      await Helper.switchCamera(tracks.first);
    } catch (e) {
      debugPrint('switchCamera: $e');
    }
  }

  /// Screen share (matches web shareScreen). Falls back with error if unsupported.
  Future<void> shareScreen() async {
    try {
      final screenStream = await navigator.mediaDevices.getDisplayMedia({
        'video': true,
        'audio': true,
      });
      if (_disposed) {
        await screenStream.dispose();
        return;
      }
      final videoTrack = screenStream.getVideoTracks().isNotEmpty
          ? screenStream.getVideoTracks().first
          : null;
      if (videoTrack == null) {
        await screenStream.dispose();
        onError?.call('No screen video track');
        return;
      }
      localRenderer.srcObject = screenStream;
      for (final pc in _pcs.values) {
        final senders = await pc.getSenders();
        for (final sender in senders) {
          if (sender.track?.kind == 'video') {
            await sender.replaceTrack(videoTrack);
          }
        }
      }
      videoTrack.onEnded = () async {
        final cam = localStream?.getVideoTracks();
        if (cam != null && cam.isNotEmpty) {
          localRenderer.srcObject = localStream;
          for (final pc in _pcs.values) {
            final senders = await pc.getSenders();
            for (final sender in senders) {
              if (sender.track?.kind == 'video') {
                await sender.replaceTrack(cam.first);
              }
            }
          }
        }
        await screenStream.dispose();
        _notify();
      };
      _notify();
    } catch (e) {
      onError?.call('Screen share failed: $e');
    }
  }

  void _sendSignal(Map<String, dynamic> data) {
    RealtimeService.instance.send(_signalApp, data);
  }

  int? _asInt(dynamic v) {
    if (v is int) return v;
    if (v is num) return v.toInt();
    if (v is String) return int.tryParse(v);
    return null;
  }

  Future<void> _onSignal(Map<String, dynamic> data) async {
    final fromId = _asInt(data['userId']);
    if (fromId == null || fromId == myUserId) return;
    final type = data['type']?.toString();
    final targetId = _asInt(data['targetUserId']);

    if (type == 'who-is-broadcasting' && localStream != null) {
      _sendSignal({
        'type': 'broadcaster-ready',
        'userId': myUserId,
        'targetUserId': fromId,
      });
      return;
    }

    if (type == 'broadcaster-ready' && (targetId == null || targetId == myUserId)) {
      _sendSignal({
        'type': 'viewer-request',
        'targetUserId': fromId,
        'userId': myUserId,
      });
      return;
    }

    if (type == 'viewer-request' && targetId == myUserId) {
      if (localStream != null) {
        await _sendOfferTo(fromId);
      } else if (isBroadcaster) {
        if (!_pendingViewerRequests.contains(fromId)) {
          _pendingViewerRequests.add(fromId);
        }
      }
      return;
    }

    if (type == 'offer' && targetId == myUserId) {
      final offerMap = data['offer'];
      if (offerMap is! Map) return;
      final pc = await _createPeerConnection(fromId);
      await pc.setRemoteDescription(
        RTCSessionDescription(
          offerMap['sdp']?.toString(),
          offerMap['type']?.toString(),
        ),
      );
      await _flushIce(fromId, pc);
      final answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);
      _sendSignal({
        'type': 'answer',
        'answer': {'type': answer.type, 'sdp': answer.sdp},
        'targetUserId': fromId,
        'userId': myUserId,
      });
      return;
    }

    if (type == 'answer' && targetId == myUserId) {
      final pc = _pcs[fromId];
      final answerMap = data['answer'];
      if (pc == null || answerMap is! Map) return;
      await pc.setRemoteDescription(
        RTCSessionDescription(
          answerMap['sdp']?.toString(),
          answerMap['type']?.toString(),
        ),
      );
      await _flushIce(fromId, pc);
      return;
    }

    if (type == 'candidate' && targetId == myUserId) {
      final cand = data['candidate'];
      if (cand == null) return;
      final pc = _pcs[fromId];
      if (pc == null) {
        _iceQueues.putIfAbsent(fromId, () => []).add(cand);
        return;
      }
      final desc = await pc.getRemoteDescription();
      if (desc == null || (desc.type ?? '').isEmpty) {
        _iceQueues.putIfAbsent(fromId, () => []).add(cand);
      } else {
        await _addCandidate(pc, cand);
      }
    }
  }

  Future<void> _sendOfferTo(int remoteUserId) async {
    final pc = await _createPeerConnection(remoteUserId);
    final offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    _sendSignal({
      'type': 'offer',
      'offer': {'type': offer.type, 'sdp': offer.sdp},
      'targetUserId': remoteUserId,
      'userId': myUserId,
    });
  }

  Future<RTCPeerConnection> _createPeerConnection(int remoteUserId) async {
    final existing = _pcs[remoteUserId];
    if (existing != null) {
      await existing.close();
      _pcs.remove(remoteUserId);
    }

    final pc = await createPeerConnection(_ice);
    _pcs[remoteUserId] = pc;
    _iceQueues.putIfAbsent(remoteUserId, () => []);

    final stream = localStream;
    if (stream != null) {
      for (final track in stream.getTracks()) {
        await pc.addTrack(track, stream);
      }
    }

    pc.onTrack = (event) async {
      if (_disposed || event.streams.isEmpty) return;
      var renderer = remoteRenderers[remoteUserId];
      if (renderer == null) {
        renderer = RTCVideoRenderer();
        await renderer.initialize();
        if (_disposed) {
          await renderer.dispose();
          return;
        }
        remoteRenderers[remoteUserId] = renderer;
      }
      renderer.srcObject = event.streams[0];
      _notify();
    };

    pc.onIceCandidate = (candidate) {
      _sendSignal({
        'type': 'candidate',
        'candidate': {
          'candidate': candidate.candidate,
          'sdpMid': candidate.sdpMid,
          'sdpMLineIndex': candidate.sdpMLineIndex,
        },
        'targetUserId': remoteUserId,
        'userId': myUserId,
      });
    };

    pc.onConnectionState = (state) {
      debugPrint('PC $remoteUserId state=$state');
    };

    return pc;
  }

  Future<void> _flushIce(int remoteUserId, RTCPeerConnection pc) async {
    final queue = _iceQueues[remoteUserId];
    if (queue == null || queue.isEmpty) return;
    final copy = List<dynamic>.from(queue);
    queue.clear();
    for (final cand in copy) {
      await _addCandidate(pc, cand);
    }
  }

  Future<void> _addCandidate(RTCPeerConnection pc, dynamic cand) async {
    try {
      if (cand is Map) {
        await pc.addCandidate(
          RTCIceCandidate(
            cand['candidate']?.toString(),
            cand['sdpMid']?.toString(),
            cand['sdpMLineIndex'] is int
                ? cand['sdpMLineIndex'] as int
                : int.tryParse('${cand['sdpMLineIndex']}'),
          ),
        );
      }
    } catch (e) {
      debugPrint('addCandidate error: $e');
    }
  }

  void _notify() {
    if (!_disposed) onChanged?.call();
  }

  Future<void> dispose() async {
    _disposed = true;
    for (final pc in _pcs.values) {
      await pc.close();
    }
    _pcs.clear();
    for (final r in remoteRenderers.values) {
      await r.dispose();
    }
    remoteRenderers.clear();
    await localStream?.dispose();
    localStream = null;
    await localRenderer.dispose();
  }
}
