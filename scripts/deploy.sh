#!/bin/bash
set -e

echo "Deploying Payment Gateway to AWS..."

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "Error: AWS CLI is not installed. Please install it first."
    exit 1
fi

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Get AWS account ID and region
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=${AWS_REGION:-us-east-1}
ECR_REPO="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/payment-gateway"

echo "AWS Account ID: $AWS_ACCOUNT_ID"
echo "AWS Region: $AWS_REGION"
echo "ECR Repository: $ECR_REPO"

# Login to ECR
echo "Logging in to ECR..."
aws ecr get-login-password --region $AWS_REGION | \
    docker login --username AWS --password-stdin $ECR_REPO

# Build Docker image
echo "Building Docker image..."
docker build -t payment-gateway:latest .

# Tag image
echo "Tagging image..."
docker tag payment-gateway:latest $ECR_REPO:latest
docker tag payment-gateway:latest $ECR_REPO:$(git rev-parse --short HEAD)

# Push to ECR
echo "Pushing image to ECR..."
docker push $ECR_REPO:latest
docker push $ECR_REPO:$(git rev-parse --short HEAD)

echo "Deployment complete!"
echo "Image pushed to: $ECR_REPO:latest"

# Trigger instance refresh if ASG exists
if aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names payment-gateway-asg --region $AWS_REGION &> /dev/null; then
    echo "Triggering Auto Scaling Group instance refresh..."
    aws autoscaling start-instance-refresh \
        --auto-scaling-group-name payment-gateway-asg \
        --preferences '{"MinHealthyPercentage": 50, "InstanceWarmup": 300}' \
        --region $AWS_REGION
    echo "Instance refresh started. Check AWS Console for status."
fi

