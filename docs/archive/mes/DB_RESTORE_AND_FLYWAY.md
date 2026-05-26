# DB 복원 + Flyway 운영 가이드

## 목적
- 초기 데이터베이스는 백업 파일로 복원한다.
- 복원 이후 스키마 변경은 Flyway로 버전 관리한다.

## 1) 초기 복원 (1회)

프로젝트 루트 기준:

```bash
# MariaDB 접속 가능 상태에서
mariadb -u root -p < mes_backend_server_backup.sql
```

복원 확인:

```bash
mariadb -u root -p -e "SHOW DATABASES LIKE 'mes_backend_server';"
```

## 2) 애플리케이션 설정

- `mes_backserver`는 Flyway가 활성화되어 있음
- 주요 설정(`application.yml`):
  - `baseline-on-migrate: true`
  - `baseline-version: 1`
  - `locations: classpath:db/migration`

의미:
- 기존 운영/복원 DB(이미 테이블 존재)에서는 Flyway가 V1 기준선(baseline)을 기록하고,
  이후 버전(V2+)부터 관리한다.

## 3) 현재 마이그레이션 상태

- `V2__baseline_marker.sql`
  - 기준선 이후 동작 확인용 최소 마이그레이션
- `V3__add_master_indexes.sql`
  - `MST_COMPANY`, `MST_PLANT` 조회 성능용 인덱스 추가
  - 중복 생성 방지를 위해 `information_schema.statistics` 기반 조건부 생성 적용

## 4) 다음 변경 작업 규칙

1. 테이블/인덱스/컬럼 변경은 반드시 `db/migration`에 버전 SQL로 추가
2. 기존 적용된 버전 파일은 수정하지 않는다 (새 버전 파일로 누적)
3. 배포 전에 로컬/개발 환경에서 마이그레이션 적용 검증

예시 파일명:
- `V3__create_mst_company_indexes.sql`
- `V4__add_mst_item_search_columns.sql`
