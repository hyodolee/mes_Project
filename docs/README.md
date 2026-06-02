# MES/MCS 문서 인덱스

이 디렉터리는 프로젝트 문서의 진입점입니다. 새로 작업하는 사람이나 AI 에이전트는 이 파일에서 필요한 문서로 이동하면 됩니다.

## 핵심 문서

| 문서 | 용도 |
|---|---|
| [../README.md](../README.md) | 프로젝트 전체 개요와 실행 명령 |
| [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md) | 현재 상태와 다음 개발 순서 |
| [MCS_DEVELOPMENT_PROGRESS.md](MCS_DEVELOPMENT_PROGRESS.md) | MCS 기능별 진행 현황 |
| [../AGENTS.md](../AGENTS.md) | Codex/AI 에이전트 작업 가이드 |
| [../CLAUDE.md](../CLAUDE.md) | Claude 작업 가이드 |

## 설계 문서

| 문서 | 용도 |
|---|---|
| [design/MES_MCS_INTEGRATION_PLAN.md](design/MES_MCS_INTEGRATION_PLAN.md) | MES/MCS 업무 연동 구조 |
| [design/MCS_ROUTE_OPTIMIZATION_DESIGN.md](design/MCS_ROUTE_OPTIMIZATION_DESIGN.md) | MCS 경로 최적화 설계 |
| [design/AI_OPERATION_PLAN.md](design/AI_OPERATION_PLAN.md) | Spring AI 기반 운영 분석/조회/알림 설계 |
| [design/REACT_FRONTEND_MIGRATION_PLAN.md](design/REACT_FRONTEND_MIGRATION_PLAN.md) | React 전환 계획 |
| [../mcs/MCS_설계문서_v2.md](../mcs/MCS_설계문서_v2.md) | MCS 원본 DDL/ERD/API 설계 |
| [../mcs/MCS_더미데이터_업무흐름.md](../mcs/MCS_더미데이터_업무흐름.md) | MCS 더미데이터 기반 업무 흐름 |

## 실행/시연 문서

| 문서 | 용도 |
|---|---|
| [runbooks/DEMO_SCENARIOS.md](runbooks/DEMO_SCENARIOS.md) | 정상/실패/복구 시연 순서 |
| [runbooks/RUNBOOK_REACT_MES_MCS.md](runbooks/RUNBOOK_REACT_MES_MCS.md) | MES/MCS/React 실행 점검 |
| [runbooks/MES_MCS_MATERIAL_FLOW_TEST_PLAN.md](runbooks/MES_MCS_MATERIAL_FLOW_TEST_PLAN.md) | MES/MCS 자재 이동 테스트 순서 |

## DB/SQL 문서

| 문서 | 용도 |
|---|---|
| [db/DB_FILES.md](db/DB_FILES.md) | DB dump, 초기 DDL, MCS 설치 SQL, patch SQL 위치 |
| [audits/MCS_INVENTORY_CONSISTENCY_AUDIT.md](audits/MCS_INVENTORY_CONSISTENCY_AUDIT.md) | MCS 재고 정합성 점검 |

## 보관 문서

초기 MES 단독 개발 문서는 [archive/mes](archive/mes)에 보관합니다. 현재 기준 문서가 아니며, 과거 설계와 진행 이력 참고용입니다.

## 문서 관리 규칙

- 현재 개발 기준 문서는 `docs/README.md`, `docs/PROJECT_ROADMAP.md`, `docs/MCS_DEVELOPMENT_PROGRESS.md`입니다.
- 설계 문서는 `docs/design/`에 둡니다.
- 시연, 실행, 테스트 문서는 `docs/runbooks/`에 둡니다.
- 점검/감사 문서는 `docs/audits/`에 둡니다.
- DB/SQL 위치 설명은 `docs/db/DB_FILES.md`에 둡니다.
- 실행 가능한 추가 SQL patch는 `db/patches/` 아래에 둡니다.
- `mcs/MCS_install.sql`과 `mcs/MCS_설계문서_v2.md`는 `AGENTS.md`에서 고정 참조하므로 이동하지 않습니다.
