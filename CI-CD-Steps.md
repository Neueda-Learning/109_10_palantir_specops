# CI/CD Steps — End-to-End Deployment Guide (EC2 + Jenkins)

> Shared guide for all Palantir Learning apps. Worked example uses **SpecOps** (`109_10_palantir_specops`); adjust names/ports/DB for other apps (see §0 matrix).
>
> Pipeline: **Git push → GitHub Actions (CI) → GHCR (images) → curl (ngrok) → Jenkins (CD) → docker compose (EC2)**

---

## 0) Per-App Matrix (from conventions)

| App | API image | UI image | API job | UI job | Database | Branch |
|-----|-----------|----------|---------|--------|----------|--------|
| SpecOps | `ghcr.io/neueda-learning/specops-api` | `ghcr.io/neueda-learning/specops-ui` | `specops-api-deploy-job` | `specops-ui-deploy-job` | `transaction_monitoring` | `main` |
| FlowOps | `.../flowops-api` | `.../flowops-ui` | `flowops-api-deploy-job` | `flowops-ui-deploy-job` | `salary_payment_db` | `master` |
| Pheonix | `.../pheonix-api` | `.../pheonix-ui` | `pheonix-api-deploy-job` | `pheonix-ui-deploy-job` | `portfolio_db` | `main` |
| POMXML | `.../pomxml-api` | `.../pomxml-ui` | `pomxml-api-deploy-job` | `pomxml-ui-deploy-job` | `portfolio_db` | `main` (+ `frontend`) |
| Beyond404 | `.../beyond404-api` | `.../beyond404-ui` | `beyond404-api-deploy-job` | `beyond404-ui-deploy-job` | `beyond404` | `main` |
| DevSquad | `.../devsquad-api` | `.../devsquad-ui` | `devsquad-api-deploy-job` | `devsquad-ui-deploy-job` | `payment_processing` | `main` |
| BND | `.../bnd-api` | `.../bnd-ui` | `bnd-api-deploy-job` | `bnd-ui-deploy-job` | `payment_processing` | `main` |
| Check-em | `.../check-em-api` | `.../check-em-ui` | `check-em-api-deploy-job` | `check-em-ui-deploy-job` | `payflow` | `main` |

Common settings everywhere:
- Ports: **UI 8081:80** · **API 8082:8080** · **market-data 8000:8000** · **MySQL 3306:3306** (`root` / `n3u3da!`)
- Backend Dockerfile: `eclipse-temurin:<17|25>-jre`, `COPY target/*.jar app.jar`, `EXPOSE 8080`
- Frontend Dockerfile: `node:22-alpine` build → `nginx:alpine` (optional `--build-arg VITE_*`)
- Jenkins URL: `${{ secrets.JENKINS_URL }}` (ngrok) + `${{ secrets.JENKINS_TOKEN }}` (build token)

---

## 1) What You Need Before Starting

| Item | Where from |
|------|-----------|
| GitHub repos (one per app, code pushed) | your org |
| GitHub PAT with `read:packages` scope | GitHub → Settings → Developer settings → Tokens |
| EC2 instance (Linux) — the Jenkins/deploy host | AWS console |
| ngrok authtoken | https://dashboard.ngrok.com |
| GitHub Actions secrets `JENKINS_URL` + `JENKINS_TOKEN` | per repo → Settings → Secrets |

> **Single EC2 host assumption:** one host runs Jenkins **and** the deployed containers. All commands below run there unless stated otherwise.

---

## 2) Repo Layout (make sure these files are committed per repo)

```
<app>-repo/
├── .github/
│   └── workflows/
│       ├── backend-ci.yml     # mvnw clean verify → push API image → trigger API job
│       └── frontend-ci.yml    # npm ci → npm run build → push UI image → trigger UI job
├── backend/
│   ├── Dockerfile             # NOTE: must be "Dockerfile", not "DockerFile"
│   └── .dockerignore
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf             # proxies /api → http://api:8080
│   └── .dockerignore
├── docker-compose.yml         # services: mysql / api / ui
└── CI-CD-Steps.md
```

Important gotchas:
- `backend/Dockerfile` **must be named exactly `Dockerfile`** (case-sensitive). CI's `docker build` fails otherwise.
- In `docker-compose.yml` the backend service **must be named `api`** because `nginx.conf` proxies to `http://api:8080`.
- Images referenced by compose must match the workflow tags: `ghcr.io/neueda-learning/<app>-api:latest` / `-ui:latest`.

---

## 3) Step 1 — Push the Code (do on your local machine)

```bash
git add -A
git commit -m "chore: add CI/CD pipeline"
git push origin main          # or "master" for FlowOps
```

GitHub Actions now runs automatically (see repo → Actions tab):

| Workflow | Triggers | What it does |
|----------|----------|--------------|
| `backend-ci.yml` | push/PR to `main` | MySQL 8.4 service → `./mvnw clean verify` → (push only) `docker build` + push `...-api:latest` → curl Jenkins API job |
| `frontend-ci.yml` | push/PR to `main` | Node 22 → `npm ci` → `npm run build` → (push only) push `...-ui:latest` → curl Jenkins UI job |

**The CI steps will fail until Steps 4–6 (secrets + Jenkins) are done** — that's expected. Do the server setup first, then push.

---

## 4) Step 2 — Provision the EC2 Server (Amazon Linux 2023)

> Ubuntu commands shown as `[apt]` alternatives.

### 4.1 Install Docker + compose plugin

```bash
# Amazon Linux 2023 (dnf)
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo systemctl status docker --no-pager

# Ubuntu (apt)
# sudo apt update && sudo apt install -y docker.io docker-compose-plugin git
# sudo systemctl enable --now docker

docker --version
docker compose version        # must print v2.x (not "docker-compose" v1)
```

If `docker compose` is missing on AL2023:
```bash
sudo dnf install -y docker-compose-plugin
```

### 4.2 Add your user to the docker group

```bash
sudo usermod -aG docker ec2-user
```

Log out and back in (or run `newgrp docker`) so the group takes effect. Verify:
```bash
docker ps                     # no "permission denied" error
```

### 4.3 Open ports in the EC2 Security Group

In the AWS console, on the instance's Security Group add inbound rules:

| Type | Port | Source |
|------|------|--------|
| Custom TCP | 8080 | 0.0.0.0/0 (Jenkins UI) |
| Custom TCP | 8081 | 0.0.0.0/0 (deployed UI) |
| Custom TCP | 8082 | 0.0.0.0/0 (deployed API / Swagger) |
| Custom TCP | 3306 | 0.0.0.0/0 (MySQL — or restrict to your IP) |

---

## 5) Step 3 — Install Jenkins on EC2

```bash
# Amazon Linux 2023
sudo dnf install -y java-17-amazon-corretto-devel
java -version

# Add the Jenkins stable repo
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io-2023.key

sudo dnf install -y jenkins
sudo systemctl enable --now jenkins
sudo systemctl status jenkins --no-pager

# Ubuntu (apt)
# sudo apt update && sudo apt install -y openjdk-17-jdk
# curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc >/dev/null
# echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" | sudo tee /etc/apt/sources.list.d/jenkins.list
# sudo apt update && sudo apt install -y jenkins
# sudo systemctl enable --now jenkins
```

Get the initial admin password:
```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Open `http://<YOUR_EC2_PUBLIC_IP>:8080` in a browser, paste the password, install the **suggested plugins**, create an admin user, and keep the URL as `http://localhost:8080`.

### Let the `jenkins` user run Docker

```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

> Without this, the deploy job fails with `Got permission denied while trying to connect to the Docker daemon socket`.

---

## 6) Step 4 — Give Jenkins GHCR access

Jenkins must be able to `docker compose pull` the images from GitHub Container Registry.

```bash
# Login as the jenkins user so its ~/.docker/config.json is used by the deploy jobs
echo '<YOUR_GH_PAT_WITH_read:packages>' | sudo -u jenkins docker login ghcr.io -u <YOUR_GITHUB_USERNAME> --password-stdin
```

Verify (as jenkins):
```bash
sudo -u jenkins docker pull ghcr.io/neueda-learning/specops-api:latest
```

- PAT scope needed: `read:packages` (nothing else).
- Do this per EC2 host once — it authenticates all `ghcr.io/neueda-learning/*` images.

---

## 7) Step 5 — Deploy Directory + docker-compose.yml on EC2

```bash
sudo mkdir -p /opt/specops
sudo chown -R jenkins:jenkins /opt/specops
```

Copy your repo's `docker-compose.yml` into it. Either `scp` it, or paste via `nano`:

```bash
sudo nano /opt/specops/docker-compose.yml
```

For SpecOps it should look like this (service named **`api`**, ports 8082/8081):

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: specops-mysql
    environment:
      MYSQL_ROOT_PASSWORD: n3u3da!
      MYSQL_DATABASE: transaction_monitoring
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-pn3u3da!"]
      interval: 10s
      timeout: 5s
      retries: 10

  api:
    image: ghcr.io/neueda-learning/specops-api:latest
    container_name: specops-api
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/transaction_monitoring?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: n3u3da!
    ports:
      - "8082:8080"

  ui:
    image: ghcr.io/neueda-learning/specops-ui:latest
    container_name: specops-ui
    depends_on:
      - api
    ports:
      - "8081:80"
    restart: unless-stopped

volumes:
  mysql_data:
```

> For other apps: change `container_name`, the DB name in `MYSQL_DATABASE`/JDBC URL, and the two image tags. Keep the service names `mysql` / `api` / `ui`.

Sanity check (as jenkins) — should pull and start everything:
```bash
sudo -u jenkins bash -c 'cd /opt/specops && docker compose pull && docker compose up -d && docker compose ps'
```

If that works, tear it down again until the pipeline is wired:
```bash
sudo -u jenkins bash -c 'cd /opt/specops && docker compose down'
```

---

## 8) Step 6 — Create the Jenkins Jobs

In Jenkins UI (`http://<IP>:8080`), for **each** app create **two** Freestyle jobs.

Job names (SpecOps example):
- `specops-api-deploy-job`
- `specops-ui-deploy-job`

### 8.1 Create `specops-api-deploy-job`

1. **Dashboard → New Item**
2. Name: `specops-api-deploy-job`, type: **Freestyle project** → OK
3. **Build Triggers** → tick **"Trigger builds remotely (e.g., from scripts)"** → Token: `deploy1234`
   > This enables the URL the GitHub workflow curls:
   > `https://<ngrok>.ngrok-free.app/buildByToken/build?job=specops-api-deploy-job&token=deploy1234`
4. **Build Steps → Add build step → Execute shell**:
   ```bash
   cd /opt/specops
   docker compose pull
   docker compose down
   docker compose up -d
   docker compose ps
   ```
5. Save.

### 8.2 Create `specops-ui-deploy-job`

Same steps, name `specops-ui-deploy-job`, same token, same build script. (Optionally use only the `ui` service.)

> One job per app per tier keeps deploys independent. Running both jobs against the same `/opt/specops` dir is safe — `docker compose` is idempotent.

### 8.3 Verify the token endpoint

```bash
curl "http://localhost:8080/buildByToken/build?job=specops-api-deploy-job&token=deploy1234" -u <jenkins-user>:<password>
```

You should see a queued build in the UI.

---

## 9) Step 7 — Expose Jenkins with ngrok

The GitHub Action needs a public URL to reach Jenkins. ngrok gives you one.

```bash
cd /tmp
curl -LO https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.zip
sudo dnf install -y unzip
unzip ngrok-v3-stable-linux-amd64.zip
sudo mv ngrok /usr/local/bin/

# One-time auth (get token from https://dashboard.ngrok.com)
ngrok config add-authtoken <YOUR_NGROK_AUTHTOKEN>

# Run it (stays in foreground; run in tmux/screen or nohup)
nohup ngrok http 8080 > /tmp/ngrok.log 2>&1 &

# Get the public URL
curl http://127.0.0.1:4040/api/tunnels
```

Copy the `public_url` value, e.g. `https://abcd-123-45-67-89.ngrok-free.app`. Keep the ngrok process running.

> Jenkins on port 8080 → ngrok forwards `https://<sub>.ngrok-free.app` → `localhost:8080`. No Security Group change needed for this to work (outbound only).

---

## 10) Step 8 — Add GitHub Repository Secrets

Per app repo: **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Value |
|--------|-------|
| `JENKINS_URL` | `https://<your-ngrok-subdomain>.ngrok-free.app` (no trailing slash) |
| `JENKINS_TOKEN` | `deploy1234` (must match the job token) |

The workflows already read these via `${{ secrets.JENKINS_URL }}` / `${{ secrets.JENKINS_TOKEN }}` — nothing is hardcoded.

---

## 11) Step 9 — End-to-End Test

```bash
# local machine
git add -A
git commit -m "ci: end-to-end pipeline test"
git push origin main
```

Watch:
1. **GitHub → Actions** — backend-ci and frontend-ci run; on push they build + push images to GHCR.
2. **Jenkins UI** (`http://<IP>:8080`) — `specops-api-deploy-job` / `specops-ui-deploy-job` queue and run.
3. Verify on EC2:

```bash
docker ps                       # specops-mysql, specops-api, specops-ui all Up

# API is up
curl http://localhost:8082/api/dashboard/stats
curl -I http://localhost:8082/swagger-ui.html

# UI is up (nginx)
curl -I http://localhost:8081

# From your browser:
#   http://<EC2_PUBLIC_IP>:8081   → React app
#   http://<EC2_PUBLIC_IP>:8082/swagger-ui.html → Swagger
```

> Note: the backend has **no actuator**, so `.../actuator/health` returns 404. Use `/swagger-ui.html` or `/api/dashboard/stats` instead (or add `spring-boot-starter-actuator` to `pom.xml`).

---

## 12) Troubleshooting

| Symptom | Cause / Fix |
|---------|-------------|
| CI `docker build` fails "Cannot locate Dockerfile" | File is named `DockerFile` — rename to `Dockerfile` (case-sensitive). |
| Jenkins job "Got permission denied … docker.sock" | `sudo usermod -aG docker jenkins && sudo systemctl restart jenkins`. |
| `docker compose pull` → `denied: requested access to the resource is denied` | GHCR login as `jenkins` user: `echo '<PAT>' \| sudo -u jenkins docker login ghcr.io -u <user> --password-stdin`. PAT needs `read:packages`. |
| Job not triggered by push | Secrets missing/mismatched; token must equal `JENKINS_TOKEN`, URL must have **no trailing slash**. Test with `curl "https://<ngrok>/buildByToken/build?job=specops-api-deploy-job&token=deploy1234"`. |
| ngrok URL 502 | Jenkins not running, or ngrok is pointing at the wrong port (must be `http 8080`). Check `curl http://127.0.0.1:8080`. |
| API up but UI can't reach it | Backend service must be named `api` in compose; `nginx.conf` proxies to `http://api:8080`. Check `docker compose ps` and `docker logs specops-ui`. |
| `communications link failure` to MySQL | MySQL container not healthy yet — `depends_on: condition: service_healthy` handles ordering; check `docker logs specops-mysql`. |
| Port 8080/8081/8082 unreachable from browser | Security Group inbound rules missing (see §4.3). |

---

## 13) Repeat for the Other Apps

For each remaining repo (FlowOps, Pheonix, POMXML, Beyond404, DevSquad, BND, Check-em):
1. Copy the pipeline files into that repo (`backend/Dockerfile`, `frontend/Dockerfile`, `.github/workflows/*`, `docker-compose.yml`, `CI-CD-Steps.md`).
2. Update image tags, job names, container names, and the DB name in compose (see §0).
3. Push → add the two secrets → create the two Jenkins jobs with the §8 build script.
4. All apps share one EC2 host (each in its own `/opt/<app>` dir and compose project) or separate hosts — either works.

---

## 14) "To finish" Checklist

- [ ] All pipeline files committed and pushed per repo
- [ ] `JENKINS_URL` + `JENKINS_TOKEN` secrets added per repo
- [ ] Jenkins Freestyle jobs created with "Trigger builds remotely" + compose script
- [ ] `ngrok http 8080` running and `JENKINS_URL` set to the resulting URL
- [ ] End-to-end test green: Actions → GHCR → Jenkins → containers running on 8081/8082
