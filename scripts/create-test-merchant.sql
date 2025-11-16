-- Create test merchant with API key for testing
-- This script creates a merchant that can be used for API testing

-- IMPORTANT: Replace the api_key_hash below with the actual SHA-256 hash of 'test-api-key-12345'
-- To generate the hash:
--   - On Linux/Mac: echo -n "test-api-key-12345" | sha256sum
--   - On Windows: Use HashUtil.java or online SHA-256 generator
--   - Or use: java -cp target/classes com.yourname.paymentgateway.util.HashUtil test-api-key-12345

INSERT INTO merchants (merchant_code, business_name, email, api_key_hash, status, created_at, updated_at)
VALUES (
    'TEST_MERCHANT_001',
    'Test Business LLC',
    'test@example.com',
    -- SHA-256 hash of 'test-api-key-12345'
    -- NOTE: This is a placeholder. Generate the actual hash using one of the methods above.
    -- Example hash format: 64 hex characters (e.g., 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3')
    'b3d4d8c0e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2',
    'ACTIVE',
    NOW(),
    NOW()
) ON CONFLICT (merchant_code) DO UPDATE
SET 
    business_name = EXCLUDED.business_name,
    email = EXCLUDED.email,
    api_key_hash = EXCLUDED.api_key_hash,
    status = EXCLUDED.status,
    updated_at = NOW();

-- Note: The API key for this merchant is: test-api-key-12345
-- Use this key in the X-API-Key header when making API requests
-- 
-- To generate the correct hash, you can use the HashUtil class:
--   String hash = HashUtil.sha256("test-api-key-12345");

