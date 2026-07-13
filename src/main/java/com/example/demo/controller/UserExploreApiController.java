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

    @Autowired
    private com.example.demo.repository.PostRepository postRepository;

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

    @RequestMapping(value = "/preview/{id}", method = RequestMethod.GET)
    public org.springframework.http.ResponseEntity<?> getProfilePreview(@PathVariable Long id, jakarta.servlet.http.HttpSession session) {
        User currentUser = null;
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            User sUser = (User) sessionUser;
            currentUser = userRepository.findById(sUser.getId()).orElse(null);
        }

        User targetUser = userRepository.findById(id).orElse(null);
        if (targetUser == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        boolean isFollowing = currentUser != null && currentUser.getFollowing().contains(targetUser);
        boolean isOwnProfile = currentUser != null && currentUser.getId().equals(targetUser.getId());
        boolean isPrivateAndNotFollowing = targetUser.isPrivateAccount() && !isOwnProfile && !isFollowing;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", targetUser.getId());
        response.put("username", targetUser.getUsername());
        response.put("profilePhotoUrl", targetUser.getProfilePhotoUrl());
        response.put("collegeName", targetUser.getCollegeName());
        response.put("level", targetUser.getLevel() != null ? targetUser.getLevel() : "Novice");
        response.put("followersCount", targetUser.getFollowers().size());
        response.put("followingCount", targetUser.getFollowing().size());
        response.put("isPrivateAndNotFollowing", isPrivateAndNotFollowing);

        if (!isPrivateAndNotFollowing) {
            long postsCount = postRepository.findByUserAndPostTypeNotOrderByCreatedAtDesc(targetUser, "STORY").size();
            long reelsCount = postRepository.findByPostTypeOrderByCreatedAtDesc("REEL").stream()
                    .filter(p -> p.getUser().getId().equals(targetUser.getId()))
                    .count();
            // Or a more optimal query:
            // long postsCount = postRepository.countByUserAndPostTypeNot(targetUser, "STORY");
            // long reelsCount = postRepository.countByUserAndPostType(targetUser, "REEL");
            
            response.put("postsCount", postsCount);
            response.put("reelsCount", reelsCount);
        }

        return org.springframework.http.ResponseEntity.ok(response);
    }
}
