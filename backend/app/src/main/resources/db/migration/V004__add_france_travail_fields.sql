-- Add France Travail integration fields to job_listings table
ALTER TABLE job_listings
ADD COLUMN france_travail_id VARCHAR(255),
ADD COLUMN published_on_france_travail BOOLEAN DEFAULT FALSE,
ADD COLUMN france_travail_url VARCHAR(500);

-- Create index on france_travail_id for faster lookups
CREATE INDEX idx_france_travail_id ON job_listings(france_travail_id);
