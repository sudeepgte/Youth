package com.example.demo.controller;

import com.example.demo.model.AdPlacement;
import com.example.demo.model.AdStatus;
import com.example.demo.model.AdType;
import com.example.demo.model.Advertisement;
import com.example.demo.model.TargetAudience;
import com.example.demo.model.User;
import com.example.demo.repository.AdvertisementRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AdvertisementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/advertisements")
public class AdminAdvertisementController {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private AdvertisementService advertisementService;
    
    @Autowired
    private UserRepository userRepository;

    private boolean isAdmin(HttpSession session) {
        return "admin".equals(session.getAttribute("user"));
    }

    @GetMapping
    public String listAds(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        List<Advertisement> ads = advertisementRepository.findAllByOrderByCreatedAtDesc();
        
        // Intelligent status update for view
        LocalDateTime now = LocalDateTime.now();
        boolean needsSave = false;
        for (Advertisement ad : ads) {
            if (advertisementService.updateStatusIntelligently(ad, now)) {
                needsSave = true;
            }
        }
        if (needsSave) {
            advertisementRepository.saveAll(ads);
        }

        long activeCount = advertisementRepository.countByStatus(AdStatus.ACTIVE);
        long scheduledCount = advertisementRepository.countByStatus(AdStatus.SCHEDULED);
        long pausedCount = advertisementRepository.countByStatus(AdStatus.PAUSED);
        Long totalImps = advertisementRepository.sumTotalImpressions();
        Long totalClicks = advertisementRepository.sumTotalClicks();
        
        if (totalImps == null) totalImps = 0L;
        if (totalClicks == null) totalClicks = 0L;
        
        double ctr = totalImps > 0 ? ((double) totalClicks / totalImps) * 100 : 0.0;

        model.addAttribute("ads", ads);
        model.addAttribute("totalAds", ads.size());
        model.addAttribute("activeAds", activeCount);
        model.addAttribute("scheduledAds", scheduledCount);
        model.addAttribute("pausedAds", pausedCount);
        model.addAttribute("totalImpressions", totalImps);
        model.addAttribute("totalClicks", totalClicks);
        model.addAttribute("ctr", String.format("%.1f", ctr));
        
        return "admin-advertisements";
    }

    @GetMapping("/new")
    public String newAdForm(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        model.addAttribute("ad", new Advertisement());
        model.addAttribute("adTypes", AdType.values());
        model.addAttribute("placements", AdPlacement.values());
        model.addAttribute("audiences", TargetAudience.values());
        model.addAttribute("statuses", AdStatus.values());
        return "admin-advertisement-form";
    }

    @PostMapping
    public String saveAd(@ModelAttribute Advertisement ad, 
                         @RequestParam("startDateStr") String startDateStr,
                         @RequestParam("endDateStr") String endDateStr,
                         HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/login";

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            if (startDateStr != null && !startDateStr.isEmpty()) {
                ad.setStartDateTime(LocalDateTime.parse(startDateStr, formatter));
            }
            if (endDateStr != null && !endDateStr.isEmpty()) {
                ad.setEndDateTime(LocalDateTime.parse(endDateStr, formatter));
            }

            // If creating new
            if (ad.getId() == null) {
                User currentAdmin = userRepository.findByUsername("admin"); // simplifier
                ad.setCreatedBy(currentAdmin);
            } else {
                Advertisement existing = advertisementRepository.findById(ad.getId()).orElse(null);
                if (existing != null) {
                    ad.setImpressionCount(existing.getImpressionCount());
                    ad.setClickCount(existing.getClickCount());
                    ad.setCreatedBy(existing.getCreatedBy());
                    ad.setCreatedAt(existing.getCreatedAt());
                }
            }

            advertisementService.updateStatusIntelligently(ad, LocalDateTime.now());
            advertisementRepository.save(ad);
            ra.addFlashAttribute("successMessage", "Advertisement saved successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error saving advertisement: " + e.getMessage());
            return "redirect:/admin/advertisements/new";
        }
        
        return "redirect:/admin/advertisements";
    }

    @GetMapping("/{id}")
    public String adDetails(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        
        Advertisement ad = advertisementRepository.findById(id).orElse(null);
        if (ad == null) return "redirect:/admin/advertisements";
        
        advertisementService.updateStatusIntelligently(ad, LocalDateTime.now());
        advertisementRepository.save(ad);
        
        double ctr = ad.getImpressionCount() > 0 ? ((double) ad.getClickCount() / ad.getImpressionCount()) * 100 : 0.0;
        model.addAttribute("ad", ad);
        model.addAttribute("ctr", String.format("%.1f", ctr));
        return "admin-advertisement-details";
    }

    @GetMapping("/{id}/edit")
    public String editAdForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";
        
        Advertisement ad = advertisementRepository.findById(id).orElse(null);
        if (ad == null) return "redirect:/admin/advertisements";

        model.addAttribute("ad", ad);
        model.addAttribute("adTypes", AdType.values());
        model.addAttribute("placements", AdPlacement.values());
        model.addAttribute("audiences", TargetAudience.values());
        model.addAttribute("statuses", AdStatus.values());
        return "admin-advertisement-form";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/login";
        
        Advertisement ad = advertisementRepository.findById(id).orElse(null);
        if (ad != null) {
            try {
                ad.setStatus(AdStatus.valueOf(status.toUpperCase()));
                advertisementRepository.save(ad);
                ra.addFlashAttribute("successMessage", "Status updated successfully.");
            } catch (Exception e) {
                ra.addFlashAttribute("errorMessage", "Invalid status.");
            }
        }
        return "redirect:/admin/advertisements";
    }

    @PostMapping("/{id}/delete")
    public String deleteAd(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) return "redirect:/login";
        advertisementRepository.deleteById(id);
        ra.addFlashAttribute("successMessage", "Advertisement deleted.");
        return "redirect:/admin/advertisements";
    }
}

