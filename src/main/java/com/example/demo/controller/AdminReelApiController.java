package com.example.demo.controller;

import com.example.demo.model.Reel;
import com.example.demo.service.ReelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/admin/reels", method = RequestMethod.GET)
public class AdminReelApiController {

    @Autowired
    private ReelService reelService;

    // 1. Approve Reel
    @RequestMapping(value = "/{id}/approve", method = RequestMethod.POST)
    public ResponseEntity<?> approveReel(@PathVariable Long id) {
        try {
            Reel approved = reelService.approveReel(id);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Approval failed: " + e.getMessage());
        }
    }

    // 2. Delete/Reject Reel
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteReel(@PathVariable Long id) {
        try {
            reelService.deleteReel(id);
            return ResponseEntity.ok("Reel deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Deletion failed: " + e.getMessage());
        }
    }

    // 3. Modulate Meta Data Manually
    @RequestMapping(value = "/{id}/metrics", method = RequestMethod.POST)
    public ResponseEntity<?> updateMetrics(
            @PathVariable Long id,
            @RequestBody Map<String, Long> metrics) {
        try {
            Reel updated = reelService.updateReelMetrics(
                    id,
                    metrics.get("views"),
                    metrics.get("likes"),
                    metrics.get("comments"));
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Metric update failed: " + e.getMessage());
        }
    }

    // Feature/Pin reel could be similar boolean flags added to standard updates.
}
