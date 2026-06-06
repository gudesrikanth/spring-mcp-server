###############################################################################
# Bootstrap stack — run ONCE, locally, before anything else.
#
# It creates the things the main stack and CI cannot create for themselves:
#   1. An S3 bucket + DynamoDB table for remote Terraform state & locking.
#   2. A dedicated IAM user + access key that the GitHub Actions workflows use
#      to authenticate to AWS (classic access key / secret credentials).
#
# This stack uses LOCAL state (a terraform.tfstate file committed nowhere) —
# that's fine because it is tiny and rarely changes. See README.md.
#
# NOTE: this uses long-lived access keys for simplicity. They are convenient for
# a learning project but are static secrets — rotate or delete them when done.
###############################################################################

terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform-bootstrap"
    }
  }
}

data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------
# 1. Remote state backend: versioned, encrypted S3 bucket + lock table.
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "tf_state" {
  bucket = "${var.project_name}-tfstate-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_versioning" "tf_state" {
  bucket = aws_s3_bucket.tf_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "tf_state" {
  bucket = aws_s3_bucket.tf_state.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "tf_state" {
  bucket                  = aws_s3_bucket.tf_state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "tf_lock" {
  name         = "${var.project_name}-tflock"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"
  attribute {
    name = "LockID"
    type = "S"
  }
}

# ---------------------------------------------------------------------------
# 2. IAM user + access key used by the GitHub Actions workflows.
#    The deploy permissions are scoped to this project's resources where
#    practical; this is a learning project, so it favours clarity.
# ---------------------------------------------------------------------------
resource "aws_iam_user" "ci" {
  name = "${var.project_name}-ci"
}

resource "aws_iam_access_key" "ci" {
  user = aws_iam_user.ci.name
}

data "aws_iam_policy_document" "deploy" {
  statement {
    sid    = "TerraformState"
    effect = "Allow"
    actions = [
      "s3:GetObject", "s3:PutObject", "s3:ListBucket", "s3:DeleteObject"
    ]
    resources = [
      aws_s3_bucket.tf_state.arn,
      "${aws_s3_bucket.tf_state.arn}/*"
    ]
  }
  statement {
    sid       = "TerraformLock"
    effect    = "Allow"
    actions   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"]
    resources = [aws_dynamodb_table.tf_lock.arn]
  }
  statement {
    sid    = "ManageInfra"
    effect = "Allow"
    actions = [
      "ec2:*",
      "ecr:*",
      "dynamodb:*",
      "iam:*",
      "ssm:*",
      "sts:GetCallerIdentity",
      "logs:*"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_user_policy" "deploy" {
  name   = "${var.project_name}-deploy"
  user   = aws_iam_user.ci.name
  policy = data.aws_iam_policy_document.deploy.json
}
