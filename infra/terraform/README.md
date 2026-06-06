# Main infrastructure stack

Provisions everything the running MCP server needs, all on the **AWS Free Tier**:

| Resource            | Purpose                                              | File          |
| ------------------- | ---------------------------------------------------- | ------------- |
| ECR repository      | Stores the Docker image (scan-on-push)               | `ecr.tf`      |
| DynamoDB table      | Backs the Task tools (`mcp-tasks`, on-demand)        | `dynamodb.tf` |
| IAM role + profile  | EC2 → SSM + ECR pull + scoped DynamoDB access        | `iam.tf`      |
| EC2 `t3.micro`      | Runs the container; managed via SSM (no SSH)         | `ec2.tf`      |
| Security group      | Opens only the HTTP port (no port 22)                | `ec2.tf`      |
| Elastic IP          | Stable public address for the MCP endpoint           | `ec2.tf`      |

State is stored remotely in the S3 bucket / lock table created by
[`bootstrap/`](bootstrap/README.md).

> **Note:** this stack is normally applied by the **`infra.yml`** GitHub Actions
> workflow, not by hand. The commands below are for local inspection only.

## Apply locally (optional)

```bash
cd infra/terraform

terraform init \
  -backend-config="bucket=<TF_STATE_BUCKET>" \
  -backend-config="dynamodb_table=<TF_LOCK_TABLE>" \
  -backend-config="region=us-east-1"

terraform plan
terraform apply
```

Then read the endpoint:

```bash
terraform output mcp_endpoint   # http://<elastic-ip>/mcp
```

## How the app gets deployed

This stack only provisions infra and installs Docker on the instance. It does
**not** run the container — that's the **`app-deploy.yml`** workflow, which
builds/scans/pushes the image and then runs it on the instance via an SSM
command. See [`docs/CICD.md`](../../docs/CICD.md).

## Free-tier notes & teardown

See [`docs/COST-AND-CLEANUP.md`](../../docs/COST-AND-CLEANUP.md). To remove
everything: `terraform destroy` here, then in `bootstrap/`.
