package com.example.demo.controller;

import com.example.demo.model.Event;
import com.example.demo.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/heatmap")
public class HeatmapApiController {

    @Autowired
    private EventRepository eventRepository;

    @GetMapping("/events")
    public Map<String, Object> getHeatmapData(@RequestParam(defaultValue = "all") String filter) {
        List<Event> allEvents = eventRepository.findAll().stream()
                .filter(e -> !e.isDeleted())
                .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(LocalTime.MAX);
        
        // Genuine Calendar Week (Monday - Sunday)
        LocalDateTime startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime endOfWeek = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
        
        // Genuine Calendar Month (1st to last day)
        LocalDateTime startOfMonth = today.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

        // Compute statistics from ALL geocoded events
        long liveCount = allEvents.stream().filter(e -> "ONGOING".equals(e.getStatus())).count();
        long todayCount = allEvents.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfToday) && !e.getDateTime().isAfter(endOfToday)).count();
        long tomorrowCount = allEvents.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfTomorrow) && !e.getDateTime().isAfter(endOfTomorrow)).count();
        long weekCount = allEvents.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfWeek) && !e.getDateTime().isAfter(endOfWeek)).count();
        long monthCount = allEvents.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfMonth) && !e.getDateTime().isAfter(endOfMonth)).count();

        // Apply filter
        List<Event> filtered;
        switch (filter.toLowerCase()) {
            case "live":
                filtered = allEvents.stream().filter(e -> "ONGOING".equals(e.getStatus())).collect(Collectors.toList());
                break;
            case "today":
                filtered = allEvents.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfToday) && !e.getDateTime().isAfter(endOfToday)).collect(Collectors.toList());
                break;
            case "tomorrow":
                filtered = allEvents.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfTomorrow) && !e.getDateTime().isAfter(endOfTomorrow)).collect(Collectors.toList());
                break;
            case "week":
                filtered = allEvents.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfToday) && !e.getDateTime().isAfter(endOfWeek)).collect(Collectors.toList());
                break;
            case "month":
                filtered = allEvents.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfToday) && !e.getDateTime().isAfter(endOfMonth)).collect(Collectors.toList());
                break;
            default:
                filtered = allEvents;
                break;
        }

        // Build grouped location data for markers
        Map<String, List<Event>> byLocation = filtered.stream()
                .collect(Collectors.groupingBy(e -> e.getLatitude() + "," + e.getLongitude()));

        List<Map<String, Object>> markers = new ArrayList<>();
        for (Map.Entry<String, List<Event>> entry : byLocation.entrySet()) {
            List<Event> eventsAtLoc = entry.getValue();
            Event sample = eventsAtLoc.get(0);

            long locLive = eventsAtLoc.stream().filter(e -> "ONGOING".equals(e.getStatus())).count();
            long locToday = eventsAtLoc.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfToday) && !e.getDateTime().isAfter(endOfToday)).count();
            long locTomorrow = eventsAtLoc.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfTomorrow) && !e.getDateTime().isAfter(endOfTomorrow)).count();
            long locWeek = eventsAtLoc.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfToday) && !e.getDateTime().isAfter(endOfWeek)).count();
            long locMonth = eventsAtLoc.stream().filter(e -> e.getDateTime() != null && !e.getDateTime().isBefore(startOfToday) && !e.getDateTime().isAfter(endOfMonth)).count();

            Map<String, Object> marker = new LinkedHashMap<>();
            marker.put("lat", sample.getLatitude());
            marker.put("lng", sample.getLongitude());
            marker.put("location", sample.getVenue() != null ? sample.getVenue() : "Unknown");
            marker.put("total", eventsAtLoc.size());
            marker.put("live", locLive);
            marker.put("today", locToday);
            marker.put("tomorrow", locTomorrow);
            marker.put("week", locWeek);
            marker.put("month", locMonth);
            markers.add(marker);
        }

        // Build heat points (lat, lng, intensity)
        List<List<Number>> heatPoints = new ArrayList<>();
        for (Map.Entry<String, List<Event>> entry : byLocation.entrySet()) {
            Event sample = entry.getValue().get(0);
            heatPoints.add(List.of(sample.getLatitude(), sample.getLongitude(), entry.getValue().size()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", Map.of(
                "live", liveCount,
                "today", todayCount,
                "tomorrow", tomorrowCount,
                "week", weekCount,
                "month", monthCount
        ));
        result.put("markers", markers);
        result.put("heatPoints", heatPoints);
        result.put("totalFiltered", filtered.size());

        return result;
    }

    @GetMapping("/populate")
    public Map<String, Object> populateDummyCoordinates() {
        List<Event> allEvents = eventRepository.findAll();
        int count = 0;
        
        double[][] indiaLocations = {
            {28.7041, 77.1025}, // Delhi
            {19.0760, 72.8777}, // Mumbai
            {12.9716, 77.5946}, // Bangalore
            {17.3850, 78.4867}, // Hyderabad
            {13.0827, 80.2707}, // Chennai
            {22.5726, 88.3639}, // Kolkata
            {18.5204, 73.8567}, // Pune
            {23.0225, 72.5714}, // Ahmedabad
            {26.9124, 75.7873}, // Jaipur
            {21.1702, 72.8311}  // Surat
        };
        
        Random rand = new Random();
        
        for (Event event : allEvents) {
            if (event.getLatitude() == null || event.getLongitude() == null) {
                double[] loc = indiaLocations[rand.nextInt(indiaLocations.length)];
                // add slight randomization so they don't exactly stack
                double latOffset = (rand.nextDouble() - 0.5) * 0.1;
                double lngOffset = (rand.nextDouble() - 0.5) * 0.1;
                event.setLatitude(loc[0] + latOffset);
                event.setLongitude(loc[1] + lngOffset);
                eventRepository.save(event);
                count++;
            }
        }
        
        return Map.of("success", true, "populated", count, "message", "Populated " + count + " events with dummy coordinates.");
    }
}
