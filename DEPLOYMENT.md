# Private V2 First Deploy Runbook

This is the shortest first-deploy sequence for a private, single-user V2 deployment using:

- Tailscale private access
- Caddy reverse proxy
- Angular static frontend
- Spring Boot backend on `127.0.0.1:8080`
- SQLite on persistent disk

No app-level authentication is included. Do not expose this deployment publicly.

## 0. Version Check

The backend is configured for Java 17 in `backend-java/pom.xml`:

```xml
<java.version>17</java.version>
```

Use Java 17 on the VPS.

## 1. VPS Setup

Assumption: Ubuntu 22.04 or 24.04 LTS.

SSH into the VPS:

```bash
ssh your_user@your_server_ip
```

Install base packages and Java:

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y curl git rsync sqlite3 ufw openjdk-17-jdk
java -version
```

Install Caddy:

```bash
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
  | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
  | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update
sudo apt install -y caddy
```

Install and connect Tailscale:

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
tailscale status
tailscale ip -4
```

Save the server's Tailscale IPv4 address. This runbook calls it:

```text
100.x.y.z
```

Lock down public inbound traffic:

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status
```

Verify:

- `java -version` shows Java 17.
- `tailscale status` shows the VPS in your tailnet.
- `tailscale ip -4` returns `100.x.y.z`.
- Public ports `80`, `443`, and `8080` are not opened in UFW.

## 2. Server Folder Layout

On the VPS:

```bash
sudo mkdir -p /opt/monthly-dashboard/app
sudo mkdir -p /opt/monthly-dashboard/frontend
sudo mkdir -p /opt/monthly-dashboard/data
sudo mkdir -p /opt/monthly-dashboard/backups
sudo chown -R your_user:your_user /opt/monthly-dashboard
chmod 700 /opt/monthly-dashboard/data
```

Expected layout:

```text
/opt/monthly-dashboard/
  app/
    monthly-dashboard-0.0.1-SNAPSHOT.jar
  frontend/
    index.html
    main-*.js
    styles-*.css
    assets/
  data/
    monthly-dashboard.db
    monthly-dashboard.db-wal
    monthly-dashboard.db-shm
  backups/
```

SQLite database path:

```text
/opt/monthly-dashboard/data/monthly-dashboard.db
```

## 3. Build Locally

On your local machine, from the repo root.

Backend:

```bash
cd backend-java
mvn test
mvn package
cd ..
```

Verify the jar exists:

```bash
ls backend-java/target/monthly-dashboard-0.0.1-SNAPSHOT.jar
```

Frontend:

```bash
cd frontend-angular/frontend-angular
npm install
npm run build
cd ../..
```

Find the Angular `index.html`:

```bash
ls frontend-angular/frontend-angular/dist/frontend-angular
ls frontend-angular/frontend-angular/dist/frontend-angular/browser
```

Use whichever folder contains `index.html`.

## 4. Copy Files To VPS

From your local machine, repo root.

Copy backend jar:

```bash
scp backend-java/target/monthly-dashboard-0.0.1-SNAPSHOT.jar \
  your_user@your_server_ip:/opt/monthly-dashboard/app/
```

Copy frontend. If `index.html` is directly in `dist/frontend-angular`:

```bash
rsync -av --delete frontend-angular/frontend-angular/dist/frontend-angular/ \
  your_user@your_server_ip:/opt/monthly-dashboard/frontend/
```

If `index.html` is in `dist/frontend-angular/browser`, use this instead:

```bash
rsync -av --delete frontend-angular/frontend-angular/dist/frontend-angular/browser/ \
  your_user@your_server_ip:/opt/monthly-dashboard/frontend/
```

Verify on the VPS:

```bash
ls -lah /opt/monthly-dashboard/app
ls -lah /opt/monthly-dashboard/frontend
test -f /opt/monthly-dashboard/frontend/index.html && echo "frontend ok"
```

## 5. Backend Systemd Service

On the VPS, create:

```bash
sudo nano /etc/systemd/system/monthly-dashboard.service
```

Paste this, replacing `your_user` and `100.x.y.z`:

```ini
[Unit]
Description=Monthly Dashboard Spring Boot Backend
After=network-online.target
Wants=network-online.target

[Service]
User=your_user
WorkingDirectory=/opt/monthly-dashboard/app

Environment=SPRING_PROFILES_ACTIVE=prod
Environment=APP_DATASOURCE_URL=jdbc:sqlite:/opt/monthly-dashboard/data/monthly-dashboard.db
Environment=APP_CORS_ALLOWED_ORIGINS=http://100.x.y.z
Environment=APP_JPA_DDL_AUTO=update
Environment=APP_LOG_LEVEL=INFO

ExecStart=/usr/bin/java -jar /opt/monthly-dashboard/app/monthly-dashboard-0.0.1-SNAPSHOT.jar

Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Start it:

```bash
sudo systemctl daemon-reload
sudo systemctl enable monthly-dashboard
sudo systemctl start monthly-dashboard
sudo systemctl status monthly-dashboard
```

Verify backend locally:

```bash
curl http://127.0.0.1:8080/api/v1/categories
```

Logs:

```bash
journalctl -u monthly-dashboard -f
```

## 6. Caddy Same-Origin Routing

On the VPS, edit:

```bash
sudo nano /etc/caddy/Caddyfile
```

Use this, replacing `100.x.y.z`:

```caddyfile
{
    auto_https off
}

http://100.x.y.z {
    bind 100.x.y.z

    root * /opt/monthly-dashboard/frontend
    encode gzip zstd

    handle /api/v1/* {
        reverse_proxy 127.0.0.1:8080
    }

    handle {
        try_files {path} /index.html
        file_server
    }
}
```

Apply it:

```bash
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
sudo systemctl status caddy
```

Verify from a device logged into your tailnet:

```text
http://100.x.y.z
```

Expected routing:

```text
http://100.x.y.z/        -> Angular frontend
http://100.x.y.z/api/v1 -> Spring Boot backend
```

The SPA fallback is:

```caddyfile
try_files {path} /index.html
```

## 7. SQLite Backup

Back up the SQLite database on the VPS:

```bash
sudo systemctl stop monthly-dashboard

BACKUP_DIR="/opt/monthly-dashboard/backups/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

cp /opt/monthly-dashboard/data/monthly-dashboard.db "$BACKUP_DIR/" 2>/dev/null || true
cp /opt/monthly-dashboard/data/monthly-dashboard.db-wal "$BACKUP_DIR/" 2>/dev/null || true
cp /opt/monthly-dashboard/data/monthly-dashboard.db-shm "$BACKUP_DIR/" 2>/dev/null || true

sudo systemctl start monthly-dashboard

ls -lah "$BACKUP_DIR"
```

Verify backup integrity:

```bash
sqlite3 "$BACKUP_DIR/monthly-dashboard.db" "PRAGMA integrity_check;"
```

Expected:

```text
ok
```

Restore test:

```bash
RESTORE_TEST="/tmp/monthly-dashboard-restore-test"
rm -rf "$RESTORE_TEST"
mkdir -p "$RESTORE_TEST"
cp "$BACKUP_DIR"/* "$RESTORE_TEST/"
sqlite3 "$RESTORE_TEST/monthly-dashboard.db" "PRAGMA integrity_check;"
```

Expected:

```text
ok
```

## 8. First-Run Smoke Test

1. Backend service is running:

```bash
sudo systemctl status monthly-dashboard
```

2. Backend API responds locally:

```bash
curl http://127.0.0.1:8080/api/v1/categories
```

3. Caddy is running:

```bash
sudo systemctl status caddy
```

4. Frontend loads privately from a tailnet device:

```text
http://100.x.y.z
```

5. Dashboard monthly view loads on desktop.
6. Today Checklist loads.
7. Create a category.
8. Edit a category from the monthly occurrence section.
9. Edit a category from the right-side Categories panel.
10. Create a task while active task count is below 15.
11. Confirm the task appears in the dashboard.
12. Edit the task.
13. Complete a checklist item.
14. Undo completion from the completed checkmark.
15. Test scoped edit with `THIS_AND_FOLLOWING` or `ALL_FUTURE`.
16. Confirm old completions remain intact.
17. Check a phone-sized viewport and confirm Today Checklist, Add Task, and Create Category access are visible.
18. Test export:

```bash
curl http://100.x.y.z/api/v1/export
```

19. Run the backup steps.
20. Run the restore integrity check.

## 9. Safety Constraints

Must remain private:

- The app UI
- `/api/v1`
- `/api/v1/export`
- SQLite files under `/opt/monthly-dashboard/data`
- Backups under `/opt/monthly-dashboard/backups`

Do not publicly expose:

- `8080`
- `80`
- `443`

Acceptable for private V2:

- No app-level authentication yet, because Tailscale/private access protects the app.
- SQLite instead of Postgres.
- Manual backups.
- Maximum 15 active tasks.
- Read-only monthly grid.
- No import yet.
- No true `THIS_OCCURRENCE` editing.
