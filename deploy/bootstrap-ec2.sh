#!/usr/bin/env bash
set -euo pipefail
dnf install -y java-21-amazon-corretto-headless mariadb105-server nginx rsync openssl
systemctl enable --now mariadb
install -d -o ec2-user -g ec2-user /opt/businesstrips /var/lib/biztrips/uploads /var/www/biztrips
if [[ ! -f /etc/biztrips.env ]]; then
  db_password=$(openssl rand -hex 24)
  mariadb <<SQL
CREATE DATABASE IF NOT EXISTS db_biztrips;
CREATE USER IF NOT EXISTS 'biztrips'@'localhost' IDENTIFIED BY '$db_password';
GRANT ALL PRIVILEGES ON db_biztrips.* TO 'biztrips'@'localhost';
SQL
  umask 077
  cat > /etc/biztrips.env <<EOF
SPRING_PROFILES_ACTIVE=mariadb
DB_URL=jdbc:mariadb://127.0.0.1:3306/db_biztrips
DB_USER=biztrips
DB_PASSWORD=$db_password
APP_STORAGE_LOCATION=/var/lib/biztrips/uploads
EOF
fi
cat > /etc/systemd/system/businesstrips.service <<'EOF'
[Unit]
Description=M324 Business Trips RefCard03-04
After=network.target mariadb.service
Requires=mariadb.service
[Service]
User=ec2-user
WorkingDirectory=/opt/businesstrips
EnvironmentFile=/etc/biztrips.env
ExecStart=/usr/bin/java -XX:MaxRAMPercentage=60 -jar /opt/businesstrips/app.jar
Restart=on-failure
RestartSec=5
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ReadWritePaths=/var/lib/biztrips/uploads
[Install]
WantedBy=multi-user.target
EOF
cat > /etc/nginx/conf.d/biztrips.conf <<'EOF'
server {
  listen 80 default_server;
  server_name _;
  root /var/www/biztrips;
  client_max_body_size 6m;
  location /v1/ { proxy_pass http://127.0.0.1:8080; }
  location = /actuator/health { proxy_pass http://127.0.0.1:8080; }
  location / { try_files $uri $uri/ /index.html; }
}
EOF
# Disable the distribution's competing default virtual host.
sed -i '/^    server {/,/^    }/d' /etc/nginx/nginx.conf
printf '<h1>M324 Business Trips</h1><p>Waiting for the first frontend deployment.</p>' > /var/www/biztrips/index.html
systemctl daemon-reload
nginx -t
systemctl enable --now nginx
