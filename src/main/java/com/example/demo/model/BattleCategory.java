package com.example.demo.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class BattleCategory {
    public static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("Entertainment", Arrays.asList("Singing", "Dance", "Acting", "Comedy", "Instrument"));
        CATEGORIES.put("Creative", Arrays.asList("Drawing", "Photography", "Video Editing", "Graphic Design", "Meme Creation"));
        CATEGORIES.put("Technology", Arrays.asList("Coding", "UI Design", "Debugging", "AI Challenge", "Tech Quiz"));
        CATEGORIES.put("Knowledge", Arrays.asList("Quiz", "General Knowledge", "Aptitude", "Logical Reasoning"));
        CATEGORIES.put("Gaming", Arrays.asList("1v1 Gaming", "Strategy", "Speed Challenge"));
    }

    public static final List<String> DIFFICULTIES = Arrays.asList("Beginner", "Intermediate", "Advanced", "Expert");
}
