variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefix used to name resources (bucket, lock table, CI user)."
  type        = string
  default     = "spring-mcp-server"
}
