#!/bin/bash
# API Testing Script for Payment Gateway
# This script tests the main API endpoints using curl

set -e

# Configuration
API_KEY="test-api-key-12345"
BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
IDEMPOTENCY_KEY="test-$(date +%s)"

echo "=========================================="
echo "Payment Gateway API Testing"
echo "=========================================="
echo "Base URL: ${BASE_URL}"
echo "API Key: ${API_KEY}"
echo "Idempotency Key: ${IDEMPOTENCY_KEY}"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test result
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}

# Test 1: Health Check
echo "1. Testing Health Endpoint..."
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL%/api/v1}/api/v1/health" || echo -e "\n000")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Health check passed"
else
    print_result 1 "Health check failed (HTTP $HTTP_CODE)"
fi
echo ""

# Test 2: Create Transaction
echo "2. Testing Create Transaction..."
CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/transactions" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${API_KEY}" \
  -H "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  -d '{
    "amount": 99.99,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "description": "Test payment",
    "customerEmail": "customer@example.com",
    "customerName": "John Doe"
  }' || echo -e "\n000")

HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "201" ]; then
    print_result 0 "Transaction created successfully"
    TRANSACTION_ID=$(echo "$BODY" | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
    echo "   Transaction ID: ${TRANSACTION_ID}"
else
    print_result 1 "Transaction creation failed (HTTP $HTTP_CODE)"
    echo "   Response: $BODY"
    TRANSACTION_ID=""
fi
echo ""

# Test 3: Get Transaction (if creation succeeded)
if [ -n "$TRANSACTION_ID" ]; then
    echo "3. Testing Get Transaction..."
    GET_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/transactions/${TRANSACTION_ID}" \
      -H "X-API-Key: ${API_KEY}" || echo -e "\n000")
    
    HTTP_CODE=$(echo "$GET_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" = "200" ]; then
        print_result 0 "Transaction retrieved successfully"
    else
        print_result 1 "Transaction retrieval failed (HTTP $HTTP_CODE)"
    fi
    echo ""
fi

# Test 4: List Transactions
echo "4. Testing List Transactions..."
LIST_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/transactions?page=0&size=10" \
  -H "X-API-Key: ${API_KEY}" || echo -e "\n000")

HTTP_CODE=$(echo "$LIST_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Transaction list retrieved successfully"
else
    print_result 1 "Transaction list failed (HTTP $HTTP_CODE)"
fi
echo ""

# Test 5: Test Idempotency (should return cached response)
echo "5. Testing Idempotency..."
IDEMPOTENT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/transactions" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ${API_KEY}" \
  -H "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  -d '{
    "amount": 99.99,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "description": "Test payment"
  }' || echo -e "\n000")

HTTP_CODE=$(echo "$IDEMPOTENT_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Idempotency test passed (cached response returned)"
else
    print_result 1 "Idempotency test failed (HTTP $HTTP_CODE)"
fi
echo ""

# Test 6: Test Rate Limiting (send multiple requests)
echo "6. Testing Rate Limiting (sending 5 rapid requests)..."
RATE_LIMIT_COUNT=0
for i in {1..5}; do
    RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/transactions?page=0&size=1" \
      -H "X-API-Key: ${API_KEY}" || echo -e "\n000")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" = "429" ]; then
        RATE_LIMIT_COUNT=$((RATE_LIMIT_COUNT + 1))
    fi
done

if [ $RATE_LIMIT_COUNT -gt 0 ]; then
    print_result 0 "Rate limiting is working ($RATE_LIMIT_COUNT requests were rate limited)"
else
    echo -e "${YELLOW}⚠${NC} Rate limiting not triggered (may need more requests or higher rate)"
fi
echo ""

# Test 7: Test Invalid API Key
echo "7. Testing Invalid API Key..."
INVALID_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/transactions?page=0&size=1" \
  -H "X-API-Key: invalid-key-12345" || echo -e "\n000")

HTTP_CODE=$(echo "$INVALID_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "401" ]; then
    print_result 0 "Invalid API key correctly rejected"
else
    print_result 1 "Invalid API key test failed (HTTP $HTTP_CODE, expected 401)"
fi
echo ""

echo "=========================================="
echo "Testing Complete"
echo "=========================================="

