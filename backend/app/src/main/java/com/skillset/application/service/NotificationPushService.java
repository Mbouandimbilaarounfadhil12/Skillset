package com.skillset.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Pushes real-time notification events to connected clients via STOMP WebSocket.
 * Frontend subscribes to /topic/notifications/{userId}.
 */
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate broker;

    public record NotifPayload(String type, String title, String body, String link) {}

    public void push(String userId, String type, String title, String body, String link) {
        if (userId == null || userId.isBlank()) return;
        NotifPayload payload = new NotifPayload(type, title, body, link != null ? link : "");
        broker.convertAndSend("/topic/notifications/" + userId, payload);
    }
}
