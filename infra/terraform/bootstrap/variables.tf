variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefix used to name resources (bucket, lock table, roles)."
  type        = string
  default     = "spring-mcp-server"
}

variable "github_repo" {
  description = "GitHub repository allowed to assume the CI role, as 'owner/repo'."
  type        = string
  # Example: "srikanth-gude/spring-mcp-server"
}
