package com.skillset.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private String id;
    private String senderId;
    private String recipientId;
    private String content;
    private Boolean isRead;
}
