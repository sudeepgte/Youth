class AppUser {
  final int id;
  final String username;
  final String? email;
  final String? bio;
  final String? aboutMe;
  final String? skills;
  final String? collegeName;
  final String? gender;
  final String? profilePhotoUrl;
  final bool privateAccount;
  final int xp;
  final String level;
  final int coins;
  final double walletBalance;
  final int followersCount;
  final int followingCount;
  final bool isPremium;
  final bool hasDiscount;
  final bool hasFreeEntry;
  final bool? isOwnProfile;
  final bool? isFollowing;
  final List<dynamic>? posts;

  AppUser({
    required this.id,
    required this.username,
    this.email,
    this.bio,
    this.aboutMe,
    this.skills,
    this.collegeName,
    this.gender,
    this.profilePhotoUrl,
    this.privateAccount = false,
    this.xp = 0,
    this.level = 'Novice',
    this.coins = 0,
    this.walletBalance = 0,
    this.followersCount = 0,
    this.followingCount = 0,
    this.isPremium = false,
    this.hasDiscount = false,
    this.hasFreeEntry = false,
    this.isOwnProfile,
    this.isFollowing,
    this.posts,
  });

  factory AppUser.fromJson(Map<String, dynamic> j) => AppUser(
        id: (j['id'] as num).toInt(),
        username: j['username'] as String? ?? '',
        email: j['email'] as String?,
        bio: j['bio'] as String?,
        aboutMe: j['aboutMe'] as String?,
        skills: j['skills'] as String?,
        collegeName: j['collegeName'] as String?,
        gender: j['gender'] as String?,
        profilePhotoUrl: j['profilePhotoUrl'] as String?,
        privateAccount: j['privateAccount'] == true,
        xp: (j['xp'] as num?)?.toInt() ?? 0,
        level: j['level'] as String? ?? 'Novice',
        coins: (j['coins'] as num?)?.toInt() ?? 0,
        walletBalance: (j['walletBalance'] as num?)?.toDouble() ?? 0,
        followersCount: (j['followersCount'] as num?)?.toInt() ??
            (j['followers'] as num?)?.toInt() ??
            0,
        followingCount: (j['followingCount'] as num?)?.toInt() ??
            (j['following'] as num?)?.toInt() ??
            0,
        isPremium: j['isPremium'] == true,
        hasDiscount: j['hasDiscount'] == true,
        hasFreeEntry: j['hasFreeEntry'] == true,
        isOwnProfile: j['isOwnProfile'] as bool?,
        isFollowing: j['isFollowing'] as bool?,
        posts: j['posts'] as List<dynamic>?,
      );

  AppUser copyWith({int? coins, double? walletBalance}) => AppUser(
        id: id,
        username: username,
        email: email,
        bio: bio,
        aboutMe: aboutMe,
        skills: skills,
        collegeName: collegeName,
        gender: gender,
        profilePhotoUrl: profilePhotoUrl,
        privateAccount: privateAccount,
        xp: xp,
        level: level,
        coins: coins ?? this.coins,
        walletBalance: walletBalance ?? this.walletBalance,
        followersCount: followersCount,
        followingCount: followingCount,
        isPremium: isPremium,
        hasDiscount: hasDiscount,
        hasFreeEntry: hasFreeEntry,
        isOwnProfile: isOwnProfile,
        isFollowing: isFollowing,
        posts: posts,
      );
}
