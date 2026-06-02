# DB/SQL 파일 정리

이 문서는 프로젝트의 SQL, dump, patch 파일 위치를 정리합니다.

## 현재 기준 DB dump

| 경로 | 용도 |
|---|---|
| `db/dumps/current/dump-MES_DB-202605261055.sql` | MES/MCS 통합 기준 DB dump |

개발 환경에서 전체 DB를 복원하거나 현재 통합 상태를 확인할 때 사용합니다.

## 보관 dump

| 경로 | 용도 |
|---|---|
| `db/dumps/archive/mes_backend_server_backup.sql` | MCS 통합 전 MES 단독 초기 백업 |
| `db/dumps/archive/dump-MES_DB-202605261009.sql` | 재고 unique 보정 전 MES/MCS 통합 dump |

보관 dump는 과거 상태 참고용입니다.

## 초기 MES DDL/더미데이터

| 경로 | 용도 |
|---|---|
| `files/mes_ddl_*.sql` | MES 초기 테이블, 뷰, 프로시저 DDL |
| `dummy/battery_dummy_*.sql` | MES 초기 배터리 도메인 더미데이터 |

현재 통합 DB 복원은 `db/dumps/current/` dump를 우선합니다.

## MCS 기준 설치 SQL

| 경로 | 용도 |
|---|---|
| `mcs/MCS_install.sql` | MCS 전용 테이블, 공통코드, 더미데이터 설치 SQL |

이 파일은 `AGENTS.md`에서 고정 참조하므로 위치를 유지합니다.

## MCS 추가 patch SQL

| 경로 | 용도 |
|---|---|
| `db/patches/mcs/MCS_add_failed_transfer_status.sql` | `MCS_TF_STATUS = FAILED` 공통코드 추가 |
| `db/patches/mcs/MCS_route_optimization.sql` | MCS 경로 최적화 테이블, 공통코드, 데모 그래프 추가 |
| `db/patches/mcs/MCS_route_demo_bypass_patch.sql` | NCM-01-01 to NCM-01-02 우회 경로 보정 |
| `db/patches/mcs/MCS_route_demo_cv01_block_bypass_patch.sql` | `E-NCM01-CV01` 막힘 시 우회 경로 보정 |
| `db/patches/mcs/MCS_route_demo_ncm_bypass_test_setup.sql` | NCM 우회 테스트용 경로 세팅 |

기존 DB에 기능을 추가할 때만 실행합니다. 전체 DB를 새로 복원한 경우에는 현재 dump에 이미 반영되어 있는지 먼저 확인하세요.

## 기타 patch SQL

| 경로 | 용도 |
|---|---|
| `db/patches/20260526_create_mcs_plc_event_log.sql` | PLC 이벤트 로그 테이블 초기 patch |

## Flyway 마이그레이션

| 경로 | 용도 |
|---|---|
| `mes_backserver/src/main/resources/db/migration/` | MES backend Flyway 마이그레이션 |

이미 적용된 Flyway 버전 파일은 수정하지 않습니다. 변경이 필요하면 다음 버전 파일로 추가합니다.

## 적용된 주요 DB 보정

- `INV_STOCK.LOT_NO`, `INV_STOCK.LOCATION_CD`: `NOT NULL DEFAULT ''`
- `MCS_LOCATION_STOCK.LOT_NO`: `NOT NULL DEFAULT ''`
- `MCS_TRANSFER_ORDER` 상태 코드에 `FAILED` 추가
- MCS 경로 최적화용 route node/edge/transfer route 구조 추가
- MES 작업오더와 MCS 이동오더를 연결하는 `MES_REF_TYPE`, `MES_REF_ID` 흐름 추가

## 보안 규칙

- DB 비밀번호, Aiven 서비스 비밀번호, API key는 SQL/문서/소스에 저장하지 않습니다.
- 비밀번호는 환경변수 또는 로컬 전용 설정 파일에서만 관리합니다.
