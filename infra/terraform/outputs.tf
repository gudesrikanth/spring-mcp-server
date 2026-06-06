output "ecr_repository_url" {
  description = "Push target for the app image."
  value       = aws_ecr_repository.app.repository_url
}

output "instance_id" {
  description = "EC2 instance id — the SSM deploy target."
  value       = aws_instance.app.id
}

output "public_ip" {
  description = "Elastic IP of the server."
  value       = aws_eip.app.public_ip
}

output "mcp_endpoint" {
  description = "Public MCP Streamable HTTP endpoint clients connect to."
  value       = "http://${aws_eip.app.public_ip}/mcp"
}

output "health_endpoint" {
  description = "Actuator health URL used by the deploy smoke test."
  value       = "http://${aws_eip.app.public_ip}/actuator/health"
}

output "dynamodb_table" {
  description = "Task storage table name."
  value       = aws_dynamodb_table.tasks.name
}
