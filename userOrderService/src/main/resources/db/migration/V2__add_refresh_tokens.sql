CREATE TABLE refresh_token (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id UUID NOT NULL,
                               token_hash VARCHAR(512) UNIQUE NOT NULL,
                               expires_at TIMESTAMP NOT NULL,
                               revoked BOOLEAN NOT NULL DEFAULT FALSE,
                               replaced_by_token_hash VARCHAR(512),
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
