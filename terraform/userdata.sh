#!/bin/bash
set -e

DUCKDNS_TOKEN="${duckdns_token}"
DUCKDNS_SUBDOMAIN="${duckdns_subdomain}"
EMAIL="${email}"
DOMAIN="$${DUCKDNS_SUBDOMAIN}.duckdns.org"

# 1. Docker + Docker Compose V2 plugin + certbot 설치 (apt, 안정적)
apt-get update -y
apt-get install -y docker.io docker-compose-plugin certbot curl
systemctl start docker
systemctl enable docker
usermod -aG docker ubuntu

# sudo가 TTY 없어도 동작하도록 (GitHub Actions SSH는 TTY 없음)
echo "Defaults !requiretty" > /etc/sudoers.d/99-notty
echo "ubuntu ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/99-ubuntu
chmod 440 /etc/sudoers.d/99-notty /etc/sudoers.d/99-ubuntu

# 2. DuckDNS에 현재 IP 자동 등록
PUBLIC_IP=$$(curl -s ifconfig.me)
curl -s "https://www.duckdns.org/update?domains=$${DUCKDNS_SUBDOMAIN}&token=$${DUCKDNS_TOKEN}&ip=$${PUBLIC_IP}"
echo "DuckDNS 업데이트 완료: $${DOMAIN} -> $${PUBLIC_IP}"

# 3. DNS 전파 대기 (90초)
echo "DNS 전파 대기 중..."
sleep 90

# 4. Let's Encrypt SSL 인증서 자동 발급
certbot certonly --standalone \
  -d "$${DOMAIN}" \
  -m "$${EMAIL}" \
  --agree-tos \
  --non-interactive

echo "SSL 인증서 발급 완료: $${DOMAIN}"
