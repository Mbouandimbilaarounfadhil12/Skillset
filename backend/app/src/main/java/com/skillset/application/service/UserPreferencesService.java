package com.skillset.application.service;

import com.skillset.domain.entity.UserPreferences;
import com.skillset.infrastructure.persistence.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;

    public UserPreferences getPreferences(String userId) {
        return preferencesRepository.findByUserId(userId).orElseGet(() -> {
            UserPreferences d = new UserPreferences();
            d.setUserId(userId);
            d.setNotificationsEnabled(true);
            d.setEmailAlertsEnabled(true);
            return d;
        });
    }

    public UserPreferences savePreferences(String userId, UserPreferences incoming) {
        if (!userId.equals(incoming.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        UserPreferences pref = preferencesRepository.findByUserId(userId).orElseGet(() -> {
            UserPreferences p = new UserPreferences();
            p.setUserId(userId);
            return p;
        });
        pref.setPreferredJobTypes(incoming.getPreferredJobTypes());
        pref.setPreferredLocations(incoming.getPreferredLocations());
        pref.setPreferredIndustries(incoming.getPreferredIndustries());
        pref.setSalaryExpectationMin(incoming.getSalaryExpectationMin());
        pref.setSalaryExpectationMax(incoming.getSalaryExpectationMax());
        pref.setNotificationsEnabled(Boolean.TRUE.equals(incoming.getNotificationsEnabled()));
        pref.setEmailAlertsEnabled(Boolean.TRUE.equals(incoming.getEmailAlertsEnabled()));
        return preferencesRepository.save(pref);
    }
}
