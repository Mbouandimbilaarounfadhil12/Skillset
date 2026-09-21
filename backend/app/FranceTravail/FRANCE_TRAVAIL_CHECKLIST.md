# France Travail Integration - Deployment Checklist

## Pre-Deployment Review

### Code Quality
- [x] All files created in correct package structure
- [x] Java 17 syntax compliance verified
- [x] Spring Boot 3.2.5 compatibility confirmed
- [x] No new Maven dependencies required
- [x] Proper use of Lombok annotations
- [x] Spring annotations correctly applied
- [x] Exception handling implemented
- [x] Logging with SLF4J in place
- [x] No hardcoded credentials
- [x] Follows project coding conventions

### Security Review
- [x] Role-based access control (@PreAuthorize)
- [x] Ownership verification on all endpoints
- [x] JWT authentication integration
- [x] API credentials in environment variables
- [x] HTTPS-only API communication
- [x] Error responses don't leak sensitive info
- [x] Input validation before API calls
- [x] Proper HTTP status codes

### Architecture Review
- [x] Clean separation of concerns (Service/Controller)
- [x] Dependency injection properly configured
- [x] No circular dependencies
- [x] RestTemplate bean reused (no duplication)
- [x] ObjectMapper bean reused (no duplication)
- [x] Database repository pattern followed
- [x] Error handling consistent
- [x] Logging appropriate levels

### Documentation
- [x] FRANCE_TRAVAIL_INTEGRATION.md (comprehensive)
- [x] FRANCE_TRAVAIL_QUICK_START.md (quick reference)
- [x] IMPLEMENTATION_SUMMARY.md (detailed)
- [x] CHANGES.md (change log)
- [x] Code comments on complex logic
- [x] Javadoc comments on public methods
- [x] API documentation in controller
- [x] Configuration comments

## Installation Checklist

### Database Setup
- [ ] Run migration V004 (automatic or manual)
- [ ] Verify new columns in job_listings table:
  - [ ] france_travail_id VARCHAR(255)
  - [ ] published_on_france_travail BOOLEAN
  - [ ] france_travail_url VARCHAR(500)
- [ ] Verify index created on france_travail_id
- [ ] No errors in Flyway logs

### Application Configuration
- [ ] Set FRANCE_TRAVAIL_API_BASE_URL in .env
- [ ] Set FRANCE_TRAVAIL_CLIENT_ID in .env
- [ ] Set FRANCE_TRAVAIL_CLIENT_SECRET in .env
- [ ] Verify application.properties loaded correctly
- [ ] Check timeout setting (default: 30000ms)
- [ ] Check retry setting (default: 3)

### Build Verification
- [ ] mvn clean compile succeeds
- [ ] No new compilation errors introduced
- [ ] JAR builds successfully
- [ ] No warnings in build log
- [ ] Verify all classes compiled

### Runtime Verification
- [ ] Application starts successfully
- [ ] Spring context loads without errors
- [ ] Beans autowired correctly
- [ ] No ClassNotFoundException
- [ ] No BeanCreationException
- [ ] Logging configured properly

## Integration Testing

### Manual API Tests
- [ ] Create a job listing via POST /api/jobs
- [ ] Record the jobId
- [ ] Test POST /api/jobboards/france-travail/publish/{jobId}
  - [ ] Returns 201 Created
  - [ ] Response contains franceTravailId
  - [ ] Response contains publicUrl
- [ ] Test GET /api/jobboards/france-travail/status/{jobId}
  - [ ] Returns 200 OK
  - [ ] Shows publishedOnFranceTravail = true
  - [ ] Shows correct franceTravailUrl
- [ ] Verify job appears on France Travail website
- [ ] Test DELETE /api/jobboards/france-travail/unpublish/{jobId}
  - [ ] Returns 204 No Content
- [ ] Verify job removed from France Travail

### Security Tests
- [ ] Test without JWT token (401 Unauthorized)
- [ ] Test with non-EMPLOYER role (403 Forbidden)
- [ ] Test with different user's job (403 Forbidden)
- [ ] Verify credentials not in logs
- [ ] Verify error messages safe (no API details)

### Error Scenario Tests
- [ ] Test with invalid jobId (404 Not Found)
- [ ] Test with missing fields (400 Bad Request)
- [ ] Test rate limit handling (429 Too Many Requests)
- [ ] Test with bad credentials (401 Unauthorized)
- [ ] Test network timeout (500 Internal Server Error)

### Performance Tests
- [ ] Single publish: under 2 seconds
- [ ] Single unpublish: under 2 seconds
- [ ] Single status check: under 1 second
- [ ] Monitor database indexes
- [ ] Check connection pooling
- [ ] Verify no memory leaks

## Production Deployment

### Pre-Production
- [ ] Code review completed
- [ ] Security review passed
- [ ] All tests passing
- [ ] Documentation reviewed
- [ ] Team trained on integration

### Production Setup
- [ ] France Travail API credentials obtained
- [ ] Credentials added to production env
- [ ] Database migrated successfully
- [ ] Application deployed to staging first
- [ ] Staging testing completed
- [ ] Rollback plan documented
- [ ] Support team briefed

### Deployment Steps
- [ ] Deploy JAR to production
- [ ] Verify application starts
- [ ] Check health endpoint
- [ ] Monitor logs for errors
- [ ] Run smoke tests
- [ ] Publish test job
- [ ] Verify on France Travail
- [ ] Unpublish test job
- [ ] Monitor error rates

### Post-Deployment
- [ ] Check application logs (no errors)
- [ ] Monitor API response times
- [ ] Monitor rate limit status
- [ ] Check database connections
- [ ] Verify all endpoints responding
- [ ] Monitor error alerts
- [ ] Check user feedback
- [ ] Schedule follow-up review

## Monitoring and Maintenance

### Logging Setup
- [ ] Configure DEBUG level for development
- [ ] Configure INFO/WARN for production
- [ ] Set up centralized logging
- [ ] Create alerts for ERROR logs
- [ ] Monitor FranceTravailException occurrences

### Metrics to Monitor
- [ ] API response times (target: less than 2 seconds)
- [ ] Rate limit hits (monitor 429 errors)
- [ ] Authentication failures (401 errors)
- [ ] Authorization failures (403 errors)
- [ ] Job publication success rate
- [ ] Database query performance
- [ ] Memory usage

### Alerts to Configure
- [ ] Consecutive 5xx errors
- [ ] Rate limit exceeded (429)
- [ ] Authentication failures (401)
- [ ] Database connection issues
- [ ] Application startup failures
- [ ] Deployment failures

### Regular Maintenance
- [ ] Weekly: Review logs for patterns
- [ ] Monthly: Analyze API usage metrics
- [ ] Quarterly: Review and optimize performance
- [ ] Yearly: Plan enhancements

## Rollback Procedure

If critical issues discovered:

1. **Immediate Actions**
   - Stop publishing new jobs
   - Alert support team
   - Disable France Travail endpoints if needed

2. **Assessment**
   - Identify issue root cause
   - Assess impact scope
   - Determine rollback necessity

3. **Rollback Steps**
   - Revert application code
   - Keep database columns (backward compatible)
   - Restart application
   - Verify functionality
   - Notify stakeholders

4. **Post-Rollback**
   - Fix issues in development
   - Thorough testing
   - Code review
   - Staged redeployment

## Handoff Checklist

### Documentation Handoff
- [ ] All documentation reviewed by team
- [ ] API documentation accessible
- [ ] Setup guide clear and complete
- [ ] Troubleshooting guide available
- [ ] Contact information provided

### Training Handoff
- [ ] Backend team trained
- [ ] DevOps team trained
- [ ] Support team trained
- [ ] Frontend team understands API
- [ ] Product team understands features

### Support Handoff
- [ ] Support procedures documented
- [ ] On-call runbook prepared
- [ ] Escalation path defined
- [ ] SLAs established
- [ ] Contact list updated

## Sign-Off

- Code review approved by: _________________ (Date: _____)
- Security review approved by: _________________ (Date: _____)
- Testing completed by: _________________ (Date: _____)
- Deployment approved by: _________________ (Date: _____)
- Go-live approved by: _________________ (Date: _____)

## Deployment Notes

Use this space for deployment notes, issues encountered, and resolutions:

Deployment Date: _________
Deployed By: _________
Notes:
- 
- 
- 

---

**Last Updated:** June 29, 2026  
**Status:** Ready for Deployment  
**Version:** 1.0
