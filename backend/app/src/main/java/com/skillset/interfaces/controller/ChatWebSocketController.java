package com.skillset.interfaces.controller;

import com.skillset.application.dto.MessageDTO;
import com.skillset.application.dto.WsChatMessage;
import com.skillset.domain.entity.Message;
import com.skillset.domain.entity.User;
import com.skillset.domain.port.MessageRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate  broker;
    private final MessageRepositoryPort  messageRepository;
    private final UserRepositoryPort     userRepository;

    @MessageMapping("/chat")
    public void handleChat(WsChatMessage msg) {
        User sender    = userRepository.findUserById(msg.senderId()).orElse(null);
        User recipient = userRepository.findUserById(msg.recipientId()).orElse(null);

        if (sender == null || recipient == null) {
            log.warn("WS chat: unknown sender {} or recipient {}", msg.senderId(), msg.recipientId());
            return;
        }

        // Persist
        Message entity = new Message();
        entity.setSender(sender);
        entity.setRecipient(recipient);
        entity.setContent(msg.content());
        entity.setIsRead(false);
        entity.setSentAt(LocalDateTime.now());
        Message saved = messageRepository.save(entity);

        MessageDTO dto = new MessageDTO(
            saved.getId(),
            saved.getSender().getId(),
            saved.getRecipient().getId(),
            saved.getContent(),
            saved.getIsRead()
        );

        // Deliver to both participants so sender sees own message confirmed
        broker.convertAndSend("/topic/user/" + msg.recipientId(), dto);
        broker.convertAndSend("/topic/user/" + msg.senderId(),    dto);
    }
}
