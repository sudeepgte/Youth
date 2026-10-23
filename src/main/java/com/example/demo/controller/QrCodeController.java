package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class QrCodeController {

    @GetMapping("/api/qrcode/generate")
    public String generateQrCode(@RequestParam(value = "text", required = false) String text) {
        if (text == null || text.trim().isEmpty()) {
            text = "INVALID";
        }
        String encodedText = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
        return "redirect:https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + encodedText;
    }
}
