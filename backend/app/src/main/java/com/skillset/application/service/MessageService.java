package com.skillset.application.service;

import com.skillset.application.dto.ConversationSummaryDTO;
import com.skillset.application.dto.MessageDTO;
import com.skillset.domain.entity.Message;
import com.skillset.domain.entity.User;
import com.skillset.domain.port.MessageRepositoryPort;
import com.skillset.domain.port.UserRepositoryPort;
import com.skillset.infrastructure.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepositoryPort    messageRepositoryPort;
    private final UserRepositoryPort       userRepositoryPort;
    private final AuthorizationService     authorizationService;
    private final SimpMessagingTemplate    messagingTemplate;

    public MessageDTO sendMessage(String currentUserId, String recipientId, String content) {
        User sender = userRepositoryPort.findUserById(currentUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        User recipient = userRepositoryPort.findUserById(recipientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destinataire introuvable"));

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setIsRead(false);
        message.setSentAt(LocalDateTime.now());

        Message saved = messageRepositoryPort.save(message);
        MessageDTO dto = convertToDTO(saved);
        // Push real-time notification to recipient
        messagingTemplate.convertAndSend("/topic/user/" + saved.getRecipient().getId(), dto);
        return dto;
    }

    public List<MessageDTO> getConversation(String currentUserId, String userId1, String userId2) {
        authorizationService.requireConversationParticipant(currentUserId, userId1, userId2);
        return messageRepositoryPort.findBySenderIdAndRecipientId(userId1, userId2)
            .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MessageDTO> getUnreadMessages(String currentUserId, String userId) {
        authorizationService.requireSelfOrAdmin(currentUserId, userId);
        return messageRepositoryPort.findByRecipientIdAndIsReadFalse(userId)
            .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public MessageDTO markAsRead(String messageId, String currentUserId) {
        Message message = messageRepositoryPort.findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));
        if (!currentUserId.equals(message.getRecipient().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès non autorisé");
        }
        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        return convertToDTO(messageRepositoryPort.save(message));
    }

    public List<ConversationSummaryDTO> listConversations(String currentUserId) {
        List<Message> messages = new ArrayList<>();
        messages.addAll(messageRepositoryPort.findBySender_Id(currentUserId));
        messages.addAll(messageRepositoryPort.findByRecipient_Id(currentUserId));

        Map<String, List<Message>> byOtherUserId = messages.stream()
            .collect(Collectors.groupingBy(m ->
                currentUserId.equals(m.getSender().getId()) ? m.getRecipient().getId() : m.getSender().getId()));

        List<ConversationSummaryDTO> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Message>> entry : byOtherUserId.entrySet()) {
            String otherUserId = entry.getKey();
            List<Message> group = entry.getValue();

            Message latest = group.stream()
                .max(Comparator.comparing(Message::getSentAt))
                .orElseThrow();

            long unreadCount = group.stream()
                .filter(m -> currentUserId.equals(m.getRecipient().getId()) && !Boolean.TRUE.equals(m.getIsRead()))
                .count();

            String otherUserName = userRepositoryPort.findUserById(otherUserId)
                .map(u -> (nvl(u.getFirstName()) + " " + nvl(u.getLastName())).trim())
                .orElse("");

            summaries.add(new ConversationSummaryDTO(
                otherUserId,
                otherUserName,
                latest.getContent(),
                latest.getSentAt(),
                unreadCount
            ));
        }

        summaries.sort(Comparator.comparing(ConversationSummaryDTO::getLastMessageTime).reversed());
        return summaries;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private MessageDTO convertToDTO(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getSender().getId(),
            message.getRecipient().getId(),
            message.getContent(),
            message.getIsRead()
        );
    }
}
