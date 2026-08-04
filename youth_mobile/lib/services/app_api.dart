import 'package:dio/dio.dart';
import '../models/event_model.dart';
import '../models/post_model.dart';
import '../models/user_model.dart';
import 'api_client.dart';

class AppApi {
  static Dio get _dio => ApiClient.instance.dio;

  static Future<List<PostModel>> feed({int page = 0, int size = 10}) async {
    final res = await _dio.get('/api/mobile/feed', queryParameters: {'page': page, 'size': size});
    return (res.data as List).map((e) => PostModel.fromJson(Map<String, dynamic>.from(e as Map))).toList();
  }

  static Future<Map<String, dynamic>> likePost(int postId) async {
    final res = await _dio.post('/api/posts/$postId/like');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> commentPost(int postId, String text) async {
    final res = await _dio.post(
      '/api/posts/$postId/comment',
      queryParameters: {'content': text},
    );
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<List<dynamic>> getComments(int postId) async {
    final res = await _dio.get('/api/posts/$postId/comments');
    return res.data as List<dynamic>;
  }

  static Future<PostModel> createPost({
    required String content,
    String postType = 'POST',
    String? hashtags,
    String? category,
    MultipartFile? file,
  }) async {
    final form = FormData.fromMap({
      'content': content,
      'postType': postType,
      if (hashtags != null) 'hashtags': hashtags,
      if (category != null) 'category': category,
      if (file != null) 'file': file,
    });
    final res = await _dio.post('/api/mobile/posts', data: form);
    return PostModel.fromJson(Map<String, dynamic>.from(res.data as Map));
  }

  static Future<List<Map<String, dynamic>>> exploreUsers({String name = '', String college = ''}) async {
    final res = await _dio.get('/api/users/explore', queryParameters: {
      if (name.isNotEmpty) 'name': name,
      if (college.isNotEmpty) 'college': college,
    });
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<AppUser> profile(String username) async {
    final res = await _dio.get('/api/mobile/profile/$username');
    return AppUser.fromJson(Map<String, dynamic>.from(res.data as Map));
  }

  static Future<AppUser> updateProfile(Map<String, dynamic> body) async {
    final res = await _dio.post('/api/mobile/profile/update', data: body);
    return AppUser.fromJson(Map<String, dynamic>.from(res.data as Map));
  }

  static Future<void> follow(int userId) => _dio.post('/api/mobile/profile/$userId/follow');
  static Future<void> unfollow(int userId) => _dio.post('/api/mobile/profile/$userId/unfollow');

  static Future<Map<String, dynamic>> events({String? category, String? search}) async {
    final res = await _dio.get('/api/mobile/events', queryParameters: {
      if (category != null) 'category': category,
      if (search != null && search.isNotEmpty) 'search': search,
    });
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<EventModel> eventDetail(int id) async {
    final res = await _dio.get('/api/mobile/events/$id');
    return EventModel.fromJson(Map<String, dynamic>.from(res.data as Map));
  }

  static Future<Map<String, dynamic>> registerEvent(int id) async {
    final res = await _dio.post('/api/mobile/events/$id/register');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<List<BookingModel>> bookings() async {
    final res = await _dio.get('/api/mobile/bookings');
    return (res.data as List).map((e) => BookingModel.fromJson(Map<String, dynamic>.from(e as Map))).toList();
  }

  static Future<void> cancelBooking(int id) => _dio.post('/api/mobile/bookings/$id/cancel');

  static Future<Map<String, dynamic>> battles() async {
    final res = await _dio.get('/api/mobile/battles');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> wallet() async {
    final res = await _dio.get('/api/mobile/wallet');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> walletAdd(double amount) async {
    final res = await _dio.post('/api/mobile/wallet/add', data: {'amount': amount});
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> walletWithdraw(double amount) async {
    final res = await _dio.post('/api/mobile/wallet/withdraw', data: {'amount': amount});
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> shop() async {
    final res = await _dio.get('/api/mobile/shop');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> shopBuy(String itemId) async {
    final res = await _dio.post('/api/mobile/shop/buy/$itemId');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<List<Map<String, dynamic>>> notifications() async {
    final res = await _dio.get('/api/mobile/notifications');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<void> markNotificationsRead() => _dio.post('/api/mobile/notifications/mark-all-read');

  static Future<Map<String, dynamic>> achievements() async {
    final res = await _dio.get('/api/mobile/achievements');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<List<Map<String, dynamic>>> musicTracks() async {
    final res = await _dio.get('/api/mobile/music/tracks');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<List<Map<String, dynamic>>> musicRooms() async {
    final res = await _dio.get('/api/mobile/music/rooms');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<List<Map<String, dynamic>>> reels({required int userId, int limit = 20}) async {
    final res = await _dio.get('/api/reels/feed', queryParameters: {'userId': userId, 'limit': limit});
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<void> reelInteract({
    required int reelId,
    required int userId,
    required String action,
  }) async {
    await _dio.post('/api/reels/$reelId/interact', data: {
      'userId': userId,
      'interactionType': action,
      'watchDuration': action == 'VIEW' ? 1 : 0,
    });
  }

  static Future<List<Map<String, dynamic>>> conversations() async {
    final res = await _dio.get('/api/chat/conversations');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<List<Map<String, dynamic>>> chatHistory(int conversationId) async {
    final res = await _dio.get('/api/chat/history/$conversationId');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<Map<String, dynamic>> sendDirect({required int receiverId, required String content}) async {
    final res = await _dio.post('/api/chat/send-direct', data: {
      'recipientId': receiverId,
      'content': content,
    });
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> createGameRoom(String game, {String? playerName}) async {
    final res = await _dio.post('/api/$game/create', data: {
      if (playerName != null) 'playerName': playerName,
    });
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> joinGameRoom(String game, {required String roomId, String? playerName}) async {
    final res = await _dio.post('/api/$game/join', data: {
      'roomId': roomId,
      if (playerName != null) 'playerName': playerName,
    });
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<List<Map<String, dynamic>>> heatmapEvents() async {
    final res = await _dio.get('/api/heatmap/events');
    final data = res.data;
    if (data is List) {
      return data.map((e) => Map<String, dynamic>.from(e as Map)).toList();
    }
    if (data is Map) {
      final map = Map<String, dynamic>.from(data);
      for (final key in ['events', 'points', 'data', 'markers']) {
        final v = map[key];
        if (v is List) {
          return v.map((e) => Map<String, dynamic>.from(e as Map)).toList();
        }
      }
      // Flatten map-of-lists responses
      final out = <Map<String, dynamic>>[];
      for (final entry in map.entries) {
        if (entry.value is List) {
          for (final e in entry.value as List) {
            if (e is Map) {
              final m = Map<String, dynamic>.from(e);
              m.putIfAbsent('group', () => entry.key);
              out.add(m);
            }
          }
        }
      }
      return out;
    }
    return [];
  }

  static Future<List<Map<String, dynamic>>> stories() async {
    final res = await _dio.get('/api/mobile/stories');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<List<Map<String, dynamic>>> followRequests() async {
    final res = await _dio.get('/api/mobile/profile/follow-requests');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<void> acceptFollowRequest(int id) =>
      _dio.post('/api/mobile/profile/follow-requests/$id/accept');

  static Future<void> rejectFollowRequest(int id) =>
      _dio.post('/api/mobile/profile/follow-requests/$id/reject');

  static Future<List<Map<String, dynamic>>> collaborationRequests() async {
    final res = await _dio.get('/api/mobile/profile/collaboration-requests');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<void> acceptCollaboration(int id) =>
      _dio.post('/api/mobile/profile/collaboration/$id/accept');

  static Future<void> rejectCollaboration(int id) =>
      _dio.post('/api/mobile/profile/collaboration/$id/reject');

  static Future<List<Map<String, dynamic>>> rewards() async {
    final res = await _dio.get('/api/mobile/rewards');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<void> revealReward(int registrationId) =>
      _dio.post('/api/mobile/rewards/reveal/$registrationId');

  static Future<void> redeemReward(String rewardCode) =>
      _dio.post('/api/mobile/rewards/redeem/$rewardCode');

  static Future<Map<String, dynamic>> ticket(String ticketId) async {
    final res = await _dio.get('/api/mobile/ticket/$ticketId');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<void> deleteNotification(int id) =>
      _dio.post('/api/mobile/notifications/$id/delete');

  static Future<void> clearNotifications() =>
      _dio.post('/api/mobile/notifications/clear-all');

  static Future<Map<String, dynamic>> redeemCoupon(String code) async {
    final res = await _dio.post('/api/mobile/coupon/redeem', data: {'code': code});
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<void> contact({
    required String name,
    required String email,
    required String message,
    String subject = 'Mobile contact',
  }) async {
    await _dio.post(
      '/contact',
      data: {
        'name': name,
        'email': email,
        'subject': subject,
        'message': message,
      },
      options: Options(contentType: Headers.formUrlEncodedContentType),
    );
  }
}
