package com.skillset.infrastructure.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillset.domain.entity.GoogleCalendarToken;
import com.skillset.domain.entity.Interview;
import com.skillset.infrastructure.persistence.GoogleCalendarTokenRepository;
import com.skillset.infrastructure.persistence.InterviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {
    private final GoogleCalendarTokenRepository tokenRepository;
    private final InterviewRepository interviewRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:}")
    private String redirectUri;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_CALENDAR_API_URL = "https://www.googleapis.com/calendar/v3";
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";

    public String getAuthorizationUrl(UUID userId) {
        String state = Base64.getEncoder().encodeToString(userId.toString().getBytes(StandardCharsets.UTF_8));
        return UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", CALENDAR_SCOPE)
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .build()
                .toUriString();
    }

    public GoogleCalendarToken handleOAuthCallback(String code, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to exchange authorization code: " + response.getStatusCode());
            }

            JsonNode tokenResponse = objectMapper.readTree(response.getBody());
            String accessToken = tokenResponse.get("access_token").asText();
            String refreshToken = tokenResponse.has("refresh_token") ? tokenResponse.get("refresh_token").asText() : null;
            int expiresIn = tokenResponse.get("expires_in").asInt();

            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);

            GoogleCalendarToken token = new GoogleCalendarToken();
            token.setUserId(userId);
            token.setAccessToken(accessToken);
            token.setRefreshToken(refreshToken);
            token.setExpiresAt(expiresAt);
            token.setCreatedAt(LocalDateTime.now());
            token.setUpdatedAt(LocalDateTime.now());

            GoogleCalendarToken savedToken = tokenRepository.save(token);
            log.info("Google Calendar token saved for user: {}", userId);
            return savedToken;
        } catch (Exception e) {
            log.error("Error handling OAuth callback for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("OAuth callback handling failed: " + e.getMessage(), e);
        }
    }

    public String createCalendarEvent(Interview interview, String candidateEmail, String employerEmail) {
        try {
            GoogleCalendarToken token = getValidToken(interview.getInterviewerId());
            if (token == null) {
                log.warn("No valid Google Calendar token found for interviewer: {}", interview.getInterviewerId());
                return null;
            }

            String eventJson = buildEventJson(interview, candidateEmail, employerEmail);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token.getAccessToken());

            HttpEntity<String> request = new HttpEntity<>(eventJson, headers);
            String url = GOOGLE_CALENDAR_API_URL + "/calendars/primary/events?sendNotifications=true";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to create calendar event: {}", response.getStatusCode());
                return null;
            }

            JsonNode eventResponse = objectMapper.readTree(response.getBody());
            String eventId = eventResponse.get("id").asText();
            log.info("Calendar event created with ID: {} for interview: {}", eventId, interview.getId());
            return eventId;
        } catch (Exception e) {
            log.error("Error creating calendar event for interview {}: {}", interview.getId(), e.getMessage(), e);
            return null;
        }
    }

    public boolean updateCalendarEvent(String eventId, Interview updatedInterview) {
        try {
            GoogleCalendarToken token = getValidToken(updatedInterview.getInterviewerId());
            if (token == null) {
                log.warn("No valid Google Calendar token found for interviewer: {}", updatedInterview.getInterviewerId());
                return false;
            }

            String eventJson = buildEventJsonForUpdate(updatedInterview);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token.getAccessToken());

            HttpEntity<String> request = new HttpEntity<>(eventJson, headers);
            String url = GOOGLE_CALENDAR_API_URL + "/calendars/primary/events/" + eventId + "?sendNotifications=true";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to update calendar event: {}", response.getStatusCode());
                return false;
            }

            log.info("Calendar event updated: {}", eventId);
            return true;
        } catch (Exception e) {
            log.error("Error updating calendar event {}: {}", eventId, e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteCalendarEvent(String eventId, String interviewerId) {
        try {
            GoogleCalendarToken token = getValidToken(interviewerId);
            if (token == null) {
                log.warn("No valid Google Calendar token found for interviewer: {}", interviewerId);
                return false;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.getAccessToken());

            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = GOOGLE_CALENDAR_API_URL + "/calendars/primary/events/" + eventId + "?sendNotifications=true";
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to delete calendar event: {}", response.getStatusCode());
                return false;
            }

            log.info("Calendar event deleted: {}", eventId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting calendar event {}: {}", eventId, e.getMessage(), e);
            return false;
        }
    }

    private GoogleCalendarToken getValidToken(String interviewerId) {
        try {
            UUID userId = UUID.fromString(interviewerId);
            Optional<GoogleCalendarToken> tokenOpt = tokenRepository.findByUserId(userId);

            if (tokenOpt.isEmpty()) {
                return null;
            }

            GoogleCalendarToken token = tokenOpt.get();
            if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Google Calendar token expired for user: {}", userId);
                return null;
            }

            return token;
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", interviewerId);
            return null;
        }
    }

    private String buildEventJson(Interview interview, String candidateEmail, String employerEmail) throws Exception {
        LocalDateTime startTime = interview.getScheduledAt();
        LocalDateTime endTime = startTime.plusHours(1);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("summary", buildEventTitle(interview));
        event.put("description", buildEventDescription(interview));
        event.put("start", Map.of("dateTime", formatDateTime(startTime), "timeZone", "UTC"));
        event.put("end", Map.of("dateTime", formatDateTime(endTime), "timeZone", "UTC"));

        List<Map<String, String>> attendees = new ArrayList<>();
        if (candidateEmail != null && !candidateEmail.isEmpty()) {
            attendees.add(Map.of("email", candidateEmail, "optional", "true"));
        }
        if (employerEmail != null && !employerEmail.isEmpty()) {
            attendees.add(Map.of("email", employerEmail, "optional", "false"));
        }
        event.put("attendees", attendees);

        Map<String, Object> reminders = new LinkedHashMap<>();
        reminders.put("useDefault", false);
        List<Map<String, Object>> notificationList = new ArrayList<>();
        Map<String, Object> reminder1 = new LinkedHashMap<>();
        reminder1.put("type", "email");
        reminder1.put("minutes", 1440); // 24 hours
        notificationList.add(reminder1);

        Map<String, Object> reminder2 = new LinkedHashMap<>();
        reminder2.put("type", "email");
        reminder2.put("minutes", 30); // 30 minutes
        notificationList.add(reminder2);

        reminders.put("overrides", notificationList);
        event.put("reminders", reminders);

        return objectMapper.writeValueAsString(event);
    }

    private String buildEventJsonForUpdate(Interview interview) throws Exception {
        LocalDateTime startTime = interview.getScheduledAt();
        LocalDateTime endTime = startTime.plusHours(1);

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("summary", buildEventTitle(interview));
        event.put("description", buildEventDescription(interview));
        event.put("start", Map.of("dateTime", formatDateTime(startTime), "timeZone", "UTC"));
        event.put("end", Map.of("dateTime", formatDateTime(endTime), "timeZone", "UTC"));

        return objectMapper.writeValueAsString(event);
    }

    private String buildEventTitle(Interview interview) {
        return "Entretien — Interview";
    }

    private String buildEventDescription(Interview interview) {
        StringBuilder description = new StringBuilder();
        if (interview.getNotes() != null && !interview.getNotes().isEmpty()) {
            description.append(interview.getNotes()).append("\n");
        }
        if (interview.getInterviewLink() != null && !interview.getInterviewLink().isEmpty()) {
            description.append("Lien: ").append(interview.getInterviewLink());
        }
        return description.toString();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
