package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/api/qrcode")
public class QrCodeController {

    @GetMapping("/generate")
    public String generateQrCode(@RequestParam("text") String text) {
        String encoded = URLEncoder.encode(text != null ? text : "", StandardCharsets.UTF_8);
        return "redirect:https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + encoded;
    }
}
