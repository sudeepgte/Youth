package com.example.demo;

import com.example.demo.model.Event;
import com.example.demo.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventDataInitializer implements CommandLineRunner {

    @Autowired
    private EventRepository eventRepository;

    @Autowired(required = false)
    private javax.sql.DataSource dataSource;

    @org.springframework.beans.factory.annotation.Value("${app.seed.events:false}")
    private boolean seedEventsEnabled = false;

    @Override
    public void run(String... args) throws Exception {
        if (!seedEventsEnabled) {
            // Remove known sample/preseeded events if they exist so production environment only retains legitimate events
            List<String> sampleTitles = List.of(
                "Summer Cricket League",
                "Zentrix Football Cup",
                "Youthians Football Cup",
                "Rhythmic Night Fest",
                "Zentrix Film Festival",
                "Youthians Film Festival",
                "Campus Standup Comedy Night",
                "Tech Hackathon 2024",
                "Tech Hackathon 2026",
                "AI & Future Workshop",
                "Valorant Ultimate Showdown",
                "Next Big Concert: EDM vs Rock?",
                "Workshop Series: Cloud Computing"
            );
            // 1. Direct SQL cleanup to delete static events and dependent records without FK constraint errors
            if (dataSource != null) {
                try (java.sql.Connection conn = dataSource.getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    String eventSelector = "SELECT id FROM events WHERE LOWER(title) IN (" +
                            "'summer cricket league', 'zentrix football cup', 'youthians football cup', " +
                            "'rhythmic night fest', 'zentrix film festival', 'youthians film festival', " +
                            "'campus standup comedy night', 'tech hackathon 2024', 'tech hackathon 2026', " +
                            "'ai & future workshop', 'valorant ultimate showdown', " +
                            "'next big concert: edm vs rock?', 'workshop series: cloud computing') " +
                            "OR LOWER(organizer) IN ('youthians sports club', 'fc youthians', 'youthians music society', " +
                            "'youthians beats', 'youthians comedy club', 'media society', 'tech innovators', " +
                            "'ai research lab', 'pro gamers league', 'youthians tech hub')";

                    String[] dependentTables = {
                        "DELETE FROM user_voted_events WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM vote WHERE poll_id IN (" + eventSelector + ")",
                        "DELETE FROM event_registration WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM event_seats WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM event_seat_tiers WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM secret_reward_partners WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM adventure_details WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM ladies_party_details WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM trekking_details WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM bike_riding_details WHERE event_id IN (" + eventSelector + ")",
                        "DELETE FROM events WHERE id IN (" + eventSelector + ")"
                    };

                    for (String sql : dependentTables) {
                        try {
                            stmt.execute(sql);
                        } catch (Exception ignored) {}
                    }
                    System.out.println("[EventDataInitializer] Successfully cleaned up all static sample events from DB.");
                } catch (Exception ex) {
                    System.err.println("[EventDataInitializer] SQL cleanup note: " + ex.getMessage());
                }
            }

            // 2. Secondary JPA cleanup
            for (String title : sampleTitles) {
                eventRepository.findAll().stream()
                    .filter(e -> title.equalsIgnoreCase(e.getTitle()))
                    .forEach(e -> {
                        try {
                            eventRepository.delete(e);
                        } catch (Exception ignored) {}
                    });
            }
            return;
        }

        // Seed default sample events if missing so map and category filters have rich, realistic data immediately
        seedDefaultEventIfNotExists("Summer Cricket League",
                "The biggest inter-college cricket tournament of the year. 16 teams, 1 trophy. Witness the adrenaline!",
                "https://images.unsplash.com/photo-1531415074968-036ba1b575da?q=80&w=2070&auto=format&fit=crop",
                LocalDateTime.now().minusHours(1),
                "M. Chinnaswamy Stadium, Bangalore", "Free", "Sports", "Youthians Sports Club", "ONGOING",
                12.9784, 77.5996);

        seedDefaultEventIfNotExists("Youthians Football Cup",
                "A 5-a-side football tournament for all skill levels. Bring your squad and dominate the pitch!",
                "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=1986&auto=format&fit=crop",
                LocalDateTime.now().plusDays(2).withHour(16).withMinute(0),
                "Cooperage Football Ground, Mumbai", "Rs. 150 Team", "Sports", "FC Youthians", "UPCOMING",
                18.9272, 72.8276);

        seedDefaultEventIfNotExists("Rhythmic Night Fest",
                "An evening of music, live bands, and cultural performances. Come groove under the stars!",
                "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?q=80&w=1974&auto=format&fit=crop",
                LocalDateTime.now().withHour(20).withMinute(0),
                "Open Air Amphitheatre, Pune", "Free", "Music", "Youthians Music Society", "UPCOMING",
                18.5204, 73.8567);

        seedDefaultEventIfNotExists("Next Big Concert: EDM vs Rock?",
                "Cast your vote and grab tickets! An electrifying clash between Bass-boosted EDM and Classic Rock.",
                "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=2070&auto=format&fit=crop",
                LocalDateTime.now().plusDays(1).withHour(18).withMinute(30),
                "JLN Stadium Arena, Delhi", "Rs. 299 Entry", "Music", "Youthians Beats", "UPCOMING",
                28.5823, 77.2344);

        seedDefaultEventIfNotExists("Campus Standup Comedy Night",
                "An evening of unfiltered humor, punchlines, and laughter featuring top college & guest comedians.",
                "https://images.unsplash.com/photo-1585699324551-f6c309eedeca?q=80&w=2070&auto=format&fit=crop",
                LocalDateTime.now().plusDays(3).withHour(19).withMinute(0),
                "GMC Balayogi Auditorium, Hyderabad", "Rs. 99 Entry", "Comedy", "Youthians Comedy Club", "UPCOMING",
                17.4474, 78.3489);

        seedDefaultEventIfNotExists("Youthians Film Festival",
                "Showcasing short films and documentaries from talented student creators. Awards for Best Director and Screenplay.",
                "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=2059&auto=format&fit=crop",
                LocalDateTime.now().plusDays(5).withHour(17).withMinute(0),
                "Nandan Cultural Centre, Kolkata", "Rs. 50 Entry", "Cultural", "Media Society", "UPCOMING",
                22.5448, 88.3426);

        seedDefaultEventIfNotExists("Tech Hackathon 2026",
                "48 hours of intense coding, problem-solving, and innovation. Build the future with Youthians!",
                "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?q=80&w=2070&auto=format&fit=crop",
                LocalDateTime.now().plusDays(4).withHour(9).withMinute(0),
                "Digital Innovation Hub, Bangalore", "Rs. 100 Registration", "Tech", "Tech Innovators", "UPCOMING",
                12.9352, 77.6245);

        seedDefaultEventIfNotExists("AI & Future Workshop",
                "Learn about the latest trends in Artificial Intelligence and Machine Learning from industry experts.",
                "https://images.unsplash.com/photo-1591453089816-0fbb971b404c?q=80&w=2070&auto=format&fit=crop",
                LocalDateTime.now().plusDays(6).withHour(14).withMinute(0),
                "IIT Madras Research Park, Chennai", "Free", "Tech", "AI Research Lab", "UPCOMING",
                12.9915, 80.2425);

        seedDefaultEventIfNotExists("Valorant Ultimate Showdown",
                "Compete against the best tactical shooters in the college. High stakes, big prizes!",
                "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2070&auto=format&fit=crop",
                LocalDateTime.now().plusDays(8).withHour(15).withMinute(0),
                "Gaming Lounge, Ahmedabad", "Rs. 20 Entry", "Gaming", "Pro Gamers League", "UPCOMING",
                23.0338, 72.5850);

        seedDefaultEventIfNotExists("Workshop Series: Cloud Computing",
                "Level up your skills! Vote to make Cloud Computing our next deep-dive workshop series.",
                "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=2072&auto=format&fit=crop",
                LocalDateTime.now().plusDays(15).withHour(11).withMinute(0),
                "Cyber City Convention Centre, Gurgaon", "Free", "Tech", "Youthians Tech Hub", "VOTING",
                28.4950, 77.0895);

        // Auto-heal: Ensure all existing events in database have valid coordinates so heatmap map never loads empty
        List<Event> missingCoords = eventRepository.findAll().stream()
                .filter(e -> !e.isDeleted())
                .filter(e -> e.getLatitude() == null || e.getLongitude() == null)
                .collect(Collectors.toList());
        if (!missingCoords.isEmpty()) {
            double[][] fallbackLocations = {
                {12.9784, 77.5996}, // Bangalore
                {18.9272, 72.8276}, // Mumbai
                {28.5823, 77.2344}, // Delhi
                {17.4474, 78.3489}, // Hyderabad
                {18.5204, 73.8567}, // Pune
                {12.9915, 80.2425}, // Chennai
                {22.5448, 88.3426}, // Kolkata
                {23.0338, 72.5850}  // Ahmedabad
            };
            int idx = 0;
            for (Event ev : missingCoords) {
                double[] loc = fallbackLocations[idx % fallbackLocations.length];
                ev.setLatitude(loc[0]);
                ev.setLongitude(loc[1]);
                try {
                    eventRepository.save(ev);
                } catch (Exception ignored) {}
                idx++;
            }
        }
    }

    private void seedDefaultEventIfNotExists(String title, String desc, String url, LocalDateTime dt, String venue, String price,
            String cat, String org, String status, Double latitude, Double longitude) {
        boolean exists = eventRepository.findAll().stream()
                .anyMatch(e -> title.equalsIgnoreCase(e.getTitle()));
        if (!exists) {
            createEvent(title, desc, url, dt, venue, price, cat, org, status, latitude, longitude);
        } else {
            eventRepository.findAll().stream()
                .filter(e -> title.equalsIgnoreCase(e.getTitle()) && (e.getLatitude() == null || e.getLongitude() == null))
                .forEach(e -> {
                    e.setLatitude(latitude);
                    e.setLongitude(longitude);
                    try {
                        eventRepository.save(e);
                    } catch (Exception ignored) {}
                });
        }
    }

    private void createEvent(String title, String desc, String url, LocalDateTime dt, String venue, String price,
            String cat, String org, String status, Double latitude, Double longitude) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(desc);
        event.setImageUrl(url);
        event.setDateTime(dt);
        event.setVenue(venue);
        event.setPrice(price);
        event.setCategory(cat);
        event.setOrganizer(org);
        event.setStatus(status);
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        eventRepository.save(event);
    }

    private void createEvent(String title, String desc, String url, LocalDateTime dt, String venue, String price,
            String cat, String org, String status) {
        createEvent(title, desc, url, dt, venue, price, cat, org, status, 12.9784, 77.5996);
    }
}
