#!/bin/bash
set -e

echo "Setting up local PostgreSQL database..."

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if container already exists
if docker ps -a | grep -q payment-gateway-db; then
    echo "Database container already exists. Starting it..."
    docker start payment-gateway-db
else
    echo "Creating new database container..."
    docker run -d \
        --name payment-gateway-db \
        -e POSTGRES_DB=payment_gateway \
        -e POSTGRES_USER=postgres \
        -e POSTGRES_PASSWORD=postgres \
        -p 5432:5432 \
        -v postgres_data:/var/lib/postgresql/data \
        postgres:15-alpine
    
    echo "Waiting for database to be ready..."
    sleep 5
fi

# Wait for database to be ready
until docker exec payment-gateway-db pg_isready -U postgres > /dev/null 2>&1; do
    echo "Waiting for database..."
    sleep 1
done

echo "Database is ready!"
echo "Connection string: jdbc:postgresql://localhost:5432/payment_gateway"
echo "Username: postgres"
echo "Password: postgres"

