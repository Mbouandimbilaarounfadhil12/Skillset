package com.skillset.application.dto;

import lombok.Data;

@Data
public class TwoFactorLoginRequest {
    private String preAuthToken;
    private String code;
}
