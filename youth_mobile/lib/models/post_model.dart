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
        user: j['user'] is Map<String, dynamic>
            ? PostAuthor.fromJson(j['user'] as Map<String, dynamic>)
            : null,
      );
}
