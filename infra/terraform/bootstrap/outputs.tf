output "state_bucket" {
  description = "S3 bucket holding the main stack's Terraform state. Use in backend config."
  value       = aws_s3_bucket.tf_state.id
}

output "lock_table" {
  description = "DynamoDB table for state locking. Use in backend config."
  value       = aws_dynamodb_table.tf_lock.name
}

output "region" {
  description = "AWS region. Set as the AWS_REGION GitHub Actions variable."
  value       = var.aws_region
}

output "aws_access_key_id" {
  description = "Set this as the AWS_ACCESS_KEY_ID GitHub Actions secret."
  value       = aws_iam_access_key.ci.id
}

output "aws_secret_access_key" {
  description = "Set this as the AWS_SECRET_ACCESS_KEY GitHub Actions secret. Read with: tofu output -raw aws_secret_access_key"
  value       = aws_iam_access_key.ci.secret
  sensitive   = true
}
