-- Migration to create security audit log table
-- Version: V1_3__Create_Security_Audit_Table.sql

CREATE TABLE IF NOT EXISTS security_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_email VARCHAR(255),
    client_ip VARCHAR(45),
    user_agent TEXT,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_security_audit_timestamp ON security_audit_log (timestamp);
CREATE INDEX IF NOT EXISTS idx_security_audit_user_email ON security_audit_log (user_email);
CREATE INDEX IF NOT EXISTS idx_security_audit_action ON security_audit_log (action);
CREATE INDEX IF NOT EXISTS idx_security_audit_client_ip ON security_audit_log (client_ip);
CREATE INDEX IF NOT EXISTS idx_security_audit_event_id ON security_audit_log (event_id);