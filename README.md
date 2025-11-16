# 💳 Payment Gateway - Cloud-Native Payment Processing System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15.5-blue.svg)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-red.svg)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![Terraform](https://img.shields.io/badge/Terraform-1.6+-623CE4.svg)](https://www.terraform.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/org.springframework.boot/spring-boot-starter-parent.svg)](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-parent)
[![GitHub Actions](https://img.shields.io/github/actions/workflow/status/yourusername/payment-gateway/ci-cd.yml?branch=main)](https://github.com/yourusername/payment-gateway/actions)
[![Code Coverage](https://img.shields.io/badge/coverage-80%25-brightgreen)](https://github.com/yourusername/payment-gateway)

A production-grade, cloud-native payment processing system built with **Java Spring Boot**, deployed on **AWS** with full **CI/CD automation**. This project demonstrates enterprise-level backend development, RESTful API design, database optimization, cloud infrastructure, and DevOps practices.

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Project Structure](#-project-structure)
- [Configuration](#-configuration)
- [Security](#-security)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🚀 Features

### Core Functionality
- ✅ **Transaction Management** - Create, retrieve, and list transactions with full CRUD operations
- ✅ **Idempotency** - Prevent duplicate transactions using idempotency keys
- ✅ **State Machine** - Immutable state transitions (PENDING → PROCESSING → COMPLETED/FAILED) with audit trail
- ✅ **Refund Processing** - Full and partial refund support with status tracking
- ✅ **Webhook Notifications** - Asynchronous webhook delivery with retry logic and HMAC signature verification

### Security & Reliability
- 🔐 **API Key Authentication** - SHA-256 hashed API keys with merchant-based access control
- 🛡️ **Rate Limiting** - Per-merchant rate limiting (120 req/min) using Bucket4j token bucket algorithm
- ✅ **Input Validation** - Comprehensive validation using Hibernate Validator
- 🔄 **Retry Logic** - Exponential backoff retry for external service calls
- 🔒 **Optimistic Locking** - Prevent concurrent update conflicts using `@Version`

### Infrastructure & DevOps
- ☁️ **AWS Deployment** - Complete infrastructure on AWS (EC2, RDS, ALB, CloudWatch)
- 🏗️ **Infrastructure as Code** - Terraform for reproducible deployments
- 🐳 **Docker Support** - Containerized application with multi-stage builds
- 🔄 **CI/CD Pipeline** - Automated testing and deployment with GitHub Actions
- 📊 **Monitoring** - CloudWatch dashboards, alarms, and log aggregation

---

## 🛠️ Tech Stack

### Backend
- **Java 17** - Modern Java with records, pattern matching, and sealed classes
- **Spring Boot 3.2.1** - Enterprise framework with auto-configuration
- **Spring Data JPA** - Database abstraction layer
- **Spring Security** - Authentication and authorization
- **Spring Retry** - Retry mechanism for resilient operations

### Database
- **PostgreSQL 15** - Relational database with ACID compliance
- **Flyway** - Database migration and version control
- **HikariCP** - High-performance connection pooling

### Infrastructure
- **AWS EC2** - Compute instances with Auto Scaling
- **AWS RDS** - Managed PostgreSQL database
- **AWS ALB** - Application Load Balancer
- **AWS CloudWatch** - Monitoring and logging
- **AWS ECR** - Container registry
- **Terraform** - Infrastructure as Code

### DevOps & Tools
- **Docker** - Containerization
- **GitHub Actions** - CI/CD automation
- **JMeter** - Load testing
- **TestContainers** - Integration testing
- **JaCoCo** - Code coverage

---

## 🏗️ Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTPS
       ▼
┌─────────────────┐
│  ALB (AWS)      │  ← Load Balancing
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  EC2 Instances  │  ← Auto Scaling Group
│  (Spring Boot)  │
└──────┬──────────┘
       │
       ├──► RDS PostgreSQL  ← Database
       │
       └──► CloudWatch      ← Monitoring
```

### Key Components

1. **API Layer** - RESTful endpoints with OpenAPI/Swagger documentation
2. **Service Layer** - Business logic with transaction management
3. **Repository Layer** - Data access with Spring Data JPA
4. **Security Layer** - API key authentication and rate limiting
5. **Infrastructure Layer** - AWS services and Terraform configuration

---

## 🏃 Quick Start

### Prerequisites

- **Java 17+** - [Download](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/get-started)
- **PostgreSQL 15** (optional, Docker Compose includes it)

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/yourusername/payment-gateway.git
cd payment-gateway

# Start all services
docker-compose up -d

# Check application health
curl http://localhost:8080/actuator/health

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

### Option 2: Local Development

```bash
# Start PostgreSQL
docker run -d \
  --name payment-gateway-db \
  -e POSTGRES_DB=paymentgateway \
  -e POSTGRES_USER=pgadmin \
  -e POSTGRES_PASSWORD=localdevpassword \
  -p 5432:5432 \
  postgres:15.5-alpine

# Run the application
mvn spring-boot:run
```

### Create Test Merchant

Before testing the API, create a test merchant:

```bash
# Generate API key hash
echo -n "test-api-key-12345" | sha256sum

# Connect to database
docker exec -it payment-gateway-db psql -U pgadmin -d paymentgateway

# Insert merchant (replace <hash> with generated hash)
INSERT INTO merchants (merchant_code, business_name, email, api_key_hash, status, created_at, updated_at)
VALUES (
    'TEST_MERCHANT_001',
    'Test Business LLC',
    'test@example.com',
    '<hash>',  -- Replace with your generated hash
    'ACTIVE',
    NOW(),
    NOW()
);
```

See [scripts/create-test-merchant.sql](scripts/create-test-merchant.sql) for a complete script.

---

## 📚 API Documentation

### Interactive Documentation

Once the application is running, access:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Authentication

All API endpoints require API key authentication:

```http
X-API-Key: your-api-key-here
```

### Example API Calls

#### Create Transaction

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "X-API-Key: test-api-key-12345" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "amount": 150.50,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "description": "Order #12345",
    "customerEmail": "customer@example.com",
    "customerName": "John Doe"
  }'
```

**Response (201 Created):**
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 150.50,
  "currency": "USD",
  "status": "PENDING",
  "paymentMethod": "CREDIT_CARD",
  "description": "Order #12345",
  "customerEmail": "customer@example.com",
  "createdAt": "2025-01-16T10:30:00Z"
}
```

#### Get Transaction

```bash
curl -X GET http://localhost:8080/api/v1/transactions/{transactionId} \
  -H "X-API-Key: test-api-key-12345"
```

#### List Transactions

```bash
curl -X GET "http://localhost:8080/api/v1/transactions?status=COMPLETED&page=0&size=20" \
  -H "X-API-Key: test-api-key-12345"
```

#### Create Refund

```bash
curl -X POST http://localhost:8080/api/v1/refunds \
  -H "Content-Type: application/json" \
  -H "X-API-Key: test-api-key-12345" \
  -d '{
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 50.00,
    "reason": "Customer request"
  }'
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transactions` | Create transaction |
| GET | `/api/v1/transactions/{id}` | Get transaction by ID |
| GET | `/api/v1/transactions` | List transactions (paginated) |
| GET | `/api/v1/transactions/{id}/history` | Get transaction audit trail |
| POST | `/api/v1/refunds` | Create refund |
| GET | `/api/v1/refunds/{id}` | Get refund by ID |
| GET | `/api/v1/reports/daily-summary` | Get daily transaction summary |
| GET | `/api/v1/reports/revenue` | Get revenue report |
| GET | `/api/v1/merchants/me` | Get current merchant info |
| GET | `/api/v1/health` | Health check |

---

## 🧪 Testing

### Run Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
# View report: target/site/jacoco/index.html

# Run integration tests
mvn verify
```

### Automated Testing Script

```bash
# Make executable
chmod +x scripts/test-api.sh

# Run API tests
export API_KEY="test-api-key-12345"
bash scripts/test-api.sh
```

### Load Testing

```bash
# Using JMeter
jmeter -n -t load-test.jmx -l results.jtl -e -o report/

# View HTML report
open report/index.html
```

### Demo Script

```bash
# Run complete demo
chmod +x demo.sh
./demo.sh
```

---

## 🚀 Deployment

### AWS Deployment with Terraform

```bash
# Navigate to terraform directory
cd terraform

# Initialize Terraform
terraform init

# Review changes
terraform plan

# Apply infrastructure
terraform apply

# Note: Set db_password via environment variable or terraform.tfvars
export TF_VAR_db_password="your-secure-password"
terraform apply
```

### CI/CD Pipeline

The project includes GitHub Actions workflows for:
- **Automated Testing** - Runs on every push and PR
- **Docker Build** - Builds and pushes to ECR
- **Auto Deployment** - Deploys to AWS on merge to main

**Required GitHub Secrets:**
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `DB_PASSWORD`

### Manual Deployment

```bash
# Build Docker image
docker build -t payment-gateway:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-db-host \
  -e DB_USERNAME=pgadmin \
  -e DB_PASSWORD=your-password \
  payment-gateway:latest
```

---

## 📁 Project Structure

```
payment-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/yourname/paymentgateway/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/              # Data transfer objects
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── enums/            # Enumerations
│   │   │   ├── exception/        # Exception handlers
│   │   │   ├── repository/       # Data access layer
│   │   │   ├── security/         # Security configuration
│   │   │   ├── service/          # Business logic
│   │   │   └── util/             # Utility classes
│   │   └── resources/
│   │       ├── application.yml   # Base configuration
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/     # Flyway migrations
│   └── test/                     # Test classes
├── terraform/                    # Infrastructure as Code
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── vpc.tf
│   ├── rds.tf
│   ├── ec2.tf
│   ├── alb.tf
│   └── cloudwatch.tf
├── scripts/                      # Utility scripts
│   ├── setup-local-db.sh
│   ├── create-test-merchant.sql
│   ├── test-api.sh
│   └── deploy.sh
├── .github/workflows/            # CI/CD pipelines
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `paymentgateway` |
| `DB_USERNAME` | Database username | `pgadmin` |
| `DB_PASSWORD` | Database password | *required* |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |
| `SERVER_PORT` | Application port | `8080` |

### Application Profiles

- **dev** - Development profile with debug logging
- **prod** - Production profile with optimized settings

### Key Configuration Files

- `application.yml` - Base configuration
- `application-dev.yml` - Development overrides
- `application-prod.yml` - Production overrides

---

## 🔒 Security

### Security Features

- ✅ **API Key Authentication** - SHA-256 hashed keys
- ✅ **Rate Limiting** - 120 requests/minute per merchant
- ✅ **Input Validation** - Hibernate Validator
- ✅ **SQL Injection Protection** - JPA/Hibernate parameterized queries
- ✅ **XSS Protection** - Spring Security defaults
- ✅ **HTTPS Ready** - Configure SSL/TLS on ALB

### Security Best Practices

- Never commit secrets to version control
- Use environment variables for sensitive data
- Rotate API keys regularly
- Enable CloudWatch monitoring
- Review security groups and IAM policies

See [SECURITY.md](SECURITY.md) for detailed security guidelines.

---

## 📊 Monitoring

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

### CloudWatch

- **Dashboard**: Transaction volume, database metrics, error logs
- **Alarms**: High error rate, database connections
- **Logs**: Application logs aggregated in CloudWatch Logs

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Java coding conventions
- Write unit tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🎓 Learning Outcomes

This project demonstrates:

- ✅ **Enterprise Spring Boot** - Production-ready backend development
- ✅ **RESTful API Design** - Proper HTTP methods, status codes, and error handling
- ✅ **Database Design** - Normalization, indexing, migrations, and ACID compliance
- ✅ **Cloud Architecture** - AWS services, infrastructure as code, and scalability
- ✅ **DevOps Practices** - CI/CD, containerization, and automation
- ✅ **Security** - Authentication, rate limiting, and input validation
- ✅ **Testing** - Unit, integration, and load testing strategies

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [AWS Documentation](https://docs.aws.amazon.com/)
- [Terraform Documentation](https://www.terraform.io/docs)
- [Docker Documentation](https://docs.docker.com/)

---

## 💬 Support

For issues and questions:
- Open an issue on [GitHub Issues](https://github.com/yourusername/payment-gateway/issues)
- Check the [Wiki](https://github.com/yourusername/payment-gateway/wiki) for common questions

---

## ⭐ Show Your Support

If you find this project helpful, please give it a star! ⭐

---

**Built with ❤️ using Spring Boot and AWS**
