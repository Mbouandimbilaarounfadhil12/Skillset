package com.skillset.interfaces.controller;

import com.skillset.application.service.UserPreferencesService;
import com.skillset.domain.entity.UserPreferences;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;

    @GetMapping
    public ResponseEntity<UserPreferences> get(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(preferencesService.getPreferences(userId));
    }

    @PutMapping
    public ResponseEntity<UserPreferences> save(
            @AuthenticationPrincipal String userId,
            @RequestBody UserPreferences body) {
        body.setUserId(userId);
        return ResponseEntity.ok(preferencesService.savePreferences(userId, body));
    }
}
