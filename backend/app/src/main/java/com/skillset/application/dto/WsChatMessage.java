package com.skillset.application.dto;

public record WsChatMessage(
    String senderId,
    String recipientId,
    String content,
    String conversationId
) {}
