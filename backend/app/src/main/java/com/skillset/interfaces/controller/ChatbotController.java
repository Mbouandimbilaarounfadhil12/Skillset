package com.skillset.interfaces.controller;

import com.skillset.application.dto.ChatRequest;
import com.skillset.application.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stella")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @AuthenticationPrincipal String userId,
            @RequestBody ChatRequest request) {
        String reply = chatbotService.chat(userId, request);
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(
            @AuthenticationPrincipal String userId,
            @RequestParam String q) {
        return ResponseEntity.ok(chatbotService.search(q));
    }
}
