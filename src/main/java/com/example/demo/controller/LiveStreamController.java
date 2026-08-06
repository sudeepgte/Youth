package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.repository.PostRepository;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LiveStreamController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private User getUserFromSession(HttpSession session) {
        Object sessionUser = session.getAttribute("user");
        if (sessionUser instanceof User) {
            return (User) sessionUser;
        }
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj != null) {
            try {
                Long userId = null;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }
                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    @GetMapping("/live")
    public String liveStreamPage(@RequestParam(required = false, defaultValue = "Live Stream") String title,
                                 @RequestParam(required = false, defaultValue = "false") boolean start,
                                 @RequestParam(required = false) String roomId,
                                 HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }
        user = userRepository.findById(user.getId()).orElse(user);
        session.setAttribute("user", user);
        
        String streamRoomId = roomId;
        if (streamRoomId == null || streamRoomId.isEmpty()) {
            if (start) {
                streamRoomId = user.getUsername() + "_" + System.currentTimeMillis();
                
                // Broadcast Live Status to feed
                Post livePost = new Post();
                livePost.setUser(user);
                livePost.setContent("I'm live now! Join my broadcast: " + title);
                livePost.setPostType("GOLIVE");
                livePost.setMediaUrl("/live?roomId=" + streamRoomId);
                postRepository.save(livePost);
                
                // Broadcast to all followers via WebSocket
                if (user.getFollowers() != null && !user.getFollowers().isEmpty()) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "live");
                    payload.put("senderId", user.getId());
                    payload.put("senderName", user.getUsername());
                    payload.put("roomId", streamRoomId);
                    payload.put("title", title);
                    
                    for (User follower : user.getFollowers()) {
                        messagingTemplate.convertAndSendToUser(
                            follower.getId().toString(),
                            "/queue/call",
                            (Object) payload
                        );
                    }
                }
                
                model.addAttribute("livePostId", livePost.getId());
            } else {
                return "redirect:/"; // Should not happen directly
            }
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("title", title);
        model.addAttribute("isBroadcaster", start);
        model.addAttribute("roomId", streamRoomId);
        return "live";
    }
    
    @PostMapping("/live/end")
    @ResponseBody
    public String endLiveStream(@RequestParam("postId") Long postId, HttpSession session) {
        User user = getUserFromSession(session);
        if (user != null && postId != null) {
            Post post = postRepository.findById(postId).orElse(null);
            if (post != null && post.getUser().getId().equals(user.getId())) {
                postRepository.delete(post);
                return "success";
            }
        }
        return "error";
    }
}
