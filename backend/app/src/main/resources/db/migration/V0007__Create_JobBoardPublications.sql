-- Create job_board_publications table to track publication attempts and results

CREATE TABLE job_board_publications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_listing_id UUID NOT NULL,
    partner VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED', -- PUBLISHED, FAILED, UNPUBLISHED
    external_id VARCHAR(255),
    external_url VARCHAR(2048),
    error_message TEXT,
    published_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes pour les requêtes fréquentes
CREATE INDEX idx_job_listing_id ON job_board_publications(job_listing_id);
CREATE INDEX idx_partner ON job_board_publications(partner);
CREATE INDEX idx_status ON job_board_publications(status);
CREATE INDEX idx_published_at ON job_board_publications(published_at);
CREATE UNIQUE INDEX idx_job_partner_unique ON job_board_publications(job_listing_id, partner);
