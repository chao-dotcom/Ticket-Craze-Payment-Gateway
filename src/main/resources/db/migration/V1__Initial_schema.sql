-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- MERCHANTS TABLE
-- =====================================================
CREATE TABLE merchants (
    id BIGSERIAL PRIMARY KEY,
    merchant_code VARCHAR(50) UNIQUE NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE')),
    webhook_url VARCHAR(500),
    webhook_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_merchant_code ON merchants(merchant_code);
CREATE INDEX idx_merchant_status ON merchants(status);

-- =====================================================
-- TRANSACTIONS TABLE (Core Entity)
-- =====================================================
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(255) NOT NULL,
    
    -- Financial fields
    amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Transaction details
    status VARCHAR(20) NOT NULL CHECK (status IN (
        'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED'
    )),
    payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN (
        'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'WALLET'
    )),
    description TEXT,
    
    -- Customer info (optional)
    customer_email VARCHAR(255),
    customer_name VARCHAR(255),
    
    -- Metadata (JSON for flexibility)
    metadata JSONB,
    
    -- Concurrency control
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    -- Composite unique constraint
    UNIQUE(merchant_id, idempotency_key)
);

CREATE INDEX idx_merchant_created ON transactions(merchant_id, created_at DESC);
CREATE INDEX idx_status ON transactions(status);
CREATE INDEX idx_transaction_id ON transactions(transaction_id);
CREATE INDEX idx_idempotency ON transactions(idempotency_key);
CREATE INDEX idx_customer_email ON transactions(customer_email);
CREATE INDEX idx_created_at ON transactions(created_at DESC);

-- =====================================================
-- TRANSACTION_HISTORY (Audit Trail)
-- =====================================================
CREATE TABLE transaction_history (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason TEXT,
    error_code VARCHAR(50),
    error_message TEXT,
    changed_by VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transaction_history_txn ON transaction_history(transaction_id, changed_at DESC);

-- =====================================================
-- REFUNDS TABLE
-- =====================================================
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    refund_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    transaction_id BIGINT NOT NULL REFERENCES transactions(id) ON DELETE RESTRICT,
    amount DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    initiated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_refund_transaction ON refunds(transaction_id);
CREATE INDEX idx_refund_status ON refunds(status);

-- =====================================================
-- IDEMPOTENCY_CACHE (For fast duplicate detection)
-- =====================================================
CREATE TABLE idempotency_cache (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    request_hash VARCHAR(64) NOT NULL,
    response_body TEXT NOT NULL,
    response_status_code INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_key ON idempotency_cache(idempotency_key);
CREATE INDEX idx_idempotency_expires ON idempotency_cache(expires_at);

-- =====================================================
-- WEBHOOK_EVENTS (Outbound notifications)
-- =====================================================
CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    transaction_id BIGINT REFERENCES transactions(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN (
        'TRANSACTION_COMPLETED', 'TRANSACTION_FAILED', 'REFUND_COMPLETED'
    )),
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP
);

CREATE INDEX idx_webhook_status ON webhook_events(status, next_retry_at);
CREATE INDEX idx_webhook_merchant ON webhook_events(merchant_id, created_at DESC);

-- =====================================================
-- API_RATE_LIMITS (For tracking usage)
-- =====================================================
CREATE TABLE api_rate_limits (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    window_start TIMESTAMP NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(merchant_id, window_start)
);

-- =====================================================
-- TRIGGERS FOR UPDATED_AT
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_merchants_updated_at
    BEFORE UPDATE ON merchants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

