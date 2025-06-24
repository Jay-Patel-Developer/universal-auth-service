-- Migration to create MFA configuration table
-- Version: V1_1__Create_MFA_Configuration_Table.sql

CREATE TABLE IF NOT EXISTS mfa_configurations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    method VARCHAR(50) NOT NULL DEFAULT 'TOTP',
    secret_key VARCHAR(255),
    phone_number VARCHAR(20),
    recovery_codes TEXT,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mfa_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_mfa_configurations_user_id ON mfa_configurations (user_id);
CREATE INDEX IF NOT EXISTS idx_mfa_configurations_enabled ON mfa_configurations (enabled);