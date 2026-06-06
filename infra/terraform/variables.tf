variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefix used to name resources."
  type        = string
  default     = "spring-mcp-server"
}

variable "instance_type" {
  description = "EC2 instance type. t3.micro is AWS Free Tier eligible (750 hrs/mo for 12 months)."
  type        = string
  default     = "t3.micro"
}

variable "dynamodb_table_name" {
  description = "Name of the DynamoDB table backing the Task tools."
  type        = string
  default     = "mcp-tasks"
}

variable "app_port" {
  description = "Container port the Spring app listens on."
  type        = number
  default     = 8080
}

variable "http_port" {
  description = "Host/public port exposed by the security group (mapped to app_port)."
  type        = number
  default     = 80
}

variable "allowed_cidr" {
  description = "CIDR allowed to reach the MCP HTTP endpoint. Default is open; tighten for real use."
  type        = string
  default     = "0.0.0.0/0"
}
