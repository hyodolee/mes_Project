#!/bin/bash
set -e

# ─────────────────────────────────────────────────────────────
# 부팅 시 1회: 소프트웨어 설치만 담당
# DuckDNS IP 등록 + Let's Encrypt 발급은 .github/workflows/deploy.yml 에서
# 매 배포마다 멱등(idempotent)하게 실행 → 타이밍에 휘둘리지 않고 자가 치유됨
# ─────────────────────────────────────────────────────────────

# GitHub Actions SSH는 TTY가 없으므로 sudo가 비밀번호를 묻지 않도록 먼저 보장
# (뒤의 설치가 실패해도 이 설정은 유효하도록 맨 앞에 둠)
echo "Defaults !requiretty" > /etc/sudoers.d/99-notty
echo "ubuntu ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/99-ubuntu
chmod 440 /etc/sudoers.d/99-notty /etc/sudoers.d/99-ubuntu

# Docker 공식 설치 스크립트 (docker-ce + compose V2 plugin 포함, 가장 안정적)
curl -fsSL https://get.docker.com | sh

# certbot (Ubuntu 기본 저장소에 있음)
apt-get install -y certbot curl

systemctl enable --now docker
usermod -aG docker ubuntu

echo "userdata 완료: Docker/compose/certbot 설치됨. DNS/SSL은 배포 시 처리."
