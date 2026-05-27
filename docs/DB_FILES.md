# DB/SQL 파일 정리

프로젝트의 SQL 파일은 목적별로 아래처럼 구분합니다.

## 현재 기준 덤프

| 경로 | 용도 |
|---|---|
| `db/dumps/current/dump-MES_DB-202605261055.sql` | MES와 MCS가 연동된 현재 기준 DB 덤프 |

이 덤프는 현재 개발 기준 데이터베이스입니다. 새 환경에서 전체 DB를 복원하거나, 현재 MES/MCS 통합 상태를 확인할 때 사용합니다.

## 보관 덤프

| 경로 | 용도 |
|---|---|
| `db/dumps/archive/mes_backend_server_backup.sql` | MCS 통합 전 MES 단독 초기 백업 |
| `db/dumps/archive/dump-MES_DB-202605261009.sql` | 재고 unique 보정 전 MES/MCS 통합 DB 덤프 |

보관 덤프는 과거 상태 참고용입니다. 현재 개발 기준으로 사용하지 않습니다.

## 초기 MES DDL/더미

| 경로 | 용도 |
|---|---|
| `files/mes_ddl_*.sql` | MES 초기 테이블/뷰/프로시저 DDL |
| `dummy/battery_dummy_*.sql` | MES 초기 배터리 도메인 더미데이터 |

이 파일들은 초기 MES 스키마와 더미데이터를 파트별로 확인할 때 사용합니다. 현재 통합 DB 복원 기준은 `db/dumps/current/`의 덤프입니다.

## MCS 설치 SQL

| 경로 | 용도 |
|---|---|
| `mcs/MCS_install.sql` | MCS 전용 테이블, 공통코드, 더미데이터 설치 SQL |

MCS 설계 변경이나 독립 설치 흐름을 확인할 때 사용합니다. 현재 DB 전체 복원은 통합 덤프를 우선합니다.

## Flyway 마이그레이션

| 경로 | 용도 |
|---|---|
| `mes_backserver/src/main/resources/db/migration/` | MES 애플리케이션용 Flyway 마이그레이션 |

이미 적용된 Flyway 버전 파일은 수정하지 않고, 새 변경은 다음 버전 파일로 추가합니다.

## 최근 DB 보정

재고 중복 방지를 위해 운영 DB에는 아래 방향의 ALTER를 적용했습니다.

- `INV_STOCK.LOT_NO`, `INV_STOCK.LOCATION_CD`: `NOT NULL DEFAULT ''`
- `MCS_LOCATION_STOCK.LOT_NO`: `NOT NULL DEFAULT ''`

이 보정은 nullable 컬럼이 포함된 unique key에서 `ON DUPLICATE KEY UPDATE`가 기대대로 동작하지 않는 문제를 줄이기 위한 것입니다.
