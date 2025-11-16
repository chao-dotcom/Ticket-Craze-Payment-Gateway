#!/bin/bash
# Generate SHA-256 hash for API key
# Usage: ./generate-api-key-hash.sh "your-api-key"

if [ -z "$1" ]; then
    echo "Usage: $0 <api-key>"
    echo "Example: $0 test-api-key-12345"
    exit 1
fi

API_KEY="$1"

# Try different methods to generate SHA-256 hash
if command -v sha256sum &> /dev/null; then
    # Linux
    echo -n "$API_KEY" | sha256sum | cut -d' ' -f1
elif command -v shasum &> /dev/null; then
    # macOS
    echo -n "$API_KEY" | shasum -a 256 | cut -d' ' -f1
elif command -v openssl &> /dev/null; then
    # OpenSSL (available on most systems)
    echo -n "$API_KEY" | openssl dgst -sha256 | cut -d' ' -f2
else
    echo "Error: No SHA-256 utility found. Please install sha256sum, shasum, or openssl"
    exit 1
fi

