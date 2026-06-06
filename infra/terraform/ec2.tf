# The compute that runs the MCP server container.

# Default VPC + a default subnet — no custom networking needed for a demo.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Latest Amazon Linux 2023 AMI (resolved from the public SSM parameter).
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# Security group: expose only the MCP HTTP port. No SSH — access is via SSM.
resource "aws_security_group" "app" {
  name        = "${var.project_name}-sg"
  description = "MCP server HTTP access (no SSH; managed via SSM)"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "MCP HTTP endpoint"
    from_port   = var.http_port
    to_port     = var.http_port
    protocol    = "tcp"
    cidr_blocks = [var.allowed_cidr]
  }

  egress {
    description = "All outbound (ECR pulls, package installs, DynamoDB)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Installs Docker on first boot; the SSM agent is preinstalled on AL2023.
# The actual container is started later by the app-deploy workflow over SSM.
locals {
  user_data = <<-EOF
    #!/bin/bash
    set -euxo pipefail
    dnf update -y
    dnf install -y docker
    systemctl enable --now docker
    systemctl enable --now amazon-ssm-agent
  EOF
}

resource "aws_instance" "app" {
  ami                         = data.aws_ssm_parameter.al2023.value
  instance_type               = var.instance_type
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.app.id]
  iam_instance_profile        = aws_iam_instance_profile.ec2.name
  associate_public_ip_address = true
  user_data                   = local.user_data

  metadata_options {
    http_tokens = "required" # IMDSv2 only
  }

  root_block_device {
    volume_size = 8 # GiB — within the 30 GiB Free Tier EBS allowance
    volume_type = "gp3"
    encrypted   = true
  }

  tags = {
    Name = var.project_name
  }
}

# Stable public IP so the MCP endpoint URL doesn't change across reboots.
resource "aws_eip" "app" {
  instance = aws_instance.app.id
  domain   = "vpc"
}
