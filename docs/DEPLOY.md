# Deploying to AWS (EC2 + RDS, free tier)

This runbook deploys the API as a Docker container on a single EC2 instance that
connects to a managed RDS PostgreSQL database. Everything here fits inside the AWS
free tier. **Tear it down when you're done** (last section) to avoid charges.

> Architecture: `Internet ──▶ EC2 (t3.micro, Docker, port 8080) ──▶ RDS PostgreSQL (db.t3.micro, port 5432)`
> The RDS instance is **not** publicly reachable — only the EC2 security group can talk to it.

---

## 1. Create the RDS PostgreSQL instance

Console → RDS → **Create database**:

- Engine: **PostgreSQL 16**, template **Free tier**
- Instance class: **db.t3.micro**, storage 20 GB gp3
- DB instance identifier: `buildlog-db`
- Master username: `app`, and set a strong master password
- **Additional configuration → Initial database name: `buildlog`** (so the app's
  `buildlog` database exists on first boot)
- Public access: **No**
- Create a new VPC security group named `buildlog-rds-sg`

When it's available, note the **endpoint** (e.g. `buildlog-db.abc123.eu-west-1.rds.amazonaws.com`).

## 2. Launch the EC2 instance

Console → EC2 → **Launch instance**:

- AMI: **Amazon Linux 2023**, type **t3.micro**
- Key pair: select or create one (you'll SSH with it)
- Security group `buildlog-ec2-sg`, inbound rules:
  - `22` (SSH) from **your IP only**
  - `8080` (HTTP) from `0.0.0.0/0` — fine for a demo; lock it down for anything real

## 3. Lock the RDS security group to EC2

Edit `buildlog-rds-sg` inbound rules: allow **PostgreSQL (5432)** with **source =
`buildlog-ec2-sg`** (the security group, not an IP). Remove any other inbound rule.
Now only the EC2 instance can reach the database.

## 4. Install Docker on EC2

SSH in (`ssh -i <key.pem> ec2-user@<ec2-public-ip>`), then:

```bash
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
exit   # log out and back in so the docker group applies
```

## 5. Build and run the container

```bash
git clone <your-repo-url> car-build-log
cd car-build-log
docker build -t car-build-log .

docker run -d --name car-build-log --restart unless-stopped -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:postgresql://<rds-endpoint>:5432/buildlog" \
  -e DB_USER="app" \
  -e DB_PASSWORD="<your-rds-password>" \
  car-build-log
```

Hibernate creates the schema on first start (`ddl-auto: update`).

## 6. Verify

```bash
curl http://<ec2-public-ip>:8080/actuator/health      # {"status":"UP"}
```

Then exercise it end-to-end:

```bash
curl -i -X POST http://<ec2-public-ip>:8080/api/v1/vehicles \
  -H 'Content-Type: application/json' \
  -d '{"make":"Volkswagen","model":"Golf GTI","year":2016,"engineCode":"EA888"}'
```

📸 Screenshot the `{"status":"UP"}` response and the `201 Created` for your records.

Useful while debugging: `docker logs -f car-build-log`.

## 7. Set a billing alarm ($5)

**Console:** Billing and Cost Management → Budgets → create a $5 monthly cost budget
with an email alert. Simplest option.

**CLI** (CloudWatch billing metrics live in `us-east-1`; requires an SNS topic
subscribed to your email, and "Receive Billing Alerts" enabled in Billing preferences):

```bash
aws sns create-topic --name buildlog-billing --region us-east-1
aws sns subscribe --topic-arn <topic-arn> --protocol email \
  --notification-endpoint you@example.com --region us-east-1   # confirm via email

aws cloudwatch put-metric-alarm \
  --alarm-name buildlog-billing-5usd \
  --namespace AWS/Billing --metric-name EstimatedCharges \
  --dimensions Name=Currency,Value=USD \
  --statistic Maximum --period 21600 --evaluation-periods 1 \
  --threshold 5 --comparison-operator GreaterThanOrEqualToThreshold \
  --alarm-actions <topic-arn> --region us-east-1
```

## 8. Tear down (do this after screenshotting)

- **EC2** → terminate the `buildlog` instance
- **RDS** → delete `buildlog-db` (you can skip the final snapshot for a throwaway)
- Delete security groups `buildlog-ec2-sg` and `buildlog-rds-sg`
- Delete the CloudWatch alarm, the SNS topic, and the budget
- Confirm the EBS volume was removed with the instance

---

### Option B (lower-touch alternative)

If you'd rather not manage EC2: push the image to **ECR** and run it on **AWS App Runner**
(or Elastic Beanstalk) pointed at the same RDS instance, with the same `DB_URL` /
`DB_USER` / `DB_PASSWORD` / `SPRING_PROFILES_ACTIVE=prod` environment variables. More
managed, less to configure, but EC2 is the more transferable thing to talk through in
an interview.
