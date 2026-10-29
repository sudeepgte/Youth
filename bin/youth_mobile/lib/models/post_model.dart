class PostAuthor {
  final int id;
  final String username;
  final String? profilePhotoUrl;
  final String? level;

  PostAuthor({
    required this.id,
    required this.username,
    this.profilePhotoUrl,
    this.level,
  });

  factory PostAuthor.fromJson(Map<String, dynamic> j) => PostAuthor(
        id: (j['id'] as num).toInt(),
        username: j['username'] as String? ?? '',
        profilePhotoUrl: j['profilePhotoUrl'] as String?,
        level: j['level'] as String?,
      );
}

class PostModel {
  final int id;
  final String? content;
  final String? mediaUrl;
  final String? mediaType;
  final String? hashtags;
  final String? postType;
  final String? category;
  final String? createdAt;
  final int likeCount;
  final int commentCount;
  final bool commentsDisabled;
  final bool liked;
  final bool saved;
  final PostAuthor? user;

  PostModel({
    required this.id,
    this.content,
    this.mediaUrl,
    this.mediaType,
    this.hashtags,
    this.postType,
    this.category,
    this.createdAt,
    this.likeCount = 0,
    this.commentCount = 0,
    this.commentsDisabled = false,
    this.liked = false,
    this.saved = false,
    this.user,
  });

  factory PostModel.fromJson(Map<String, dynamic> j) => PostModel(
        id: (j['id'] as num).toInt(),
        content: j['content'] as String?,
        mediaUrl: j['mediaUrl'] as String?,
        mediaType: j['mediaType'] as String?,
        hashtags: j['hashtags'] as String?,
        postType: j['postType'] as String?,
        category: j['category'] as String?,
        createdAt: j['createdAt'] as String?,
        likeCount: (j['likeCount'] as num?)?.toInt() ?? 0,
        commentCount: (j['commentCount'] as num?)?.toInt() ?? 0,
        commentsDisabled: j['commentsDisabled'] == true,
        liked: j['liked'] == true,
        saved: j['saved'] == true,
        user: j['user'] is Map
            ? PostAuthor.fromJson(Map<String, dynamic>.from(j['user'] as Map))
            : null,
      );

  PostModel copyWith({
    int? likeCount,
    int? commentCount,
    bool? liked,
    bool? saved,
    String? content,
    String? hashtags,
  }) =>
      PostModel(
        id: id,
        content: content ?? this.content,
        mediaUrl: mediaUrl,
        mediaType: mediaType,
        hashtags: hashtags ?? this.hashtags,
        postType: postType,
        category: category,
        createdAt: createdAt,
        likeCount: likeCount ?? this.likeCount,
        commentCount: commentCount ?? this.commentCount,
        commentsDisabled: commentsDisabled,
        liked: liked ?? this.liked,
        saved: saved ?? this.saved,
        user: user,
      );
}
