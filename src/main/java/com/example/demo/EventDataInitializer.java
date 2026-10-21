package com.example.demo;

import com.example.demo.model.Event;
import com.example.demo.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class EventDataInitializer implements CommandLineRunner {

    @Autowired
    private EventRepository eventRepository;

    @org.springframework.beans.factory.annotation.Value("${app.seed.events:false}")
    private boolean seedEventsEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!seedEventsEnabled) {
            // Remove known sample/preseeded events if they exist so production environment only retains legitimate events
            List<String> sampleTitles = List.of(
                "Summer Cricket League",
                "Zentrix Football Cup",
                "Rhythmic Night Fest",
                "Zentrix Film Festival",
                "Tech Hackathon 2024",
                "AI & Future Workshop",
                "Valorant Ultimate Showdown",
                "Next Big Concert: EDM vs Rock?",
                "Workshop Series: Cloud Computing"
            );
            for (String title : sampleTitles) {
                eventRepository.findAll().stream()
                    .filter(e -> title.equalsIgnoreCase(e.getTitle()))
                    .forEach(e -> {
                        try {
                            eventRepository.delete(e);
                        } catch (Exception ex) {
                            // If has dependent child records, skip deletion
                        }
                    });
            }
            return;
        }

        if (eventRepository.count() == 0) {
            // Sports
            createEvent("Summer Cricket League",
                    "The biggest inter-college cricket tournament of the year. 16 teams, 1 trophy. Witness the adrenaline!",
                    "https://images.unsplash.com/photo-1531415074968-036ba1b575da?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(10).withHour(10).withMinute(0),
                    "Main Cricket Ground", "Free", "Sports", "Zentrix Sports Club", "UPCOMING");

            createEvent("Zentrix Football Cup",
                    "A 5-a-side football tournament for all skill levels. Bring your squad and dominate the pitch!",
                    "https://images.unsplash.com/photo-1574629810360-7efbbe195018?q=80&w=1986&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(15).withHour(16).withMinute(0),
                    "Blue Turf Arena", "₹150 Team", "Sports", "FC Zentrix", "UPCOMING");

            // Cultural
            createEvent("Rhythmic Night Fest",
                    "An evening of music, dance, and artistic expression. Featuring local bands and cultural performances.",
                    "https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?q=80&w=1974&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(20).withHour(18).withMinute(30),
                    "Open Air Amphitheatre", "Free", "Cultural", "Arts Council", "UPCOMING");

            createEvent("Zentrix Film Festival",
                    "Showcasing short films and documentaries from talented student creators. Awards for Best Director and Screenplay.",
                    "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=2059&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(25).withHour(17).withMinute(0),
                    "Auditorium B", "₹50 Entry", "Cultural", "Media Society", "UPCOMING");

            // Tech
            createEvent("Tech Hackathon 2024",
                    "48 hours of intense coding, problem-solving, and innovation. Build the future with Zentrix!",
                    "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(5).withHour(9).withMinute(0),
                    "Digital Innovation Hub", "₹100 Registration", "Tech", "Tech Innovators", "UPCOMING");

            createEvent("AI & Future Workshop",
                    "Learn about the latest trends in Artificial Intelligence and Machine Learning from industry experts.",
                    "https://images.unsplash.com/photo-1591453089816-0fbb971b404c?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(12).withHour(14).withMinute(0),
                    "Seminar Hall 1", "Free", "Workshop", "AI Research Lab", "UPCOMING");

            // E-Sports
            createEvent("Valorant Ultimate Showdown",
                    "Compete against the best tactical shooters in the college. High stakes, big prizes!",
                    "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(8).withHour(15).withMinute(0),
                    "Gaming Lounge / Online", "₹20 Entry", "E-Sports", "Pro Gamers League", "UPCOMING");

            // Voting Polls
            createEvent("Next Big Concert: EDM vs Rock?",
                    "Cast your vote! Should our next cultural night be a Bass-boosted EDM rave or a Classic Rock showdown?",
                    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=2070&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(30),
                    "Main Hall", "TBA", "Cultural", "Events Team", "VOTING");

            createEvent("Workshop Series: Cloud Computing",
                    "Level up your skills! Vote to make Cloud Computing our next deep-dive workshop series.",
                    "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=2072&auto=format&fit=crop",
                    LocalDateTime.now().plusDays(45),
                    "Virtual Lab", "Free", "Tech", "Zentrix Tech Hub", "VOTING");
        }
    }

    private void createEvent(String title, String desc, String url, LocalDateTime dt, String venue, String price,
            String cat, String org, String status) {
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
        eventRepository.save(event);
    }
}
