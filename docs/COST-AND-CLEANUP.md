# Cost & cleanup

This project is designed to sit inside the **AWS Free Tier**, but "free" has
conditions — read this before leaving it running.

## What's free (and the catches)

| Service | Free tier | Catch |
| --- | --- | --- |
| EC2 `t3.micro` | 750 hrs/month for **12 months** | One instance ≈ always-on. A *second* instance, or staying past 12 months, costs money. |
| EBS gp3 | 30 GiB for 12 months | We use 8 GiB. |
| Elastic IP | Free **while attached** to a running instance | An **unattached** EIP (e.g. after stopping the instance) is billed hourly. |
| ECR | 500 MB private storage for 12 months | Lifecycle policy keeps only the last 10 images. |
| DynamoDB | 25 GB + generous on-demand | On-demand pricing applies beyond the free allowance. |
| Data transfer | Some free egress | Heavy outbound traffic can cost. |
| SSM, IAM, S3 state | Effectively free here | — |

> The single biggest "surprise bill" risk is an **unattached Elastic IP** or a
> **stopped-but-not-terminated** instance. If you stop the instance, either
> release the EIP or terminate everything.

## Keep costs at zero when idle

- Easiest: **destroy** when you're not demoing (a fresh deploy takes minutes).
- Or restrict exposure (`allowed_cidr`) so it isn't taking internet traffic.

## Full teardown

```bash
# 1. Main stack (EC2, EIP, ECR, DynamoDB, IAM, SG)
cd infra/terraform
terraform destroy \
  -backend-config="bucket=<TF_STATE_BUCKET>" \
  -backend-config="dynamodb_table=<TF_LOCK_TABLE>" \
  -backend-config="region=<REGION>"
# (or run the Infra workflow with action = destroy)

# 2. Bootstrap (state bucket, lock table, CI IAM user) — last
cd bootstrap
terraform destroy
```

Then double-check the console for: a leftover **Elastic IP**, the **ECR images**,
and the **DynamoDB table**. The `terraform destroy` covers all of these, but a
30-second glance avoids a slow-burn bill.

## Local-only has no AWS cost

`docker compose up` and `./mvnw verify` run entirely on your machine — no AWS
resources, no charges.
