terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
  }

  # Remote state in S3 with DynamoDB locking. Values are supplied at init time
  # via `-backend-config` (see infra.yml / README) so this file stays generic.
  backend "s3" {
    key     = "main/terraform.tfstate"
    encrypt = true
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "terraform"
    }
  }
}
