class EventModel {
  final int id;
  final String? title;
  final String? description;
  final String? imageUrl;
  final String? dateTime;
  final String? venue;
  final String? price;
  final String? category;
  final String? organizer;
  final String? status;
  final String? entryFeeType;
  final String? eventMode;
  final String? meetingLink;
  final int? pollVotes;
  final double? regularPrice;
  final double? vipPrice;
  final bool enableSecretRewards;
  final int? maxParticipants;
  final int? registeredCount;
  final int? spotsLeft;
  final bool isRegistered;
  final String? myTicketId;
  final bool hasSeats;
  final bool hasFreeEntry;
  final bool hasDiscount;
  final double? walletBalance;
  final List<Map<String, dynamic>> seats;

  EventModel({
    required this.id,
    this.title,
    this.description,
    this.imageUrl,
    this.dateTime,
    this.venue,
    this.price,
    this.category,
    this.organizer,
    this.status,
    this.entryFeeType,
    this.eventMode,
    this.meetingLink,
    this.pollVotes,
    this.regularPrice,
    this.vipPrice,
    this.enableSecretRewards = false,
    this.maxParticipants,
    this.registeredCount,
    this.spotsLeft,
    this.isRegistered = false,
    this.myTicketId,
    this.hasSeats = false,
    this.hasFreeEntry = false,
    this.hasDiscount = false,
    this.walletBalance,
    this.seats = const [],
  });

  bool get isPaid {
    if (hasFreeEntry) return false;
    if ((entryFeeType ?? '').toUpperCase() == 'FREE') return false;
    final p = effectiveUnitPrice('REGULAR');
    return p > 0;
  }

  double effectiveUnitPrice(String tier) {
    if (tier.toUpperCase() == 'VIP' && vipPrice != null) return vipPrice!;
    if (regularPrice != null) return regularPrice!;
    if (price != null) {
      final cleaned = price!.replaceAll(RegExp(r'[^0-9.]'), '');
      return double.tryParse(cleaned) ?? 0;
    }
    return 0;
  }

  String priceLabel({String tier = 'REGULAR'}) {
    final p = effectiveUnitPrice(tier);
    if (p <= 0 || (entryFeeType ?? '').toUpperCase() == 'FREE') return 'Free';
    return '₹${p.toStringAsFixed(0)}';
  }

  factory EventModel.fromJson(Map<String, dynamic> j) => EventModel(
        id: (j['id'] as num).toInt(),
        title: j['title'] as String?,
        description: j['description'] as String?,
        imageUrl: j['imageUrl'] as String?,
        dateTime: j['dateTime'] as String?,
        venue: j['venue'] as String?,
        price: j['price'] as String?,
        category: j['category'] as String?,
        organizer: j['organizer'] as String?,
        status: j['status'] as String?,
        entryFeeType: j['entryFeeType'] as String?,
        eventMode: j['eventMode'] as String?,
        meetingLink: j['meetingLink'] as String?,
        pollVotes: (j['pollVotes'] as num?)?.toInt(),
        regularPrice: (j['regularPrice'] as num?)?.toDouble(),
        vipPrice: (j['vipPrice'] as num?)?.toDouble(),
        enableSecretRewards: j['enableSecretRewards'] == true,
        maxParticipants: (j['maxParticipants'] as num?)?.toInt(),
        registeredCount: (j['registeredCount'] as num?)?.toInt(),
        spotsLeft: (j['spotsLeft'] as num?)?.toInt(),
        isRegistered: j['isRegistered'] == true,
        myTicketId: j['myTicketId']?.toString(),
        hasSeats: j['hasSeats'] == true,
        hasFreeEntry: j['hasFreeEntry'] == true,
        hasDiscount: j['hasDiscount'] == true,
        walletBalance: (j['walletBalance'] as num?)?.toDouble(),
        seats: (j['seats'] as List? ?? [])
            .whereType<Map>()
            .map((e) => Map<String, dynamic>.from(e))
            .toList(),
      );
}

class BookingModel {
  final int id;
  final String? status;
  final String? ticketId;
  final String? registrationDate;
  final EventModel? event;

  BookingModel({
    required this.id,
    this.status,
    this.ticketId,
    this.registrationDate,
    this.event,
  });

  factory BookingModel.fromJson(Map<String, dynamic> j) => BookingModel(
        id: (j['id'] as num).toInt(),
        status: j['status'] as String?,
        ticketId: j['ticketId'] as String?,
        registrationDate: j['registrationDate'] as String?,
        event: j['event'] is Map
            ? EventModel.fromJson(Map<String, dynamic>.from(j['event'] as Map))
            : null,
      );
}
