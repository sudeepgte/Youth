import 'package:dio/dio.dart';
import '../models/event_model.dart';
import '../models/post_model.dart';
import '../models/user_model.dart';
import 'api_client.dart';

class AppApi {
  static Dio get _dio => ApiClient.instance.dio;

  static Future<List<PostModel>> feed({int page = 0, int size = 10, String? category}) async {
    final res = await _dio.get('/api/mobile/feed', queryParameters: {
      'page': page,
      'size': size,
      if (category != null && category.isNotEmpty && category != 'All') 'category': category,
    });
    return (res.data as List).map((e) => PostModel.fromJson(Map<String, dynamic>.from(e as Map))).toList();
  }

  static Future<Map<String, dynamic>> likePost(int postId) async {
    final res = await _dio.post('/api/posts/$postId/like');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> savePost(int postId) async {
    final res = await _dio.post('/api/posts/$postId/save');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> postStats(int postId) async {
    final res = await _dio.get('/api/posts/$postId/stats');
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

  static Future<Map<String, dynamic>> editPost(int postId, {required String content, String? hashtags}) async {
    final res = await _dio.post(
      '/api/posts/$postId/edit',
      queryParameters: {
        'content': content,
        if (hashtags != null) 'hashtags': hashtags,
      },
    );
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<PostModel> createPost({
    required String content,
    String postType = 'POST',
    String? hashtags,
    String? category,
    String? collaborators,
    String? bgColor,
    String? textColor,
    MultipartFile? file,
  }) async {
    final form = FormData.fromMap({
      'content': content,
      'postType': postType,
      if (hashtags != null) 'hashtags': hashtags,
      if (category != null) 'category': category,
      if (collaborators != null) 'collaborators': collaborators,
      if (bgColor != null) 'bgColor': bgColor,
      if (textColor != null) 'textColor': textColor,
      if (file != null) 'file': file,
    });
    final res = await _dio.post('/api/mobile/posts', data: form);
    return PostModel.fromJson(Map<String, dynamic>.from(res.data as Map));
  }

  static Future<void> deletePost(int postId) => _dio.post('/profile/post/delete/$postId');

  static Future<List<Map<String, dynamic>>> userStories(int userId) async {
    final res = await _dio.get('/api/stories/user/$userId');
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
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

  static Future<Map<String, dynamic>> joinOnlineEvent(int id) async {
    final res = await _dio.post('/api/mobile/events/$id/join-online');
    return Map<String, dynamic>.from(res.data as Map? ?? {});
  }

  static Future<Map<String, dynamic>> userPreview(int userId) async {
    final res = await _dio.get('/api/users/preview/$userId');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> registerEvent(int id) async {
    final res = await _dio.post('/api/mobile/events/$id/register');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> pollVote(int id) async {
    final res = await _dio.post('/api/mobile/events/$id/poll-vote');
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

  static Future<Map<String, dynamic>> sendDirect({
    required int receiverId,
    required String content,
    String? mediaUrl,
  }) async {
    final res = await _dio.post('/api/chat/send-direct', data: {
      'recipientId': receiverId,
      'content': content,
      if (mediaUrl != null && mediaUrl.isNotEmpty) 'mediaUrl': mediaUrl,
    });
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> sharePostToUser({
    required int postId,
    required int recipientId,
  }) async {
    final res = await _dio.post('/api/chat/share-post-to-user', data: {
      'postId': postId,
      'recipientId': recipientId,
    });
    return Map<String, dynamic>.from(res.data as Map? ?? {'ok': true});
  }

  static Future<void> markChatSeen(int conversationId) =>
      _dio.post('/api/chat/mark-seen/$conversationId');

  static Future<int> unreadChatCount() async {
    final res = await _dio.get('/api/chat/unread-count');
    final data = res.data;
    if (data is Map) return (data['count'] as num?)?.toInt() ?? (data['unread'] as num?)?.toInt() ?? 0;
    if (data is num) return data.toInt();
    return 0;
  }

  static Future<String> uploadChatMedia(String filePath, {String filename = 'chat-media.jpg'}) async {
    final form = FormData.fromMap({
      'file': await MultipartFile.fromFile(filePath, filename: filename),
    });
    // Do not set Content-Type manually — Dio must add the multipart boundary
    final res = await _dio.post(
      '/api/chat/upload',
      data: form,
      options: Options(
        sendTimeout: const Duration(seconds: 60),
        receiveTimeout: const Duration(seconds: 60),
      ),
    );
    final data = res.data;
    if (data is Map && data['url'] != null) return data['url'].toString();
    if (data is Map && data['error'] != null) throw Exception(data['error'].toString());
    throw Exception('Upload failed');
  }

  static Future<Map<String, dynamic>> createGroupChat({
    required String name,
    required List<int> participantIds,
  }) async {
    final res = await _dio.post('/api/chat/create-group', data: {
      'name': name,
      'participantIds': participantIds,
    });
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> acceptChatRequest(int conversationId) async {
    final res = await _dio.post('/api/chat/accept/$conversationId');
    return res.data is Map ? Map<String, dynamic>.from(res.data as Map) : {'ok': true};
  }

  static Future<void> rejectChatRequest(int conversationId) =>
      _dio.post('/api/chat/reject/$conversationId');

  static Future<List<Map<String, dynamic>>> chatUsers({String query = ''}) async {
    final res = query.isEmpty
        ? await _dio.get('/api/chat/users')
        : await _dio.get('/api/chat/search-users', queryParameters: {'query': query});
    final data = res.data;
    if (data is List) {
      return data.map((e) => Map<String, dynamic>.from(e as Map)).toList();
    }
    if (data is Map && data['users'] is List) {
      return (data['users'] as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
    }
    return [];
  }

  static Future<Map<String, dynamic>> battleDetail(int id) async {
    final res = await _dio.get('/api/mobile/battles/$id');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static String _battleErrorMessage(String? code) {
    switch (code) {
      case 'not_found':
        return 'Battle not found';
      case 'full':
        return 'Battle is full';
      case 'ended':
        return 'Battle has ended';
      case 'already_started':
        return 'Joining is closed — battle already started';
      case 'already_voted':
        return 'You already voted';
      case 'already_submitted':
        return 'You already submitted';
      case 'insufficient_funds':
        return 'Insufficient wallet balance for entry fee';
      case 'invalid_prizes':
        return 'Prize pool cannot exceed entry fee × max participants';
      case 'empty_submission':
        return 'Submission URL is required';
      case 'not_active':
        return 'Battle is not active';
      case 'not_participant':
        return 'You are not a participant';
      case 'unauthorized':
        return 'Not allowed';
      default:
        return code != null && code.isNotEmpty ? code : 'Battle request failed';
    }
  }

  /// Form POST to web battle endpoints; parses redirect query errors.
  static Future<Map<String, dynamic>> _battleFormPost(
    String path, [
    Map<String, dynamic>? fields,
  ]) async {
    final res = await _dio.post(
      path,
      data: fields ?? <String, dynamic>{},
      options: Options(
        contentType: Headers.formUrlEncodedContentType,
        followRedirects: false,
        validateStatus: (s) => s != null && (s < 400 || s == 302 || s == 303 || s == 307),
      ),
    );
    final loc = res.headers.value('location') ?? res.headers.value('Location') ?? '';
    if (loc.isNotEmpty) {
      final uri = Uri.parse(loc.startsWith('http') ? loc : 'http://local$loc');
      final err = uri.queryParameters['error'];
      if (err != null && err.isNotEmpty) {
        throw Exception(_battleErrorMessage(err));
      }
      final match = RegExp(r'/battles/(\d+)').firstMatch(loc);
      if (match != null) {
        return {'ok': true, 'battleId': int.parse(match.group(1)!)};
      }
      if (loc.contains('/pay')) {
        final payMatch = RegExp(r'/battles/(\d+)/pay').firstMatch(loc);
        if (payMatch != null) {
          return {'ok': true, 'needsPayment': true, 'battleId': int.parse(payMatch.group(1)!)};
        }
      }
    }
    if (res.data is Map) return Map<String, dynamic>.from(res.data as Map);
    return {'ok': true};
  }

  static Future<Map<String, dynamic>> createBattle(Map<String, dynamic> fields) =>
      _battleFormPost('/battles/create', fields);

  static Future<Map<String, dynamic>> joinBattle({required String roomCode}) =>
      _battleFormPost('/battles/join', {'roomCode': roomCode});

  static Future<Map<String, dynamic>> startBattle(int id) =>
      _battleFormPost('/battles/$id/start');

  static Future<Map<String, dynamic>> processBattlePayment(int id) =>
      _battleFormPost('/battles/$id/process-payment');

  static Future<Map<String, dynamic>> registerOfflineBattle(int id) =>
      _battleFormPost('/battles/$id/register');

  static Future<Map<String, dynamic>> startBattleVoting(int id) =>
      _battleFormPost('/battles/$id/start-voting');

  static Future<Map<String, dynamic>> submitBattleEntry(
    int id, {
    required String submissionUrl,
    String? description,
    String? secondaryUrl,
  }) {
    final data = <String, dynamic>{'submissionUrl': submissionUrl};
    if (description != null && description.isNotEmpty) data['description'] = description;
    if (secondaryUrl != null && secondaryUrl.isNotEmpty) data['secondaryUrl'] = secondaryUrl;
    return _battleFormPost('/battles/$id/submit', data);
  }

  static Future<Map<String, dynamic>> voteBattleSubmission(int battleId, int submissionId) =>
      _battleFormPost('/battles/$battleId/vote/$submissionId');

  static Future<Map<String, dynamic>> voteBattleParticipant(int battleId, int participantId) =>
      _battleFormPost('/battles/$battleId/vote-participant/$participantId');

  static Future<Map<String, dynamic>> endBattle(int id) =>
      _battleFormPost('/battles/$id/end');

  static Future<Map<String, dynamic>> leaveBattle(int id) =>
      _battleFormPost('/battles/$id/leave');

  static Future<Map<String, dynamic>> deleteBattle(int id) =>
      _battleFormPost('/battles/$id/delete');

  static Future<Map<String, dynamic>> heatmapPayload({String filter = 'all'}) async {
    final res = await _dio.get('/api/heatmap/events', queryParameters: {'filter': filter});
    if (res.data is Map) return Map<String, dynamic>.from(res.data as Map);
    if (res.data is List) {
      return {'markers': res.data, 'heatPoints': [], 'stats': {}};
    }
    return {};
  }

  static Future<List<Map<String, dynamic>>> heatmapEvents({String filter = 'all'}) async {
    final payload = await heatmapPayload(filter: filter);
    final markers = payload['markers'];
    if (markers is List) {
      return markers.map((e) => Map<String, dynamic>.from(e as Map)).toList();
    }
    return [];
  }

  static Future<Map<String, dynamic>> registerEventWithFields(int id, Map<String, dynamic> fields) async {
    final res = await _dio.post(
      '/api/mobile/events/$id/register',
      data: fields,
    );
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<void> resetPassword(String newPassword) async {
    await _dio.post(
      '/profile/reset-password',
      data: {'newPassword': newPassword},
      options: Options(contentType: Headers.formUrlEncodedContentType),
    );
  }

  static Future<void> likeMusicTrack(int trackId) => _dio.post('/music/$trackId/like');

  static Future<Map<String, dynamic>> musicLeaderboard() async {
    final res = await _dio.get('/api/mobile/music/leaderboard');
    return Map<String, dynamic>.from(res.data as Map? ?? {});
  }

  static Future<Map<String, dynamic>> recordMusicListen(int trackId, int seconds) async {
    final res = await _dio.post(
      '/api/music/$trackId/listen',
      queryParameters: {'seconds': seconds},
    );
    return Map<String, dynamic>.from(res.data as Map? ?? {});
  }

  static Future<void> uploadMusicTrack({
    required String title,
    required String artistName,
    required String licenseName,
    String? licenseUrl,
    required MultipartFile file,
  }) async {
    final form = FormData.fromMap({
      'title': title,
      'artistName': artistName,
      'licenseName': licenseName,
      if (licenseUrl != null && licenseUrl.isNotEmpty) 'licenseUrl': licenseUrl,
      'acceptTerms': 'true',
      'file': file,
    });
    await _dio.post(
      '/music/upload',
      data: form,
      options: Options(
        followRedirects: false,
        validateStatus: (s) => s != null && (s < 400 || s == 302 || s == 303),
      ),
    );
  }

  static Future<Map<String, dynamic>> _musicRoomFormPost(
    String path, [
    Map<String, dynamic>? fields,
    MultipartFile? localFile,
  ]) async {
    final Options options = Options(
      followRedirects: false,
      validateStatus: (s) => s != null && (s < 400 || s == 302 || s == 303 || s == 307),
    );
    final Response res;
    if (localFile != null) {
      final map = <String, dynamic>{
        ...?fields,
        'localFile': localFile,
        'acceptLocalTerms': 'true',
      };
      res = await _dio.post(path, data: FormData.fromMap(map), options: options);
    } else {
      res = await _dio.post(
        path,
        data: fields ?? <String, dynamic>{},
        options: options.copyWith(contentType: Headers.formUrlEncodedContentType),
      );
    }
    final loc = res.headers.value('location') ?? res.headers.value('Location') ?? '';
    if (loc.isNotEmpty) {
      final uri = Uri.parse(loc.startsWith('http') ? loc : 'http://local$loc');
      final err = uri.queryParameters['error'];
      if (err != null && err.isNotEmpty) {
        throw Exception(err);
      }
      final match = RegExp(r'/music/rooms/([^/?]+)').firstMatch(loc);
      if (match != null) {
        return {'ok': true, 'code': match.group(1)};
      }
    }
    if (res.data is Map) return Map<String, dynamic>.from(res.data as Map);
    return {'ok': true};
  }

  static Future<Map<String, dynamic>> createMusicRoom({
    required String name,
    String? category,
    int? trackId,
    MultipartFile? localFile,
    String? localTitle,
    String? localArtistName,
  }) =>
      _musicRoomFormPost(
        '/music/rooms/create',
        {
          'name': name,
          if (category != null && category.isNotEmpty) 'category': category,
          if (trackId != null) 'trackId': trackId.toString(),
          if (localTitle != null) 'localTitle': localTitle,
          if (localArtistName != null) 'localArtistName': localArtistName,
        },
        localFile,
      );

  static Future<Map<String, dynamic>> joinMusicRoom({
    required String code,
    int? trackId,
    MultipartFile? localFile,
    String? localTitle,
    String? localArtistName,
  }) =>
      _musicRoomFormPost(
        '/music/rooms/join',
        {
          'code': code,
          if (trackId != null) 'trackId': trackId.toString(),
          if (localTitle != null) 'localTitle': localTitle,
          if (localArtistName != null) 'localArtistName': localArtistName,
        },
        localFile,
      );

  static Future<Map<String, dynamic>> submitMusicRoomTrack(String code, int trackId) =>
      _musicRoomFormPost('/music/rooms/$code/submit', {'trackId': trackId.toString()});

  static Future<Map<String, dynamic>> checkInBattleParticipant(int battleId, int participantId) =>
      _battleFormPost('/battles/$battleId/checkin/$participantId');

  static Future<Map<String, dynamic>> rewardByCode(String rewardCode) async {
    final res = await _dio.get('/api/mobile/rewards/code/$rewardCode');
    return Map<String, dynamic>.from(res.data as Map);
  }

  static Future<Map<String, dynamic>> createGameRoom(
    String game, {
    String? playerName,
    int? maxPlayers,
  }) async {
    final res = await _dio.post('/api/$game/create', data: {
      if (playerName != null) 'playerName': playerName,
      if (maxPlayers != null) 'maxPlayers': maxPlayers.toString(),
    });
    final map = Map<String, dynamic>.from(res.data as Map);
    if (map['error'] != null) throw Exception(map['error'].toString());
    return map;
  }

  static Future<Map<String, dynamic>> joinGameRoom(
    String game, {
    required String roomId,
    String? playerName,
  }) async {
    final res = await _dio.post('/api/$game/join', data: {
      'roomId': roomId,
      if (playerName != null) 'playerName': playerName,
    });
    final map = Map<String, dynamic>.from(res.data as Map);
    if (map['error'] != null) throw Exception(map['error'].toString());
    return map;
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

  static Future<Map<String, dynamic>> redeemReward(String rewardCode) async {
    final res = await _dio.post('/api/mobile/rewards/redeem/$rewardCode');
    return Map<String, dynamic>.from(res.data as Map? ?? {'message': 'Reward redeemed'});
  }

  static Future<List<Map<String, dynamic>>> gameCoinHistory() async {
    final res = await _dio.get('/api/games/history');
    final data = res.data;
    if (data is! List) return [];
    return data.map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  static Future<void> updateChatTheme(int conversationId, String theme) =>
      _dio.post('/api/chat/update-theme/$conversationId', queryParameters: {'theme': theme});

  static Future<void> cleanupVanish(int conversationId) =>
      _dio.post('/api/chat/cleanup-vanish/$conversationId');

  static Future<Map<String, dynamic>> togglePinMessage(int messageId) async {
    final res = await _dio.post('/api/chat/toggle-pin/$messageId');
    return Map<String, dynamic>.from(res.data as Map? ?? {});
  }

  static Future<List<Map<String, dynamic>>> chatMedia(int conversationId) async {
    final res = await _dio.get('/api/chat/media/$conversationId');
    final data = res.data;
    if (data is! List) return [];
    return data.map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

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
