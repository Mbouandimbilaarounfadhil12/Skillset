package com.skillset.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean onboardingCompleted;
    private boolean twoFactorRequired;
    private String preAuthToken;
}
