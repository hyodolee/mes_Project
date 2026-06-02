# 프로젝트 문서 정리

이 문서는 MES/MCS 프로젝트의 문서 입구입니다. 새로 작업할 때는 먼저 이 파일을 보고, 필요한 문서만 따라가면 됩니다.

## 현재 기준 문서

| 문서 | 역할 |
|---|---|
| [../README.md](../README.md) | 프로젝트 전체 소개, 구조, 실행 명령 |
| [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md) | 현재 상태, 다음 작업, 개발 우선순위 |
| [MCS_DEVELOPMENT_PROGRESS.md](MCS_DEVELOPMENT_PROGRESS.md) | MCS 개발 진행 현황과 다음 작업 |
| [AI_OPERATION_PLAN.md](AI_OPERATION_PLAN.md) | AI 운영 분석/조회/알림 기능 범위 |
| [DB_FILES.md](DB_FILES.md) | SQL 덤프, 초기 DDL, 더미데이터 파일 정리 |
| [../mcs/MCS_설계문서_v2.md](../mcs/MCS_설계문서_v2.md) | MCS 테이블, ERD, API, MES 연동 설계 |
| [../mcs/MCS_더미데이터_업무흐름.md](../mcs/MCS_더미데이터_업무흐름.md) | MCS 더미데이터와 업무 시나리오 |
| [../AGENTS.md](../AGENTS.md) | Codex 작업 가이드 |
| [../CLAUDE.md](../CLAUDE.md) | Claude 작업 가이드 |

## 보관 문서

아래 문서는 MES 단독 개발 초기 단계에서 작성된 문서입니다. 현재 작업의 기준 문서는 아니지만, 테이블 설계와 과거 진행 내역 참고용으로 보관합니다.

| 문서 | 비고 |
|---|---|
| [archive/mes/MES_백엔드_설계문서.md](archive/mes/MES_백엔드_설계문서.md) | MES 백엔드 초기 설계 문서 |
| [archive/mes/mes_database_design.md](archive/mes/mes_database_design.md) | MES DB 상세 설계서 |
| [archive/mes/DATABASE_SETUP.md](archive/mes/DATABASE_SETUP.md) | 초기 DB 셋업 가이드 |
| [archive/mes/DB_RESTORE_AND_FLYWAY.md](archive/mes/DB_RESTORE_AND_FLYWAY.md) | 초기 DB 복원/Flyway 가이드 |
| [archive/mes/BACKEND_DEVELOPMENT_PLAN.md](archive/mes/BACKEND_DEVELOPMENT_PLAN.md) | 초기 MES 백엔드 개발 계획 |
| [archive/mes/BACKEND_PROGRESS_CHECK.md](archive/mes/BACKEND_PROGRESS_CHECK.md) | 초기 MES 백엔드 진행 체크 |

## 문서 관리 규칙

- 현재 개발 기준은 `README.md`, `docs/PROJECT_ROADMAP.md`, `docs/MCS_DEVELOPMENT_PROGRESS.md`, `mcs/MCS_설계문서_v2.md`입니다.
- 오래된 계획 문서는 삭제하지 않고 `docs/archive/` 아래로 이동합니다.
- 새 기능의 진행 상태는 먼저 `docs/MCS_DEVELOPMENT_PROGRESS.md`에 반영합니다.
- AI/PLC/모니터링 관련 설계는 `docs/AI_OPERATION_PLAN.md`에 요약하고, 구현이 커지면 별도 문서로 분리합니다.
- SQL 덤프와 DB 설치 파일은 `docs/DB_FILES.md` 기준으로 분류합니다.

## React Frontend Migration

- [REACT_FRONTEND_MIGRATION_PLAN.md](REACT_FRONTEND_MIGRATION_PLAN.md): React + Mantis 프론트엔드 전환 계획
## MCS Route Optimization

- [MCS_ROUTE_OPTIMIZATION_DESIGN.md](MCS_ROUTE_OPTIMIZATION_DESIGN.md): MCS 경로 최적화 설계, DB/API/화면/PLC 시나리오

## Demo And AI

- [DEMO_SCENARIOS.md](DEMO_SCENARIOS.md): Fixed normal/failure/recovery demo scenarios for MES/MCS/PLC.
- [AI_OPERATION_PLAN.md](AI_OPERATION_PLAN.md): Spring AI based operation analysis and query plan.
