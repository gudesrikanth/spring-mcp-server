# Task storage. PAY_PER_REQUEST (on-demand) avoids provisioned capacity and
# stays comfortably within the Free Tier for a demo workload.
resource "aws_dynamodb_table" "tasks" {
  name         = var.dynamodb_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }
}
