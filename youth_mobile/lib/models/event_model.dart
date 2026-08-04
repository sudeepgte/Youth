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
  final int? pollVotes;
  final double? regularPrice;
  final double? vipPrice;
  final bool enableSecretRewards;

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
    this.pollVotes,
    this.regularPrice,
    this.vipPrice,
    this.enableSecretRewards = false,
  });

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
        pollVotes: (j['pollVotes'] as num?)?.toInt(),
        regularPrice: (j['regularPrice'] as num?)?.toDouble(),
        vipPrice: (j['vipPrice'] as num?)?.toDouble(),
        enableSecretRewards: j['enableSecretRewards'] == true,
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
        event: j['event'] is Map<String, dynamic>
            ? EventModel.fromJson(j['event'] as Map<String, dynamic>)
            : null,
      );
}
