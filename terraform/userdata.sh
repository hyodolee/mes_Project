#!/bin/bash
set -e

DUCKDNS_TOKEN="${duckdns_token}"
DUCKDNS_SUBDOMAIN="${duckdns_subdomain}"
EMAIL="${email}"
DOMAIN="$${DUCKDNS_SUBDOMAIN}.duckdns.org"

# 1. Docker 설치
apt-get update -y
apt-get install -y docker.io curl
systemctl start docker
systemctl enable docker
usermod -aG docker ubuntu

# 2. Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$$(uname -s)-$$(uname -m)" \
  -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 3. certbot 설치
apt-get install -y certbot

# 4. DuckDNS에 현재 IP 자동 등록
PUBLIC_IP=$$(curl -s ifconfig.me)
curl -s "https://www.duckdns.org/update?domains=$${DUCKDNS_SUBDOMAIN}&token=$${DUCKDNS_TOKEN}&ip=$${PUBLIC_IP}"
echo "DuckDNS 업데이트 완료: $${DOMAIN} → $${PUBLIC_IP}"

# 5. DNS 전파 대기 (90초)
echo "DNS 전파 대기 중..."
sleep 90

# 6. Let's Encrypt SSL 인증서 자동 발급
certbot certonly --standalone \
  -d "$${DOMAIN}" \
  -m "$${EMAIL}" \
  --agree-tos \
  --non-interactive

echo "✅ SSL 인증서 발급 완료: $${DOMAIN}"
