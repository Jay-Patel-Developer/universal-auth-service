-- Migration to add GDPR compliance fields to users table
-- Version: V1_2__Add_GDPR_Fields_To_Users_Table.sql

-- Add GDPR-related columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS scheduled_deletion_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_reason TEXT;

-- Create indexes for GDPR queries
CREATE INDEX IF NOT EXISTS idx_users_deletion_requested_at ON users (deletion_requested_at);
CREATE INDEX IF NOT EXISTS idx_users_scheduled_deletion_at ON users (scheduled_deletion_at);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users (last_login);