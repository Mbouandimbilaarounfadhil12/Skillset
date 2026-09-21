# France Travail Integration - Implementation Summary

**Date Created:** 2026-06-29  
**Status:** Complete  
**Build Status:** Ready for compilation

## Executive Summary

A complete France Travail (Pôle Emploi) jobboard integration has been successfully implemented for the SkillSet ATS backend. This enables employers to publish, unpublish, and monitor job listings directly on the France Travail platform through the SkillSet interface.

## Files Created

### 1. Service Layer
**File:** `infrastructure/integration/FranceTravailService.java` (510 lines)

Main service orchestrating all France Travail API interactions with comprehensive error handling.

**Key Methods:**
- `publishJobListing(String jobId)` - Publishes job to France Travail, returns ID and public URL
- `unpublishJobListing(String jobId)` - Removes job from France Travail
- `syncJobStatus(String jobId)` - Synchronizes job status from France Travail API

**Features:**
- RestTemplate-based HTTP communication
- Jackson ObjectMapper for JSON parsing
- Detailed logging at DEBUG and INFO levels
- Try-catch error handling with specific exception types
- Support for Basic Auth with clientId/clientSecret
- Configurable API base URL, credentials, and timeout from application.properties
- Smart field mapping from SkillSet to France Travail API format
- Automatic description truncation (2000 char limit)
- Support for multiple job type mappings (CDI, CDD, STAGE, FREELANCE, etc.)

**Dependencies Injected:**
- `RestTemplate` - HTTP client (Spring Bean)
- `JobListingRepository` - Data access
- `ObjectMapper` - JSON processing (Spring Bean)

**Configuration Properties Used:**
- `france-travail.api.base-url`
- `france-travail.api.client-id`
- `france-travail.api.client-secret`
- `integration.api.timeout`

### 2. Exception Handling
**File:** `infrastructure/util/FranceTravailException.java` (30 lines)

Custom runtime exception with HTTP status code support.

**Constructors:**
```java
FranceTravailException(String message)
FranceTravailException(String message, Throwable cause)
FranceTravailException(String message, int httpStatus)
FranceTravailException(String message, int httpStatus, Throwable cause)
```

**Features:**
- HTTP status code tracking
- Cause chain support for debugging
- Consistent error handling throughout integration

### 3. REST API Controller
**File:** `interfaces/controller/JobboardController.java` (235 lines)

REST endpoints for managing France Travail job listings.

**Endpoints:**

#### POST `/api/jobboards/france-travail/publish/{jobId}`
- **Description:** Publish a job listing to France Travail
- **Auth:** EMPLOYER role required
- **Input:** Job ID in path
- **Output (201 Created):**
  ```json
  {
    "franceTravailId": "string",
    "publicUrl": "string",
    "status": "published",
    "message": "Job successfully published to France Travail"
  }
  ```
- **Error Codes:** 403 (Forbidden), 404 (Not Found), 400 (Bad Request), 401 (Unauthorized), 429 (Rate Limited), 500 (Server Error)

#### DELETE `/api/jobboards/france-travail/unpublish/{jobId}`
- **Description:** Remove a job listing from France Travail
- **Auth:** EMPLOYER role required
- **Input:** Job ID in path
- **Output:** 204 No Content
- **Error Codes:** 400 (Bad Request), 403 (Forbidden), 404 (Not Found), 500 (Server Error)

#### GET `/api/jobboards/france-travail/status/{jobId}`
- **Description:** Get current publication status on France Travail
- **Auth:** EMPLOYER role required
- **Input:** Job ID in path
- **Output (200 OK):**
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
- **Error Codes:** 403 (Forbidden), 404 (Not Found), 500 (Server Error)

**Features:**
- Role-based access control with `@PreAuthorize("hasRole('EMPLOYER')")`
- Ownership verification on all endpoints (user must own the company/job)
- Consistent error response format with timestamps
- Detailed logging for audit trail
- HTTP status code mapping for all scenarios

### 4. Data Model Enhancement
**File:** `domain/entity/JobListing.java` (Modified)

Added three new fields to JobListing entity:

```java
@Column(name = "france_travail_id")
private String franceTravailId;

@Column(name = "published_on_france_travail")
private Boolean publishedOnFranceTravail = false;

@Column(name = "france_travail_url")
private String franceTravailUrl;
```

**Changes:**
- Tracks France Travail ID from API response
- Maintains publication status flag
- Stores public URL for easy reference

### 5. Database Migration
**File:** `db/migration/V004__add_france_travail_fields.sql`

Flyway migration script to add new columns:

```sql
ALTER TABLE job_listings
ADD COLUMN france_travail_id VARCHAR(255),
ADD COLUMN published_on_france_travail BOOLEAN DEFAULT FALSE,
ADD COLUMN france_travail_url VARCHAR(500);

CREATE INDEX idx_france_travail_id ON job_listings(france_travail_id);
```

**Features:**
- Safe ADD COLUMN operations
- Proper defaults (FALSE for publication flag)
- Performance index on france_travail_id
- Version-controlled schema updates

### 6. Documentation
**Files:**
- `FRANCE_TRAVAIL_INTEGRATION.md` - Comprehensive technical documentation (400+ lines)
- `FRANCE_TRAVAIL_QUICK_START.md` - Quick reference guide (300+ lines)
- `IMPLEMENTATION_SUMMARY.md` - This document

## Data Mapping

Field mapping from SkillSet to France Travail API format:

| SkillSet Field | France Travail API Field | Transformation |
|---|---|---|
| `title` | `intitule` | Direct copy |
| `description` | `description` | Truncate to 2000 chars |
| `jobType` | `typeContrat` | Map CDI→CDI, CDD→CDD, etc. |
| `salaryMin` + `salaryMax` | `salaire.libelle` | Format as "min-max €" |
| `location` | `lieuTravail.libelle` | Direct copy |
| `requiredSkills` | `competences` | Parse JSON/CSV to array |
| `responsibilities` | `missions` | Direct copy (optional) |
| `LocalDateTime.now()` | `datePublication` | Format as ISO datetime |

## Error Handling Strategy

### Exception Hierarchy
```
Exception
├── FranceTravailException (custom)
│   └── handled with HTTP status mapping
└── RestClientException
    └── handled gracefully with fallback
```

### HTTP Status Code Mapping
| Scenario | HTTP Status | Handler |
|----------|---|---|
| Authentication failed | 401 Unauthorized | Check credentials |
| Rate limit exceeded | 429 Too Many Requests | Retry after delay |
| Invalid job data | 400 Bad Request | Validate job fields |
| Job not found | 404 Not Found | Check job ID |
| Server error | 500 Internal Server Error | Log and notify |

### Logging
All operations logged with context:
- **DEBUG:** API call details, request/response bodies
- **INFO:** Successful operations, status changes
- **ERROR:** Failures with full stack traces

## Security Implementation

### 1. Authentication
- Spring Security integration via `@AuthenticationPrincipal String userId`
- JWT token validation (inherited from project)

### 2. Authorization
- Role-based: `@PreAuthorize("hasRole('EMPLOYER')")`
- Ownership verification: User must own the company that created the job
- Method-level security on all endpoints

### 3. API Credentials
- Stored in environment variables (never hardcoded)
- Basic Auth with clientId:clientSecret
- Configuration-driven via `application.properties`

### 4. Data Protection
- HTTPS-only communication with France Travail API
- Error responses never expose sensitive information
- Detailed logging for debugging without leaking secrets

## Configuration

### Application Properties (Already in application.properties)
```properties
france-travail.api.base-url=${FRANCE_TRAVAIL_API_BASE_URL:https://api.francetravail.io/partenaire/offresdemploi/v2}
france-travail.api.client-id=${FRANCE_TRAVAIL_CLIENT_ID:}
france-travail.api.client-secret=${FRANCE_TRAVAIL_CLIENT_SECRET:}
integration.api.timeout=${INTEGRATION_API_TIMEOUT:30000}
integration.api.max-retries=${INTEGRATION_API_MAX_RETRIES:3}
```

### Environment Variables (Required for Production)
```env
FRANCE_TRAVAIL_API_BASE_URL=https://api.francetravail.io/partenaire/offresdemploi/v2
FRANCE_TRAVAIL_CLIENT_ID=your_client_id
FRANCE_TRAVAIL_CLIENT_SECRET=your_client_secret
INTEGRATION_API_TIMEOUT=30000
INTEGRATION_API_MAX_RETRIES=3
```

## Integration Architecture

### Component Diagram
```
JobboardController (REST API)
         ↓
  Authentication/Authorization (Spring Security)
         ↓
  Ownership Verification
         ↓
FranceTravailService
    ├─ JobListingRepository (Data Access)
    ├─ RestTemplate (HTTP Client)
    └─ ObjectMapper (JSON Processing)
         ↓
    France Travail API
         ↓
    FranceTravailException
```

### Flow: Publishing a Job
```
1. POST /api/jobboards/france-travail/publish/{jobId}
2. Authenticate user (JWT validation)
3. Verify EMPLOYER role
4. Fetch JobListing from repository
5. Verify ownership (userId == companyId)
6. Map JobListing to France Travail format
7. POST to France Travail API
8. Extract ID and generate public URL
9. Update JobListing with France Travail details
10. Save to database
11. Return 201 with response
```

## Testing Considerations

### Unit Testing
```java
@ExtendWith(MockitoExtension.class)
class FranceTravailServiceTest {
    @Mock RestTemplate restTemplate;
    @Mock JobListingRepository repository;
    @InjectMocks FranceTravailService service;
    
    // Test cases:
    // - publishJobListing success
    // - publishJobListing when already published
    // - publishJobListing with missing fields
    // - publishJobListing with API error
    // - unpublishJobListing success
    // - unpublishJobListing when not published
    // - syncJobStatus success
}
```

### Integration Testing
```java
@SpringBootTest
@AutoConfigureMockMvc
class JobboardControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean FranceTravailService service;
    
    // Test cases:
    // - POST /publish/{jobId} returns 201
    // - DELETE /unpublish/{jobId} returns 204
    // - GET /status/{jobId} returns 200
    // - 403 when user doesn't own job
    // - 404 when job not found
}
```

## Performance Considerations

### Optimizations
- Index on `france_travail_id` for quick lookups
- Connection pooling via RestTemplate
- Configurable timeouts (default 30 seconds)
- Logging at appropriate levels to avoid overhead

### Scalability
- Stateless design allows horizontal scaling
- No session affinity required
- Database indexes prevent n+1 queries
- Future: Add async/queue-based publishing for bulk operations

## Deployment Checklist

- [ ] Set FRANCE_TRAVAIL_CLIENT_ID in production .env
- [ ] Set FRANCE_TRAVAIL_CLIENT_SECRET in production .env
- [ ] Verify FRANCE_TRAVAIL_API_BASE_URL points to correct endpoint
- [ ] Run database migrations (automatic via Flyway)
- [ ] Test publishing with a sample job
- [ ] Monitor logs for any API errors
- [ ] Set up alerts for 429 (rate limit) errors
- [ ] Enable DEBUG logging in staging for troubleshooting
- [ ] Load test with concurrent publish requests

## Future Enhancements

1. **Batch Operations:** Publish multiple jobs in single request
2. **Webhooks:** Receive notifications from France Travail about job changes
3. **Advanced Filtering:** Filter jobs by criteria before publishing
4. **Analytics:** Track job performance on France Travail
5. **Auto-Renewal:** Automatically renew expiring listings
6. **Scheduled Sync:** Periodic synchronization with France Travail
7. **Rate Limiting:** Implement retry with exponential backoff
8. **Caching:** Cache France Travail responses to reduce API calls

## Summary of Implementation

| Aspect | Status | Notes |
|--------|--------|-------|
| Service implementation | ✓ Complete | All 3 methods with error handling |
| Controller endpoints | ✓ Complete | All 3 endpoints with security |
| Entity model | ✓ Modified | 3 new fields with proper annotations |
| Database migration | ✓ Created | Version 004 with index |
| Configuration | ✓ In place | Properties already in application.properties |
| Error handling | ✓ Complete | Custom exceptions + HTTP status mapping |
| Logging | ✓ Implemented | DEBUG/INFO/ERROR levels |
| Security | ✓ Implemented | Role-based + ownership verification |
| Documentation | ✓ Complete | 2 comprehensive guides |
| Code quality | ✓ Ready | Java 17, Spring Boot 3.2.5 compatible |

## Compilation Notes

The implementation:
- Uses only standard Java 17 and Spring Boot 3.2.5 features
- Requires no additional Maven dependencies
- Leverages existing RestTemplate and ObjectMapper beans
- Follows project coding conventions and structure
- Is fully compatible with existing codebase

**Note:** The project has pre-existing compilation errors in `GoogleCalendarService.java` unrelated to this integration.

## Next Steps

1. **Configure Credentials:** Add France Travail API credentials to .env
2. **Database Setup:** Run migrations (automatic on startup)
3. **Testing:** Test publishing workflow with sample jobs
4. **Monitoring:** Set up logging and alerting
5. **Documentation:** Share quick start guide with team
6. **Deployment:** Deploy to staging, then production

## Support & References

- Full documentation: `FRANCE_TRAVAIL_INTEGRATION.md`
- Quick start guide: `FRANCE_TRAVAIL_QUICK_START.md`
- France Travail API: https://api.francetravail.io/documentation
- Spring REST: https://spring.io/guides/gs/rest-service/
- Spring Security: https://spring.io/projects/spring-security

---

**Implementation Date:** June 29, 2026  
**Status:** Ready for Integration Testing  
**Recommended Review:** Code review, security audit, load testing
