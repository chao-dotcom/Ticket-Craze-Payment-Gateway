# Security Group for RDS
resource "aws_security_group" "rds" {
  name        = "payment-gateway-rds-sg"
  description = "Security group for RDS"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }
}

# RDS Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "payment-gateway-db-subnet"
  subnet_ids = aws_subnet.private[*].id
  
  tags = {
    Name = "payment-gateway-db-subnet"
  }
}

# RDS Instance
resource "aws_db_instance" "postgres" {
  identifier           = "payment-gateway-db"
  engine               = "postgres"
  engine_version       = "15.5"
  instance_class       = var.db_instance_class
  allocated_storage    = 20
  storage_type         = "gp3"
  storage_encrypted    = true
  
  db_name  = "paymentgateway"
  username = "pgadmin"
  password = var.db_password
  
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"
  skip_final_snapshot    = true
  
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  
  performance_insights_enabled = false
  
  tags = {
    Name = "payment-gateway-postgres"
  }
}

