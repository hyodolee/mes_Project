#!/bin/bash
set -e

# ─────────────────────────────────────────────────────────────
# 부팅 시 1회: 소프트웨어 설치만 담당 (불안정한 타이밍 작업은 배포 워크플로로 이관)
# DuckDNS IP 등록 + Let's Encrypt 발급은 .github/workflows/deploy.yml 에서
# 매 배포마다 멱등(idempotent)하게 실행 → 타이밍에 휘둘리지 않고 자가 치유됨
# ─────────────────────────────────────────────────────────────

# Docker + Compose V2 plugin + certbot 설치 (apt, 안정적)
apt-get update -y
apt-get install -y docker.io docker-compose-plugin certbot curl
systemctl start docker
systemctl enable docker
usermod -aG docker ubuntu

# GitHub Actions SSH는 TTY가 없으므로 sudo가 비밀번호를 묻지 않도록 보장
echo "Defaults !requiretty" > /etc/sudoers.d/99-notty
echo "ubuntu ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/99-ubuntu
chmod 440 /etc/sudoers.d/99-notty /etc/sudoers.d/99-ubuntu

echo "userdata 완료: Docker/certbot 설치됨. DNS/SSL은 배포 시 처리."
