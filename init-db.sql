-- =============================================================================
-- SalesCRM PostgreSQL Database Initialization & Seed Script
-- Compatible with PostgreSQL 14+ / Spring WebFlux / Spring Data R2DBC
-- =============================================================================

-- Create schema tables if not existing
CREATE TABLE IF NOT EXISTS campaign (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ad_set (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    campaign_id BIGINT REFERENCES campaign(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ad (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    ad_set_id BIGINT REFERENCES ad_set(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lead_form (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) NOT NULL UNIQUE,
    raw_phone_number VARCHAR(50),
    location VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lead (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    created_time TIMESTAMP WITH TIME ZONE NOT NULL,
    is_organic BOOLEAN NOT NULL DEFAULT FALSE,
    platform VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    notes TEXT,
    customer_id BIGINT REFERENCES customer(id) ON DELETE RESTRICT,
    ad_id BIGINT REFERENCES ad(id) ON DELETE SET NULL,
    lead_form_id BIGINT REFERENCES lead_form(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lead_preference (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL UNIQUE REFERENCES lead(id) ON DELETE CASCADE,
    mattress_type VARCHAR(100),
    mattress_size VARCHAR(50),
    budget_range VARCHAR(100),
    purchase_timeline VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indices
CREATE INDEX IF NOT EXISTS idx_adset_campaign_id ON ad_set(campaign_id);
CREATE INDEX IF NOT EXISTS idx_ad_adset_id ON ad(ad_set_id);
CREATE INDEX IF NOT EXISTS idx_lead_customer_id ON lead(customer_id);
CREATE INDEX IF NOT EXISTS idx_lead_ad_id ON lead(ad_id);
CREATE INDEX IF NOT EXISTS idx_lead_lead_form_id ON lead(lead_form_id);
CREATE INDEX IF NOT EXISTS idx_lead_created_time ON lead(created_time);
CREATE INDEX IF NOT EXISTS idx_lead_status ON lead(status);
CREATE INDEX IF NOT EXISTS idx_lead_platform ON lead(platform);
CREATE INDEX IF NOT EXISTS idx_customer_phone ON customer(phone_number);
CREATE INDEX IF NOT EXISTS idx_lead_preference_lead_id ON lead_preference(lead_id);

-- =============================================================================
-- Seed Data Transformed from Raw Excel Export
-- =============================================================================

-- 1. Campaign
INSERT INTO campaign (id, external_id, name, status, created_at, updated_at)
VALUES (1, 'c:52559644532780', 'Latax_mattress_Lead_form', 'ACTIVE', '2026-10-05T02:00:00Z', '2026-10-05T02:00:00Z')
ON CONFLICT (external_id) DO UPDATE SET name = EXCLUDED.name;

-- 2. AdSet
INSERT INTO ad_set (id, external_id, name, status, campaign_id, created_at)
VALUES (1, 'as:52559644533180', 'Leads_form', 'ACTIVE', 1, '2026-10-05T02:00:00Z')
ON CONFLICT (external_id) DO UPDATE SET name = EXCLUDED.name;

-- 3. Ad
INSERT INTO ad (id, external_id, name, status, ad_set_id, created_at)
VALUES (1, 'ag:52559644532980', 'Video_ad', 'ACTIVE', 1, '2026-10-05T02:00:00Z')
ON CONFLICT (external_id) DO UPDATE SET name = EXCLUDED.name;

-- 4. LeadForm
INSERT INTO lead_form (id, external_id, name, status, created_at)
VALUES (1, 'f:2366440540429976', 'latax_Mattress_5/10/2026', 'ACTIVE', '2026-10-05T02:00:00Z')
ON CONFLICT (external_id) DO UPDATE SET name = EXCLUDED.name;

-- 5. Customers
INSERT INTO customer (id, full_name, phone_number, raw_phone_number, location, created_at)
VALUES 
    (1, 'MKR EV MOTORS', '+917010202931', 'p:+917010202931', 'salem', '2026-10-05T02:24:13-05:00'),
    (2, 'Nagaraj Lexi', '+919750748427', 'p:+919750748427', 'namakkal', '2026-10-05T04:11:38-05:00'),
    (3, 'CATHERIN MARY', '+919731369877', 'p:+919731369877', 'namakkal', '2026-10-05T05:19:22-05:00')
ON CONFLICT (phone_number) DO UPDATE 
SET full_name = EXCLUDED.full_name, location = EXCLUDED.location;

-- 6. Leads
INSERT INTO lead (id, external_id, created_time, is_organic, platform, status, notes, customer_id, ad_id, lead_form_id, created_at, updated_at)
VALUES
    (1, 'l:3229496824107505', '2026-10-05T02:24:13-05:00', FALSE, 'IG', 'CREATED', 'Imported from Meta Ads Excel', 1, 1, 1, '2026-10-05T02:24:13-05:00', '2026-10-05T02:24:13-05:00'),
    (2, 'l:1126688279719555', '2026-10-05T04:11:38-05:00', FALSE, 'FB', 'CREATED', 'Imported from Meta Ads Excel', 2, 1, 1, '2026-10-05T04:11:38-05:00', '2026-10-05T04:11:38-05:00'),
    (3, 'l:1111406007947029', '2026-10-05T05:19:22-05:00', FALSE, 'IG', 'CREATED', 'Imported from Meta Ads Excel', 3, 1, 1, '2026-10-05T05:19:22-05:00', '2026-10-05T05:19:22-05:00')
ON CONFLICT (external_id) DO UPDATE
SET status = EXCLUDED.status, notes = EXCLUDED.notes;

-- 7. Lead Preferences (Product survey details)
INSERT INTO lead_preference (id, lead_id, mattress_type, mattress_size, budget_range, purchase_timeline, created_at)
VALUES
    (1, 1, 'latex_mattress', 'queen', '₹5,000_–_₹10,000', 'immediately_/_within_7_days', '2026-10-05T02:24:13-05:00'),
    (2, 2, 'latex_mattress', 'queen', '₹10,000_–_₹15,000', 'immediately_/_within_7_days', '2026-10-05T04:11:38-05:00'),
    (3, 3, 'latex_mattress', 'king', '₹15,000_–_₹25,000', 'immediately_/_within_7_days', '2026-10-05T05:19:22-05:00')
ON CONFLICT (lead_id) DO UPDATE
SET mattress_type = EXCLUDED.mattress_type,
    mattress_size = EXCLUDED.mattress_size,
    budget_range = EXCLUDED.budget_range,
    purchase_timeline = EXCLUDED.purchase_timeline;

-- Reset sequence IDs
SELECT setval('campaign_id_seq', (SELECT COALESCE(MAX(id), 1) FROM campaign));
SELECT setval('ad_set_id_seq', (SELECT COALESCE(MAX(id), 1) FROM ad_set));
SELECT setval('ad_id_seq', (SELECT COALESCE(MAX(id), 1) FROM ad));
SELECT setval('lead_form_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lead_form));
SELECT setval('customer_id_seq', (SELECT COALESCE(MAX(id), 1) FROM customer));
SELECT setval('lead_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lead));
SELECT setval('lead_preference_id_seq', (SELECT COALESCE(MAX(id), 1) FROM lead_preference));
