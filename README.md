# MES (Manufacturing Execution System) Project

Spring Boot 3.3 + MyBatis + MariaDB + Thymeleaf 기반의 현대화된 MES 백엔드 프로젝트입니다.

## 💻 프로젝트 소개

제조 현장의 실시간 생산 관리 및 기준정보 관리를 위한 시스템입니다. 기존의 레거시 설계를 바탕으로 **Java 21** 및 **Spring Boot 3.3** 환경으로 새롭게 구축되었습니다.

## 🗓 개발 기간 (현대화 작업)

* 2026.02.28 ~ 진행 중

## ⚙ 개발 환경 (Modernized)

* **Java**: 21 (OpenJDK)
* **Framework**: Spring Boot 3.3.5
* **Build Tool**: Gradle 8.10.2
* **Database**: MariaDB 10.11+
* **ORM**: MyBatis 3.0.x
* **View**: Thymeleaf (초기 단계), 향후 React 전환 예정

## 🚀 주요 구현 기능

### 1. 공통 모듈
* 표준 응답 포맷 (`ApiResponse`)
* 공통 페이징 처리 (`PageRequest`, `PageResponse`)
* 전역 예외 처리 및 비즈니스 예외 정의

### 2. 기준정보 관리 (Master Data) - Phase 1 완료
* **회사 관리**: CRUD 및 목록 조회
* **공장 관리**: CRUD 및 목록 조회
* **품목 관리**: CRUD, 검색, 페이징 기능 포함

### 3. 생산계획/작업지시 (Planning) - Phase 2 완료
* **생산계획 관리**: 조회/등록/페이징
* **작업지시 관리**: CRUD, 상태전이(대기→진행→완료/취소), 페이징

### 4. UI/UX 개선
* 사용여부(Y/N) 입력 방식 개선 (Select Box 적용)
* 목록 페이지 페이징 네비게이션 구현
* 입력 폼 레이아웃 및 필드 너비 최적화

## 📁 프로젝트 구조

* `/mes_backserver`: Spring Boot 백엔드 소스 코드
* `/docs`: 개발 계획서, 진행 체크리스트, DB 복구 가이드 등
* `/files`: DB DDL 및 초기 설정 SQL
* `/dummy`: 테스트를 위한 더미 데이터 SQL

## 📚 구축 로드맵

1. **기준정보 관리** (완료 - 회사/공장/품목)
2. **작업지시/생산계획** (완료 - 생산계획/작업지시 CRUD/페이징)
3. 생산실적 관리 (다음 단계)
4. 품질관리
5. 재고/자재관리
6. 설비관리
7. 모니터링/분석

## 📄 라이센스

Copyright 2026 MES 개발팀. All rights reserved.
