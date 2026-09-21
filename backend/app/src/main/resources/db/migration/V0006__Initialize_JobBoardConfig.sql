-- Initialize JobBoardConfig table with default country-to-jobboard mappings
-- Cette migration remplit la table avec les mappings pays → job boards

INSERT INTO job_board_configs (id, country_code, partner, is_active, priority, created_at, updated_at)
VALUES
    -- France
    (gen_random_uuid(), 'FR', 'FRANCE_TRAVAIL', true, 1, NOW(), NOW()),
    (gen_random_uuid(), 'FR', 'LINKEDIN', true, 2, NOW(), NOW()),

    -- Cameroon
    (gen_random_uuid(), 'CM', 'BRIGHTERMONDAY', true, 1, NOW(), NOW()),
    (gen_random_uuid(), 'CM', 'LINKEDIN', true, 2, NOW(), NOW()),
    (gen_random_uuid(), 'CM', 'JOBARTISAN', true, 3, NOW(), NOW()),

    -- Senegal
    (gen_random_uuid(), 'SN', 'BRIGHTERMONDAY', true, 1, NOW(), NOW()),
    (gen_random_uuid(), 'SN', 'LINKEDIN', true, 2, NOW(), NOW()),

    -- Côte d'Ivoire
    (gen_random_uuid(), 'CI', 'BRIGHTERMONDAY', true, 1, NOW(), NOW()),
    (gen_random_uuid(), 'CI', 'LINKEDIN', true, 2, NOW(), NOW()),

    -- Nigeria
    (gen_random_uuid(), 'NG', 'BRIGHTERMONDAY', true, 1, NOW(), NOW()),
    (gen_random_uuid(), 'NG', 'LINKEDIN', true, 2, NOW(), NOW()),

    -- Kenya
    (gen_random_uuid(), 'KE', 'BRIGHTERMONDAY', true, 1, NOW(), NOW()),
    (gen_random_uuid(), 'KE', 'LINKEDIN', true, 2, NOW(), NOW()),

    -- Fallback universel pour tous les autres pays
    (gen_random_uuid(), '*', 'LINKEDIN', true, 1, NOW(), NOW());
