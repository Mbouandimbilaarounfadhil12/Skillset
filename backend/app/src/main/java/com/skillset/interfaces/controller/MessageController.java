package com.skillset.interfaces.controller;

import com.skillset.application.dto.ConversationSummaryDTO;
import com.skillset.application.dto.MessageDTO;
import com.skillset.application.dto.SendMessageRequest;
import com.skillset.application.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageDTO> sendMessage(@AuthenticationPrincipal String userId,
                                                   @RequestBody SendMessageRequest request) {
        MessageDTO sent = messageService.sendMessage(userId, request.getRecipientId(), request.getContent());
        return new ResponseEntity<>(sent, HttpStatus.CREATED);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDTO>> getMyConversations(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(messageService.listConversations(userId));
    }

    @GetMapping("/conversation")
    @PreAuthorize("hasRole('ADMIN') or #userId1 == authentication.principal or #userId2 == authentication.principal")
    public ResponseEntity<List<MessageDTO>> getConversation(
            @AuthenticationPrincipal String currentUserId,
            @RequestParam String userId1,
            @RequestParam String userId2) {
        List<MessageDTO> messages = messageService.getConversation(currentUserId, userId1, userId2);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/unread/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<List<MessageDTO>> getUnreadMessages(@AuthenticationPrincipal String currentUserId,
                                                              @PathVariable String userId) {
        List<MessageDTO> unreadMessages = messageService.getUnreadMessages(currentUserId, userId);
        return ResponseEntity.ok(unreadMessages);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<MessageDTO> markAsRead(@AuthenticationPrincipal String userId,
                                                 @PathVariable String id) {
        return ResponseEntity.ok(messageService.markAsRead(id, userId));
    }
}
