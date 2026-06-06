# Infrastructure (AWS Free Tier)

All infrastructure is Terraform, split into two stacks:

- **`infra/terraform/bootstrap/`** — run once, locally. Creates the remote state
  backend and the GitHub OIDC role. See its
  [README](../infra/terraform/bootstrap/README.md).
- **`infra/terraform/`** — the main stack, normally applied by the `infra.yml`
  workflow. See its [README](../infra/terraform/README.md).

## Resources and why each exists

| Resource | Why | Free-tier angle |
| --- | --- | --- |
| **ECR repository** | Stores the Docker image; scan-on-push | 500 MB private storage free for 12 months; lifecycle keeps last 10 images |
| **DynamoDB `mcp-tasks`** | Persists tasks; on-demand billing | 25 GB + plenty of on-demand requests free |
| **EC2 `t3.micro`** | Runs the container 24/7 | 750 hrs/month free for 12 months (one instance) |
| **EBS gp3 8 GiB** | Root volume | Within 30 GiB free allowance |
| **Elastic IP** | Stable public address | Free **while attached** to a running instance |
| **IAM role + instance profile** | EC2 → SSM, ECR pull, scoped DynamoDB | Free |
| **Security group** | Opens only port 80 (no SSH) | Free |
| **S3 + DynamoDB (bootstrap)** | Terraform state + lock | Negligible |

## Security choices

- **No SSH.** There's no port 22 and no key pair. Administration and deployment
  happen through **AWS Systems Manager (SSM)** using the instance role — fewer
  moving parts and no inbound shell exposure.
- **OIDC, no static keys.** GitHub Actions assumes an IAM role via OpenID
  Connect; there are no `AWS_ACCESS_KEY_ID` secrets to leak.
- **IMDSv2 required**, EBS encrypted, scoped DynamoDB policy (only the one
  table).
- **Known trade-offs (demo):** plain HTTP, endpoint open to `0.0.0.0/0`, no MCP
  auth. Tighten `allowed_cidr`, add TLS, and add authorization for real use.

## Networking

Uses the account's **default VPC** and a default subnet to keep the example
small. The instance gets a public IP + Elastic IP; clients reach
`http://<eip>/mcp` where host port 80 maps to the container's 8080.

## Inputs / outputs

Configurable variables (see `variables.tf`): `aws_region`, `project_name`,
`instance_type`, `dynamodb_table_name`, `allowed_cidr`, ports.

Outputs (see `outputs.tf`): `ecr_repository_url`, `instance_id`, `public_ip`,
`mcp_endpoint`, `health_endpoint`, `dynamodb_table`.
