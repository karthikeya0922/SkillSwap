#!/usr/bin/env bash
# One-shot installer for a fresh Ubuntu server (tested target: Oracle Cloud Always Free, Ubuntu 22.04/24.04, ARM or x86).
# Run from the repo root:  bash deploy/setup.sh
# Re-running it is safe: it keeps the existing .env and just rebuilds and restarts the stack.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> Installing Docker"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
fi

echo "==> Adding 2 GB swap (helps the Maven build on small machines)"
if ! sudo swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

echo "==> Opening ports 80 and 443 in the OS firewall"
# Oracle's Ubuntu images ship iptables rules that reject everything except SSH.
for port in 80 443; do
  if ! sudo iptables -C INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null; then
    sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport "$port" -j ACCEPT 2>/dev/null \
      || sudo iptables -I INPUT -p tcp --dport "$port" -j ACCEPT
  fi
done
if command -v netfilter-persistent >/dev/null; then
  sudo netfilter-persistent save
fi

if [ ! -f .env ]; then
  echo "==> Generating .env with fresh secrets"
  ip=$(curl -fsS https://api.ipify.org)
  site="${ip//./-}.sslip.io"
  rand() { openssl rand -hex "$1"; }
  cat > .env <<EOF
SITE_ADDRESS=$site
PUBLIC_URL=https://$site
DB_PASSWORD=$(rand 16)
MYSQL_ROOT_PASSWORD=$(rand 16)
JWT_SECRET=$(rand 32)
SEED_ENABLED=true
SEED_DEMO_PASSWORD=Demo@1$(rand 4)
ADMIN_EMAIL=admin@skillswap.dev
ADMIN_PASSWORD=Admin@1$(rand 8)
EOF
  chmod 600 .env
fi

echo "==> Building and starting SkillSwap (first build takes several minutes)"
sudo docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml up -d --build

set -a; . ./.env; set +a
cat <<EOF

SkillSwap is starting at: $PUBLIC_URL
(the backend needs about a minute after the build before logins work)

Demo students:  <firstname>@skillswap.dev  /  $SEED_DEMO_PASSWORD
Admin:          $ADMIN_EMAIL  /  $ADMIN_PASSWORD
These are stored in .env on this server.

Logs:    sudo docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml logs -f backend
Update:  git pull && bash deploy/setup.sh
EOF
