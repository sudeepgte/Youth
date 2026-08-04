package com.example.demo.controller;

import com.example.demo.model.ContactMessage;
import com.example.demo.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ContactController {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    @PostMapping("/contact")
    public String submitContactForm(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            HttpServletRequest request
    ) {
        ContactMessage contactMessage = new ContactMessage(name, email, subject, message);
        contactMessageRepository.save(contactMessage);

        // Determine where the request came from to redirect back
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/support")) {
            return "redirect:/support?success=message_sent";
        }
        return "redirect:/home?success=message_sent";
    }
}
