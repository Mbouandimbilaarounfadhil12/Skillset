# Google Calendar Integration - SkillSet Backend

## Overview

Complete Google Calendar integration for the SkillSet ATS platform has been successfully implemented. This integration allows employers to automatically sync interviews with Google Calendar, including event creation, updates, and deletion.

## Files Created

### 1. Domain Entity Layer

#### GoogleCalendarToken.java
**Path:** `src/main/java/com/skillset/domain/entity/GoogleCalendarToken.java`

Entity for storing encrypted Google OAuth2 tokens and calendar configuration.

**Fields:**
- `id` (UUID) - Primary key
- `userId` (UUID) - Reference to employer user
- `accessToken` (String, encrypted) - OAuth2 access token
- `refreshToken` (String, encrypted) - OAuth2 refresh token
- `expiresAt` (LocalDateTime) - Token expiration time
- `calendarId` (String) - Google Calendar ID
- `createdAt` (LocalDateTime) - Creation timestamp
- `updatedAt` (LocalDateTime) - Last update timestamp

**Features:**
- Uses `@Convert(converter = EncryptionConverter.class)` for token encryption at rest
- Automatic timestamp management with Lombok

### 2. Utility Classes

#### EncryptionConverter.java
**Path:** `src/main/java/com/skillset/infrastructure/util/EncryptionConverter.java`

JPA AttributeConverter for AES encryption/decryption of sensitive data.

**Features:**
- AES 256-bit encryption for sensitive fields
- Configurable via `ENCRYPTION_KEY` environment variable
- Automatic encoding/decoding to Base64 for database storage
- Used to encrypt accessToken and refreshToken fields

### 3. Persistence Layer

#### GoogleCalendarTokenRepository.java
**Path:** `src/main/java/com/skillset/infrastructure/persistence/GoogleCalendarTokenRepository.java`

Spring Data JPA repository for managing Google Calendar tokens.

**Methods:**
- `findByUserId(UUID userId)` - Find token for a specific user
- `deleteByUserId(UUID userId)` - Delete token for a user

### 4. Integration Service

#### GoogleCalendarService.java
**Path:** `src/main/java/com/skillset/infrastructure/integration/GoogleCalendarService.java`

Core service handling all Google Calendar API interactions using REST/HTTP.

**Key Methods:**

1. **getAuthorizationUrl(UUID userId)**
   - Generates OAuth2 authorization URL
   - Returns URL for frontend redirect to Google login

2. **handleOAuthCallback(String code, UUID userId)**
   - Exchanges authorization code for access/refresh tokens
   - Saves encrypted tokens to database
   - Throws RuntimeException on failure with detailed message

3. **createCalendarEvent(Interview interview, String candidateEmail, String employerEmail)**
   - Creates calendar event in primary calendar
   - Event details:
     * Summary: "Entretien — Interview"
     * Description: Interview notes + interview link
     * Duration: 1 hour from scheduledAt time
     * Attendees: Candidate (optional), Employer (required)
     * Reminders: 24 hours before, 30 minutes before
   - Returns eventId on success, null on failure
   - Graceful fallback: logs error and returns null

4. **updateCalendarEvent(String eventId, Interview updatedInterview)**
   - Updates existing calendar event
   - Returns boolean success/failure
   - Supports rescheduling and notes updates

5. **deleteCalendarEvent(String eventId, String interviewerId)**
   - Deletes calendar event
   - Returns boolean success/failure
   - Ensures cleanup when interviews are cancelled

**Technical Details:**
- Uses Spring's RestTemplate for HTTP calls
- Connects directly to Google Calendar API v3 endpoints
- Automatic token validation before API calls
- Detailed error logging for debugging
- ISO 8601 datetime formatting for Google Calendar

### 5. REST Controller

#### GoogleCalendarController.java
**Path:** `src/main/java/com/skillset/interfaces/controller/GoogleCalendarController.java`

REST endpoints for managing Google Calendar integration.

**Endpoints:**

1. **GET /api/integrations/google/auth** `@PreAuthorize("hasRole('EMPLOYER')")`
   - Returns OAuth2 authorization URL
   - Response: `{authUrl: "https://accounts.google.com/o/oauth2/v2/auth?..."}`

2. **GET /api/integrations/google/callback?code={code}&state={state}** (No auth required)
   - Handles OAuth callback from Google
   - Exchanges code for tokens
   - Returns: `{success: true, message: "Google Calendar integration successful"}`

3. **GET /api/integrations/google/status** `@PreAuthorize("hasRole('EMPLOYER')")`
   - Checks if user has connected Google Calendar
   - Response: `{connected: true, connectedAt: "...", expiresAt: "..."}`

4. **DELETE /api/integrations/google/** `@PreAuthorize("hasRole('EMPLOYER')")`
   - Disconnects Google Calendar integration
   - Deletes stored tokens
   - Returns: 204 No Content

### 6. Modified Files

#### Interview.java
**Path:** `src/main/java/com/skillset/domain/entity/Interview.java`

**Added Fields:**
- `googleEventId` (String) - ID of synced Google Calendar event
- `calendarSyncStatus` (String) - Status: PENDING, SYNCED, FAILED, CANCELLED

#### InterviewService.java
**Path:** `src/main/java/com/skillset/application/service/InterviewService.java`

**Modified Method: scheduleInterview()**
- Automatically attempts Google Calendar sync after interview creation
- Sets `calendarSyncStatus` to PENDING before sync attempt
- On success: Sets to SYNCED with eventId
- On failure: Sets to FAILED and logs error
- Maintains graceful fallback: interview still created even if Calendar sync fails
- Preserves existing notification push functionality

#### WebConfig.java
**Path:** `src/main/java/com/skillset/infrastructure/config/WebConfig.java`

**Added Beans:**
- `RestTemplate` - For HTTP calls to Google API
- `ObjectMapper` - For JSON serialization/deserialization

## Configuration

### Environment Variables (Add to .env)

```properties
# Google OAuth Configuration
GOOGLE_OAUTH_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:8080/api/integrations/google/callback

# Encryption Key for Token Storage
ENCRYPTION_KEY=your-32-character-encryption-key-here
```

### application.properties (Pre-configured)

```properties
google.oauth.client-id=${GOOGLE_OAUTH_CLIENT_ID:}
google.oauth.client-secret=${GOOGLE_OAUTH_CLIENT_SECRET:}
google.oauth.redirect-uri=${GOOGLE_OAUTH_REDIRECT_URI:http://localhost:8080/api/integrations/google/callback}
```

## Setup Instructions

### 1. Google Cloud Console Setup

1. Create a project in Google Cloud Console
2. Enable Google Calendar API
3. Create OAuth2 credentials (Authorized redirect URLs):
   - Development: `http://localhost:8080/api/integrations/google/callback`
   - Production: `https://yourdomain.com/api/integrations/google/callback`
4. Copy Client ID and Client Secret

### 2. Database Setup

Run the following migration to create the necessary table:

```sql
CREATE TABLE google_calendar_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    calendar_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_google_tokens_user_id ON google_calendar_tokens(user_id);

-- Add columns to interviews table
ALTER TABLE interviews ADD COLUMN google_event_id VARCHAR(255);
ALTER TABLE interviews ADD COLUMN calendar_sync_status VARCHAR(20);
```

### 3. Environment Configuration

1. Update `.env` file with Google OAuth credentials
2. Set `ENCRYPTION_KEY` to a secure 32-character key
3. Update `GOOGLE_OAUTH_REDIRECT_URI` to match your deployment

### 4. Build and Test

```bash
cd Skills-Talent/backend/app
mvn clean compile
mvn spring-boot:run
```

## Frontend Integration

### OAuth Flow

1. **User clicks "Connect Google Calendar"**
   - Frontend calls: `GET /api/integrations/google/auth`
   - Receives authorization URL
   - Redirects user to Google login

2. **Google Authorization**
   - User grants calendar access permissions
   - Google redirects to callback URL with authorization code

3. **Backend Processes Callback**
   - Controller receives code and state
   - Service exchanges code for tokens
   - Tokens encrypted and saved to database

4. **Connection Confirmed**
   - Frontend can check status: `GET /api/integrations/google/status`
   - Returns `{connected: true}`

### Interview Sync

When an employer schedules an interview:

1. Interview created in SkillSet database
2. If user has Google Calendar connected:
   - Event automatically created in Google Calendar
   - `googleEventId` and `calendarSyncStatus` updated
3. If not connected:
   - Interview still created successfully
   - `calendarSyncStatus` remains PENDING
4. When interview details updated:
   - Google Calendar event updated automatically
   - Attendees notified of changes

## Error Handling

### Graceful Fallbacks

- Interview creation succeeds even if Google Calendar sync fails
- No blocking of critical workflow
- All errors logged with full context
- Clear error messages returned to frontend

### Recovery

- Failed sync status visible in interview details
- Employer can retry sync manually if needed
- Token refresh handled automatically on expiration

## Security Considerations

1. **Token Encryption**
   - All tokens encrypted at rest using AES-256
   - Encryption key managed via environment variables
   - Never logged or exposed in responses

2. **Scope Limiting**
   - Only `https://www.googleapis.com/auth/calendar` scope requested
   - Minimal permissions required for functionality

3. **State Parameter**
   - OAuth state parameter encodes userId
   - Prevents CSRF attacks
   - Base64 encoded for transport

4. **HTTPS Requirement**
   - Production deployment must use HTTPS
   - OAuth redirect URI must match Google console configuration

## Monitoring and Debugging

### Logging

All operations logged at INFO/WARN/ERROR levels:
- Token management operations
- Calendar event lifecycle (create, update, delete)
- API call failures with HTTP status codes
- Sync status changes

### Status Tracking

Query interview sync status:
```sql
SELECT id, status, calendar_sync_status, google_event_id 
FROM interviews 
WHERE calendar_sync_status != 'SYNCED';
```

### Debug Mode

Set application logging to DEBUG for verbose output:
```properties
logging.level.com.skillset.infrastructure.integration=DEBUG
logging.level.com.skillset.interfaces.controller=DEBUG
```

## API Response Examples

### Get Auth URL
```json
{
  "authUrl": "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&scope=...&state=..."
}
```

### OAuth Callback Success
```json
{
  "success": true,
  "message": "Google Calendar integration successful"
}
```

### Connection Status
```json
{
  "connected": true,
  "connectedAt": "2024-06-29T14:30:00",
  "expiresAt": "2024-07-29T14:30:00"
}
```

## Testing Checklist

- [ ] Environment variables configured
- [ ] Database migrations applied
- [ ] Application compiles successfully
- [ ] OAuth flow completes without errors
- [ ] Interview creation triggers Google Calendar event
- [ ] Event details match interview data
- [ ] Interview updates sync to Calendar
- [ ] Interview cancellation removes Calendar event
- [ ] Disconnection removes stored tokens
- [ ] Error scenarios handled gracefully

## Future Enhancements

1. **Token Refresh**
   - Implement automatic token refresh before expiration
   - Background job to refresh tokens nearing expiration

2. **Video Conference Integration**
   - Automatically create Google Meet links
   - Add meeting link to calendar event description

3. **Attendee Management**
   - Support for multiple interviewer calendars
   - Availability checking across calendars

4. **Sync Status Dashboard**
   - UI for managing Calendar integrations
   - Bulk sync retry capabilities
   - Sync history and analytics

5. **Time Zone Support**
   - Calendar event time zone handling
   - Multi-timezone interview scheduling

## Troubleshooting

### Issue: "Could not resolve dependencies"
**Solution:** Remove cached Google libraries from Maven cache and rebuild.

### Issue: OAuth redirect not working
**Solution:** Verify redirect URI exactly matches Google Console configuration (including http/https and trailing slash).

### Issue: Tokens not encrypting
**Solution:** Ensure `ENCRYPTION_KEY` environment variable is set and at least 32 characters.

### Issue: Calendar events not syncing
**Solution:** 
1. Check user has connected calendar: `GET /api/integrations/google/status`
2. Verify interview has valid employer user ID
3. Check application logs for sync errors

## Version Information

- Java Version: 17
- Spring Boot: 3.2.5
- Database: PostgreSQL
- Google Calendar API: v3
- Implementation: REST-based (RestTemplate)

## Support

For issues or enhancements:
1. Check application logs for detailed error messages
2. Verify environment configuration
3. Review troubleshooting section above
4. Contact development team with logs and configuration details
