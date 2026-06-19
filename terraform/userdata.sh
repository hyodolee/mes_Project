#!/bin/bash
set -e

# ─────────────────────────────────────────────────────────────
# 부팅 시 1회 실행 (cloud-init).
# 핵심: SSH 접속(배포)이 일어나기 '전에' DuckDNS를 이 서버 IP로 맞춰둔다.
#   → EC2를 새로 만들어 IP가 바뀌어도, EC2_HOST=도메인 으로 첫 배포가 깨지지 않음.
# certbot SSL 발급/갱신은 deploy.yml 에서 멱등하게 처리(여기선 설치만).
# ─────────────────────────────────────────────────────────────

# GitHub Actions SSH는 TTY가 없으므로 sudo가 비밀번호를 묻지 않도록 먼저 보장
echo "Defaults !requiretty" > /etc/sudoers.d/99-notty
echo "ubuntu ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/99-ubuntu
chmod 440 /etc/sudoers.d/99-notty /etc/sudoers.d/99-ubuntu

# DuckDNS: 부팅 즉시 이 서버의 공인 IP를 도메인에 등록 (실패해도 배포 시 재갱신되므로 비치명적)
PUBLIC_IP=$$(curl -s ifconfig.me || true)
curl -s "https://www.duckdns.org/update?domains=${duckdns_subdomain}&token=${duckdns_token}&ip=$${PUBLIC_IP}" || true
echo "DuckDNS 등록 시도: ${duckdns_subdomain}.duckdns.org -> $${PUBLIC_IP}"

# Docker 공식 설치 스크립트 (docker-ce + compose V2 plugin 포함, 가장 안정적)
curl -fsSL https://get.docker.com | sh

# certbot (Ubuntu 기본 저장소)
apt-get install -y certbot curl

systemctl enable --now docker
usermod -aG docker ubuntu

echo "userdata 완료: DNS 등록 + Docker/compose/certbot 설치됨. SSL은 배포 시 처리."
