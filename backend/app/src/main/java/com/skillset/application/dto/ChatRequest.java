package com.skillset.application.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    private String message;
    private List<Map<String, String>> history;
    private Map<String, Object> profile;
    private Boolean online;
}
