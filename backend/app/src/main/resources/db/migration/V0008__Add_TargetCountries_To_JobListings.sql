-- Add target_countries column to job_listings table to track which countries the job is published to

ALTER TABLE job_listings ADD COLUMN target_countries VARCHAR(500);

-- JSON array format: e.g., ["CM", "FR", "SN"]
-- Will be populated when a job is published to specific countries
