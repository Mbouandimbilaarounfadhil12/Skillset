# France Travail Integration - Quick Start Guide

## What Was Added

This integration enables SkillSet employers to publish job listings directly to France Travail (Pôle Emploi) jobboard.

## New Files Created

### Core Service Files
1. **`infrastructure/integration/FranceTravailService.java`**
   - Service managing all France Travail API interactions
   - Methods: `publishJobListing()`, `unpublishJobListing()`, `syncJobStatus()`

2. **`infrastructure/util/FranceTravailException.java`**
   - Custom exception for France Travail-specific errors
   - Includes HTTP status codes for proper error handling

3. **`interfaces/controller/JobboardController.java`**
   - REST API endpoints for job publishing
   - Endpoints: POST `/publish/{jobId}`, DELETE `/unpublish/{jobId}`, GET `/status/{jobId}`
   - Role-based access control: `@PreAuthorize("hasRole('EMPLOYER')")`

4. **`domain/entity/JobListing.java` (MODIFIED)**
   - Added three new fields:
     - `franceTravailId` - ID from France Travail API
     - `publishedOnFranceTravail` - Publication status flag
     - `franceTravailUrl` - Public URL on France Travail

5. **`db/migration/V004__add_france_travail_fields.sql`**
   - Database migration script to add new columns
   - Creates index on `france_travail_id` for performance

## Environment Setup

### 1. Set Environment Variables

Add to your `.env` file:
```env
FRANCE_TRAVAIL_API_BASE_URL=https://api.francetravail.io/partenaire/offresdemploi/v2
FRANCE_TRAVAIL_CLIENT_ID=your_client_id_here
FRANCE_TRAVAIL_CLIENT_SECRET=your_client_secret_here
INTEGRATION_API_TIMEOUT=30000
INTEGRATION_API_MAX_RETRIES=3
```

### 2. Database Setup

The application will automatically run the migration on startup (via Flyway). To manually run it:

```sql
-- Add France Travail integration fields to job_listings table
ALTER TABLE job_listings
ADD COLUMN france_travail_id VARCHAR(255),
ADD COLUMN published_on_france_travail BOOLEAN DEFAULT FALSE,
ADD COLUMN france_travail_url VARCHAR(500);

-- Create index on france_travail_id for faster lookups
CREATE INDEX idx_france_travail_id ON job_listings(france_travail_id);
```

## API Usage Examples

### 1. Publish a Job

```bash
curl -X POST http://localhost:8080/api/jobboards/france-travail/publish/{jobId} \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json"
```

**Response (201 Created):**
```json
{
  "franceTravailId": "ft-123456789",
  "publicUrl": "https://www.francetravail.fr/offres/ft-123456789",
  "status": "published",
  "message": "Job successfully published to France Travail"
}
```

### 2. Get Job Status

```bash
curl -X GET http://localhost:8080/api/jobboards/france-travail/status/{jobId} \
  -H "Authorization: Bearer {jwt_token}"
```

**Response (200 OK):**
```json
{
  "jobId": "job-uuid-123",
  "title": "Senior Software Engineer",
  "franceTravailId": "ft-123456789",
  "publishedOnFranceTravail": true,
  "franceTravailUrl": "https://www.francetravail.fr/offres/ft-123456789",
  "postedAt": "2024-06-29T10:30:00",
  "status": "OPEN"
}
```

### 3. Unpublish a Job

```bash
curl -X DELETE http://localhost:8080/api/jobboards/france-travail/unpublish/{jobId} \
  -H "Authorization: Bearer {jwt_token}"
```

**Response (204 No Content):**
- No response body, just status code 204

## Field Mapping Reference

When publishing a job, SkillSet fields are automatically mapped to France Travail format:

| SkillSet Field | France Travail Field | Details |
|---|---|---|
| `job.title` | `intitule` | Job title |
| `job.description` | `description` | Description (max 2000 chars) |
| `job.jobType` | `typeContrat` | CDI, CDD, STAGE, FREELANCE |
| `job.salaryMin`/`job.salaryMax` | `salaire` | Range like "30000-50000 €" |
| `job.location` | `lieuTravail` | Work location |
| `job.requiredSkills` | `competences` | Array of required skills |
| `job.responsibilities` | `missions` | Job responsibilities |
| Current time | `datePublication` | ISO datetime of publication |

## Common Errors & Solutions

### 401 Unauthorized
**Cause:** Invalid or missing France Travail credentials
**Solution:** Verify `FRANCE_TRAVAIL_CLIENT_ID` and `FRANCE_TRAVAIL_CLIENT_SECRET` in `.env`

### 429 Too Many Requests
**Cause:** France Travail API rate limit exceeded
**Solution:** Wait a few minutes before retrying, implement backoff strategy

### 400 Bad Request
**Cause:** Job missing required fields or invalid data
**Solution:** Ensure job has: title, description, jobType, location, salaryMin, salaryMax

### 404 Not Found
**Cause:** Job doesn't exist in database
**Solution:** Verify the `jobId` is correct and the job hasn't been deleted

### 500 Internal Server Error
**Cause:** Unexpected server-side error
**Solution:** Check application logs for detailed error message

## Testing the Integration

### Manual Test Flow
1. Create a job listing via POST `/api/jobs`
2. Get the returned `jobId`
3. Publish the job: POST `/api/jobboards/france-travail/publish/{jobId}`
4. Verify `franceTravailId` is returned
5. Check job status: GET `/api/jobboards/france-travail/status/{jobId}`
6. Unpublish the job: DELETE `/api/jobboards/france-travail/unpublish/{jobId}`
7. Verify `publishedOnFranceTravail` is now false

### Key Properties to Validate
- ✓ Job exists in database
- ✓ User has EMPLOYER role
- ✓ User owns the company that owns the job
- ✓ France Travail credentials are valid
- ✓ Job has all required fields populated
- ✓ Network connection to France Travail API works

## Security Notes

1. **Role-Based Access**: Only users with `EMPLOYER` role can publish jobs
2. **Ownership Verification**: Users can only publish their own company's jobs
3. **Environment Variables**: Never hardcode API credentials
4. **HTTPS Only**: All communication with France Travail API uses HTTPS
5. **Rate Limiting**: Implement API rate limiting in production

## Dependencies

The integration uses:
- **Spring Boot 3.2.5** - Web framework
- **Spring Security** - Authentication & authorization
- **RestTemplate** - HTTP client (provided by Spring)
- **Jackson ObjectMapper** - JSON processing (provided by Spring)
- **Lombok** - Code generation
- **SLF4J** - Logging

No new dependencies need to be added to `pom.xml`.

## Logging

Enable DEBUG logging to see detailed France Travail integration logs:

```properties
# application.properties
logging.level.com.skillset.infrastructure.integration=DEBUG
logging.level.com.skillset.interfaces.controller=DEBUG
```

Look for logs containing:
- `FranceTravailService` - Service operation logs
- `JobboardController` - API endpoint logs

## Next Steps

1. Configure France Travail credentials in `.env`
2. Run database migrations (automatic on startup)
3. Build and test the application
4. Create test jobs and publish them
5. Monitor logs for any issues
6. Deploy to production with proper credentials

## Support & Documentation

- Full documentation: `FRANCE_TRAVAIL_INTEGRATION.md`
- France Travail API docs: https://api.francetravail.io/documentation
- Spring REST guide: https://spring.io/guides/gs/rest-service/

## File Locations Summary

```
Skills-Talent/backend/app/
├── src/main/java/com/skillset/
│   ├── domain/entity/
│   │   └── JobListing.java (MODIFIED - added 3 fields)
│   ├── infrastructure/
│   │   ├── integration/
│   │   │   └── FranceTravailService.java (NEW)
│   │   └── util/
│   │       └── FranceTravailException.java (NEW)
│   └── interfaces/controller/
│       └── JobboardController.java (NEW)
├── src/main/resources/
│   ├── application.properties (properties already added)
│   └── db/migration/
│       └── V004__add_france_travail_fields.sql (NEW)
└── FRANCE_TRAVAIL_INTEGRATION.md (NEW)
```
