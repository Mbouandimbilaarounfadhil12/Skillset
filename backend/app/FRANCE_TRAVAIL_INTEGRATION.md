# France Travail Jobboard Integration

This document describes the France Travail jobboard integration for the SkillSet ATS platform.

## Overview

The France Travail integration allows SkillSet employers to publish and manage job listings on the France Travail (Pôle Emploi) jobboard directly from the SkillSet platform.

## Components

### 1. FranceTravailService
**Location:** `infrastructure/integration/FranceTravailService.java`

Main service handling all France Travail API interactions with comprehensive error handling and logging.

**Dependencies:**
- `RestTemplate` - HTTP client for API calls
- `JobListingRepository` - Data access layer
- `ObjectMapper` - JSON parsing (Jackson)

**Key Methods:**

#### `publishJobListing(String jobId) -> FranceTravailPublishResponse`
- Fetches job listing from database
- Maps SkillSet fields to France Travail API format
- Publishes to France Travail via POST to `/offres` endpoint
- Updates JobListing entity with France Travail ID and publication URL
- Returns response containing franceTravailId and publicUrl
- Throws `FranceTravailException` on failure

**Field Mappings:**
```
SkillSet Field          → France Travail Field
job.title              → intitule
job.description        → description (max 2000 chars)
job.jobType            → typeContrat (CDI/CDD/STAGE/FREELANCE)
job.salary             → salaire {libelle: "min-max €"}
job.location           → lieuTravail {libelle: "location"}
job.requiredSkills     → competences (array)
job.responsibilities   → missions
LocalDateTime.now()    → datePublication
```

#### `unpublishJobListing(String jobId) -> boolean`
- Checks if job is published on France Travail
- Calls DELETE endpoint to remove listing
- Updates JobListing entity to mark as unpublished
- Returns success/failure status
- Handles errors gracefully

#### `syncJobStatus(String jobId) -> JobListing`
- Fetches job status from France Travail API
- Compares with local status
- Updates local JobListing if different
- Used to synchronize published job status with France Travail
- Returns updated JobListing

### 2. JobboardController
**Location:** `interfaces/controller/JobboardController.java`

REST API endpoints for France Travail integration with role-based access control.

**Annotations:**
- `@PreAuthorize("hasRole('EMPLOYER')")` - Only employers can access
- `@AuthenticationPrincipal String userId` - For ownership verification

**Endpoints:**

#### POST `/api/jobboards/france-travail/publish/{jobId}`
**Request:** Job ID in path
**Response:** 
```json
{
  "franceTravailId": "string",
  "publicUrl": "string",
  "status": "published",
  "message": "Job successfully published to France Travail"
}
```
**Status Codes:**
- 201 Created - Publication successful
- 403 Forbidden - User doesn't own the job
- 404 Not Found - Job not found
- 400 Bad Request - Invalid request to France Travail
- 401 Unauthorized - Authentication failed
- 429 Too Many Requests - Rate limited
- 500 Internal Server Error - Unexpected error

#### DELETE `/api/jobboards/france-travail/unpublish/{jobId}`
**Request:** Job ID in path
**Response:** 204 No Content
**Status Codes:**
- 204 No Content - Unpublish successful
- 400 Bad Request - Job not published or unpublish failed
- 403 Forbidden - User doesn't own the job
- 404 Not Found - Job not found
- 500 Internal Server Error - Unexpected error

#### GET `/api/jobboards/france-travail/status/{jobId}`
**Request:** Job ID in path
**Response:**
```json
{
  "jobId": "string",
  "title": "string",
  "franceTravailId": "string",
  "publishedOnFranceTravail": boolean,
  "franceTravailUrl": "string",
  "postedAt": "ISO DateTime",
  "status": "OPEN|CLOSED|DRAFT"
}
```
**Status Codes:**
- 200 OK - Status retrieved successfully
- 403 Forbidden - User doesn't own the job
- 404 Not Found - Job not found
- 500 Internal Server Error - Unexpected error

### 3. JobListing Entity
**Location:** `domain/entity/JobListing.java`

Added France Travail-specific fields:

```java
@Column(name = "france_travail_id")
private String franceTravailId;  // ID from France Travail API

@Column(name = "published_on_france_travail")
private Boolean publishedOnFranceTravail = false;  // Publication flag

@Column(name = "france_travail_url")
private String franceTravailUrl;  // Public URL on France Travail
```

### 4. FranceTravailException
**Location:** `infrastructure/util/FranceTravailException.java`

Custom exception for France Travail API-related errors with HTTP status codes.

```java
public FranceTravailException(String message)                        // Default 500
public FranceTravailException(String message, int httpStatus)        // Custom status
public FranceTravailException(String message, Throwable cause)       // With cause
public FranceTravailException(String message, int httpStatus, Throwable cause)
```

## Configuration

### Application Properties
**File:** `application.properties`

```properties
# France Travail API Base URL
france-travail.api.base-url=${FRANCE_TRAVAIL_API_BASE_URL:https://api.francetravail.io/partenaire/offresdemploi/v2}

# France Travail OAuth2 Credentials
france-travail.api.client-id=${FRANCE_TRAVAIL_CLIENT_ID:}
france-travail.api.client-secret=${FRANCE_TRAVAIL_CLIENT_SECRET:}

# Integration Timeouts
integration.api.timeout=${INTEGRATION_API_TIMEOUT:30000}
integration.api.max-retries=${INTEGRATION_API_MAX_RETRIES:3}
```

### Environment Variables
Required for production deployment:
- `FRANCE_TRAVAIL_API_BASE_URL` - France Travail API endpoint
- `FRANCE_TRAVAIL_CLIENT_ID` - OAuth2 client ID
- `FRANCE_TRAVAIL_CLIENT_SECRET` - OAuth2 client secret

## Database Schema

### Migration Script
**File:** `db/migration/V004__add_france_travail_fields.sql`

Adds three columns to `job_listings` table:
- `france_travail_id VARCHAR(255)` - France Travail job ID
- `published_on_france_travail BOOLEAN` - Publication status flag
- `france_travail_url VARCHAR(500)` - Public URL on France Travail

Index created on `france_travail_id` for efficient lookups.

## API Request/Response Examples

### Publish Job Request
```json
POST /api/jobboards/france-travail/publish/job-uuid-123
Authorization: Bearer <jwt-token>

Response (201 Created):
{
  "franceTravailId": "ft-123456",
  "publicUrl": "https://www.francetravail.fr/offres/ft-123456",
  "status": "published",
  "message": "Job successfully published to France Travail"
}
```

### Get Status Request
```json
GET /api/jobboards/france-travail/status/job-uuid-123
Authorization: Bearer <jwt-token>

Response (200 OK):
{
  "jobId": "job-uuid-123",
  "title": "Senior Software Engineer",
  "franceTravailId": "ft-123456",
  "publishedOnFranceTravail": true,
  "franceTravailUrl": "https://www.francetravail.fr/offres/ft-123456",
  "postedAt": "2024-06-29T10:30:00",
  "status": "OPEN"
}
```

### Unpublish Job Request
```json
DELETE /api/jobboards/france-travail/unpublish/job-uuid-123
Authorization: Bearer <jwt-token>

Response (204 No Content)
```

## Error Handling

### HTTP Status Code Mapping

| Scenario | HTTP Status | Reason |
|----------|-------------|--------|
| Authentication failed | 401 Unauthorized | Invalid credentials |
| Rate limit exceeded | 429 Too Many Requests | API quota exceeded |
| Invalid job data | 400 Bad Request | Field validation failed |
| Job not found | 404 Not Found | Job doesn't exist |
| Server error | 500 Internal Server Error | France Travail API error |

### Error Response Format
```json
{
  "error": "Error message describing what went wrong",
  "timestamp": 1719662400000
}
```

## Logging

All France Travail operations are logged at DEBUG and INFO levels:

```
DEBUG: Publishing job {jobId} to France Travail with URL: ...
INFO: Successfully published job {jobId} to France Travail with ID: {franceTravailId}
ERROR: FranceTravailException while publishing job {jobId}: {message}
```

## Security Considerations

1. **Access Control**: Only employers (EMPLOYER role) can access France Travail endpoints
2. **Ownership Verification**: Each endpoint verifies the authenticated user owns the job
3. **Rate Limiting**: Implement API rate limiting to avoid hitting France Travail quotas
4. **Credentials**: Store API credentials in environment variables, never in code
5. **HTTPS**: All communications with France Travail API use HTTPS

## Testing

### Unit Test Template
```java
@ExtendWith(MockitoExtension.class)
class FranceTravailServiceTest {
    @Mock private RestTemplate restTemplate;
    @Mock private JobListingRepository jobListingRepository;
    @InjectMocks private FranceTravailService service;
    
    @Test
    void testPublishJobListing() {
        // Create test JobListing
        // Mock repository response
        // Mock RestTemplate response
        // Assert franceTravailId extracted correctly
    }
}
```

### Integration Test Template
```java
@SpringBootTest
@AutoConfigureMockMvc
class JobboardControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private FranceTravailService franceTravailService;
    
    @Test
    void testPublishJobEndpoint() throws Exception {
        // Given: authenticated employer
        // When: POST /api/jobboards/france-travail/publish/{jobId}
        // Then: returns 201 with response body
    }
}
```

## Troubleshooting

### Job Not Publishing
**Symptoms:** 500 Internal Server Error

**Debugging Steps:**
1. Check France Travail API credentials in `.env`
2. Verify job has all required fields (title, description, jobType, location, salary)
3. Check application logs for detailed error message
4. Ensure France Travail API is accessible (not rate limited)

### Job Published But URL Incorrect
**Symptoms:** `publicUrl` doesn't match actual France Travail page

**Solution:**
1. Verify `franceTravailId` extraction in `extractFranceTravailId()`
2. Check France Travail URL format documentation
3. Test `generatePublicUrl()` with known IDs

### Database Migration Failures
**Symptoms:** Application startup fails with column errors

**Solution:**
1. Check if `db/migration/V004__add_france_travail_fields.sql` exists
2. Ensure Flyway migrations are enabled
3. Verify database user has ALTER TABLE permissions

## Future Enhancements

1. **Batch Publishing**: Publish multiple jobs in one request
2. **Webhook Support**: Receive notifications from France Travail about job status changes
3. **Advanced Filtering**: Filter publishable jobs by criteria
4. **Analytics**: Track job performance on France Travail
5. **Auto-Renewal**: Automatically renew job listings near expiration
6. **Scheduled Sync**: Periodic status synchronization with France Travail

## References

- France Travail API Documentation: https://api.francetravail.io/documentation
- OAuth2 Implementation: Spring Security Documentation
- REST API Best Practices: Richardson Maturity Model

## Support

For issues or questions about the France Travail integration:
1. Check the logs for detailed error messages
2. Review the troubleshooting section above
3. Consult France Travail API documentation
4. Contact the development team
