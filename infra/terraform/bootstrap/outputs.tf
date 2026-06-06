output "state_bucket" {
  description = "S3 bucket holding the main stack's Terraform state. Use in backend config."
  value       = aws_s3_bucket.tf_state.id
}

output "lock_table" {
  description = "DynamoDB table for state locking. Use in backend config."
  value       = aws_dynamodb_table.tf_lock.name
}

output "github_actions_role_arn" {
  description = "Set this as the AWS_ROLE_ARN GitHub Actions variable/secret."
  value       = aws_iam_role.github_actions.arn
}

output "region" {
  description = "AWS region. Set as the AWS_REGION GitHub Actions variable."
  value       = var.aws_region
}
