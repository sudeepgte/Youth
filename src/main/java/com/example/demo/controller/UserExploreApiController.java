package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * REST API for user search by name and/or college.
 * GET /api/users/explore?name=&college=
 */
@RestController
@RequestMapping(value = "/api/users")
public class UserExploreApiController {

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "/explore", method = RequestMethod.GET)
    public List<Map<String, Object>> exploreUsers(
            @RequestParam(required = false, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "") String college) {

        boolean hasName    = !name.trim().isEmpty();
        boolean hasCollege = !college.trim().isEmpty();

        List<User> users;

        if (hasName && hasCollege) {
            // Both filters — must match both
            users = userRepository.findByUsernameAndCollege(name.trim(), college.trim());
        } else if (hasName) {
            users = userRepository.findByUsernameContainingIgnoreCase(name.trim());
        } else if (hasCollege) {
            users = userRepository.findByCollegeNameContainingIgnoreCase(college.trim());
        } else {
            // No filter → return all
            users = userRepository.findAll();
        }

        // Map to safe DTO (no password / email exposed)
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id",          u.getId());
            dto.put("username",    u.getUsername());
            dto.put("collegeName", u.getCollegeName());
            dto.put("level",       u.getLevel() != null ? u.getLevel() : "Novice");
            dto.put("followers",   u.getFollowers() != null ? u.getFollowers().size() : 0);
            dto.put("profilePhotoUrl", u.getProfilePhotoUrl());
            result.add(dto);
        }
        return result;
    }
}
