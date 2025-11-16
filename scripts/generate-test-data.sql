-- Generate test data for payment gateway

-- Insert test merchant
INSERT INTO merchants (merchant_code, business_name, email, api_key_hash, status)
VALUES (
    'TEST_MERCHANT',
    'Test Merchant Inc',
    'test@merchant.com',
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', -- SHA-256 of 'test-api-key'
    'ACTIVE'
) ON CONFLICT (merchant_code) DO NOTHING;

-- Insert sample transactions
INSERT INTO transactions (
    merchant_id,
    idempotency_key,
    amount,
    currency,
    status,
    payment_method,
    description,
    customer_email
)
SELECT 
    1,
    'test-txn-' || generate_series,
    (random() * 1000 + 10)::numeric(19,4),
    'USD',
    CASE (random() * 4)::int
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'PROCESSING'
        WHEN 2 THEN 'COMPLETED'
        WHEN 3 THEN 'FAILED'
        ELSE 'COMPLETED'
    END,
    CASE (random() * 3)::int
        WHEN 0 THEN 'CREDIT_CARD'
        WHEN 1 THEN 'DEBIT_CARD'
        WHEN 2 THEN 'BANK_TRANSFER'
        ELSE 'WALLET'
    END,
    'Test transaction #' || generate_series,
    'customer' || generate_series || '@example.com'
FROM generate_series(1, 50);

-- Update some transactions to have completed_at timestamps
UPDATE transactions
SET completed_at = created_at + INTERVAL '5 minutes'
WHERE status = 'COMPLETED' AND completed_at IS NULL;

-- Insert transaction history for completed transactions
INSERT INTO transaction_history (transaction_id, from_status, to_status, reason, changed_by)
SELECT 
    id,
    'PENDING',
    'PROCESSING',
    'Payment processing started',
    'SYSTEM'
FROM transactions
WHERE status IN ('PROCESSING', 'COMPLETED', 'FAILED');

INSERT INTO transaction_history (transaction_id, from_status, to_status, reason, changed_by)
SELECT 
    id,
    'PROCESSING',
    status,
    CASE status
        WHEN 'COMPLETED' THEN 'Payment successful'
        WHEN 'FAILED' THEN 'Payment failed'
        ELSE 'Processing'
    END,
    'SYSTEM'
FROM transactions
WHERE status IN ('COMPLETED', 'FAILED');

