#!/bin/bash
# Complete Payment Gateway Demo Script

API_KEY="test-api-key-12345"
BASE_URL="http://localhost:8080/api/v1"

echo "=== Payment Gateway Demo ==="
echo ""

# 1. Health Check
echo "1. Health Check:"
curl -s "$BASE_URL/../actuator/health" | jq '.' 2>/dev/null || curl -s "$BASE_URL/../actuator/health"
echo ""
echo ""

# 2. Create Transaction
echo "2. Creating Transaction:"
IDEMPOTENCY_KEY="demo-$(date +%s)"
RESPONSE=$(curl -s -X POST "$BASE_URL/transactions" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "amount": 150.50,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "description": "Demo transaction",
    "customerEmail": "customer@example.com",
    "customerName": "John Doe"
  }')
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
TRANSACTION_ID=$(echo "$RESPONSE" | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4 || echo "$RESPONSE" | jq -r '.transactionId' 2>/dev/null)
echo "Transaction ID: $TRANSACTION_ID"
echo ""
echo ""

# 3. Get Transaction
if [ -n "$TRANSACTION_ID" ] && [ "$TRANSACTION_ID" != "null" ]; then
  echo "3. Retrieving Transaction:"
  curl -s "$BASE_URL/transactions/$TRANSACTION_ID" \
    -H "X-API-Key: $API_KEY" | jq '.' 2>/dev/null || curl -s "$BASE_URL/transactions/$TRANSACTION_ID" -H "X-API-Key: $API_KEY"
  echo ""
  echo ""
  
  # 4. Test Idempotency
  echo "4. Testing Idempotency (duplicate request):"
  curl -s -X POST "$BASE_URL/transactions" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $API_KEY" \
    -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
    -d '{
      "amount": 150.50,
      "currency": "USD",
      "paymentMethod": "CREDIT_CARD"
    }' | jq '.' 2>/dev/null || echo "Response received"
  echo ""
  echo ""
  
  # 5. List Transactions
  echo "5. Listing Transactions:"
  curl -s "$BASE_URL/transactions?page=0&size=5" \
    -H "X-API-Key: $API_KEY" | jq '.content | length, .[0]' 2>/dev/null || curl -s "$BASE_URL/transactions?page=0&size=5" -H "X-API-Key: $API_KEY"
  echo ""
  echo ""
  
  # 6. Get Transaction History
  echo "6. Transaction History (waiting 3 seconds for processing...):"
  sleep 3
  curl -s "$BASE_URL/transactions/$TRANSACTION_ID/history" \
    -H "X-API-Key: $API_KEY" | jq '.' 2>/dev/null || curl -s "$BASE_URL/transactions/$TRANSACTION_ID/history" -H "X-API-Key: $API_KEY"
  echo ""
  echo ""
fi

# 7. Test Rate Limiting
echo "7. Testing Rate Limiting (sending 5 rapid requests):"
for i in {1..5}; do
  STATUS=$(curl -s -w "%{http_code}" -o /dev/null \
    -X GET "$BASE_URL/transactions?page=0&size=1" \
    -H "X-API-Key: $API_KEY")
  echo "Request $i: HTTP $STATUS"
  sleep 0.1
done
echo ""
echo ""

echo "=== Demo Complete ==="

