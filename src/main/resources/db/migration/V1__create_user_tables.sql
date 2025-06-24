-- Create database schema for user authentication service
-- PostgreSQL version

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    name VARCHAR(255),
    phone_number VARCHAR(255),
    status VARCHAR(50) CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED', 'PENDING_VERIFICATION')),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create oauth_providers table
CREATE TABLE oauth_providers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_name VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(255),
    refresh_token VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(provider_name, provider_id)
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_oauth_providers_user_id ON oauth_providers(user_id);
CREATE INDEX idx_oauth_providers_provider ON oauth_providers(provider_name, provider_id);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();