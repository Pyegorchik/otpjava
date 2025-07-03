-- Создание таблиц
CREATE TABLE IF NOT EXISTS public.users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    email VARCHAR(100),
    phone VARCHAR(20),
    telegram_chat_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.otp_config (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    code_length INTEGER NOT NULL DEFAULT 6 CHECK (code_length BETWEEN 4 AND 8),
    expiry_minutes INTEGER NOT NULL DEFAULT 5 CHECK (expiry_minutes > 0),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.otp_codes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(8) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
    delivery_method VARCHAR(20) NOT NULL CHECK (delivery_method IN ('SMS', 'EMAIL', 'TELEGRAM', 'FILE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

-- Создание индексов
CREATE INDEX IF NOT EXISTS idx_otp_codes_user_id ON otp_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_codes_status ON otp_codes(status);
CREATE INDEX IF NOT EXISTS idx_otp_codes_expires_at ON otp_codes(expires_at);

CREATE TABLE IF NOT EXISTS public.jwt_tokens (
    id SERIAL PRIMARY KEY,
    jti VARCHAR(50) NOT NULL UNIQUE, -- Unique token identifier
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_jwt_tokens_user_id ON jwt_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_jwt_tokens_jti ON jwt_tokens(jti);
CREATE INDEX IF NOT EXISTS idx_jwt_tokens_expires_at ON jwt_tokens(expires_at);

-- Вставка начальной конфигурации
INSERT INTO public.otp_config (id, code_length, expiry_minutes) 
VALUES (1, 6, 5) 
ON CONFLICT (id) DO NOTHING;