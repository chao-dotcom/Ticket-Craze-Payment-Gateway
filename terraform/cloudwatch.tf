# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "app" {
  name              = "/aws/payment-gateway"
  retention_in_days = 7
  
  tags = {
    Name = "payment-gateway-logs"
  }
}

# CloudWatch Dashboard
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "payment-gateway-dashboard"
  
  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric"
        properties = {
          metrics = [
            ["PaymentGateway", "transaction.created", {stat = "Sum"}],
            [".", "transaction.completed", {stat = "Sum"}],
            [".", "transaction.failed", {stat = "Sum"}]
          ]
          period = 300
          stat   = "Sum"
          region = var.aws_region
          title  = "Transaction Volume"
        }
      },
      {
        type = "metric"
        properties = {
          metrics = [
            ["AWS/RDS", "CPUUtilization", {stat = "Average"}],
            [".", "DatabaseConnections", {stat = "Sum"}]
          ]
          period = 300
          stat   = "Average"
          region = var.aws_region
          title  = "Database Metrics"
        }
      },
      {
        type = "log"
        properties = {
          query = "fields @timestamp, @message | filter @message like /ERROR/ | sort @timestamp desc | limit 20"
          region = var.aws_region
          title = "Recent Errors"
        }
      }
    ]
  })
}

# CloudWatch Alarms
resource "aws_cloudwatch_metric_alarm" "high_error_rate" {
  alarm_name          = "payment-gateway-high-error-rate"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "transaction.failed"
  namespace           = "PaymentGateway"
  period              = "300"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "This metric monitors transaction failure rate"
  
  alarm_actions = [aws_sns_topic.alerts.arn]
  
  tags = {
    Name = "payment-gateway-error-alarm"
  }
}

resource "aws_sns_topic" "alerts" {
  name = "payment-gateway-alerts"
  
  tags = {
    Name = "payment-gateway-alerts"
  }
}

