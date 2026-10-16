package com.example.demo.controller;

import com.example.demo.service.AdvertisementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/advertisements")
public class AdvertisementApiController {

    @Autowired
    private AdvertisementService advertisementService;

    @PostMapping("/{id}/impression")
    public ResponseEntity<?> recordImpression(@PathVariable Long id) {
        try {
            advertisementService.incrementImpression(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/click")
    public RedirectView recordClick(@PathVariable Long id) {
        String url = advertisementService.registerClickAndGetUrl(id);
        if (url == null || url.isEmpty()) {
            url = "/";
        }
        return new RedirectView(url);
    }
}
