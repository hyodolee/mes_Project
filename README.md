# MES/MCS 통합 제조 운영 프로젝트

MES(Manufacturing Execution System)와 MCS(Material Control System)를 같은 DB 기반으로 연동하고, 이후 Spring AI로 운영 조회/분석/알림 기능을 확장하는 포트폴리오 프로젝트입니다.

## 프로젝트 목표

- MES는 생산계획, 작업오더, 기준정보, 생산 실적 흐름을 담당합니다.
- MCS는 창고, Zone, Location, 재고, 자재 이동, 경로 최적화 흐름을 담당합니다.
- PLC/설비 이벤트는 실제 장비 대신 PowerShell 시뮬레이터로 재현합니다.
- AI는 상태를 직접 변경하지 않고, 운영 데이터를 분석하고 설명하는 보조 역할로 붙입니다.

## 시스템 구성

| 영역 | 경로 | 포트 | 역할 |
|---|---|---:|---|
| MES backend | `mes_backserver/` | 8080 | 생산/기준정보 중심 API |
| MCS backend | `mcs_backserver/` | 8081 | 자재 이동/로케이션 재고 중심 API |
| React frontend | `mes_frontend/` | 3000 | MES/MCS 운영 화면 |
| MCS schema | `mcs/` | - | MCS 기준 설치 SQL과 설계 문서 |
| Documents | `docs/` | - | 설계, 시연, 실행, DB 파일 정리 |

## 현재 구현 요약

- MES 작업오더 생성, 시작, 완료, 취소
- MES 작업오더에서 MCS 자재 이동 요청
- MCS 이동오더 자동 생성, LOT/로케이션/경로 자동 배정
- MCS 경로 관리와 경로 최적화 계산
- PLC 시뮬레이터 기반 성공/실패/복구 이벤트 처리
- MCS 자재 이동 완료 전 MES 작업 시작 차단
- React 화면에서 MES/MCS/PLC 상태 확인

## 실행

DB 비밀번호는 Git에 저장하지 않습니다. MES/MCS backend 실행 전 `MES_DB_PASSWORD` 환경변수를 설정하세요.

```powershell
$env:MES_DB_PASSWORD = "your-password"
```

MES:

```powershell
Set-Location 'C:\dev\mes_project\mes_backserver'
.\gradlew.bat bootRun
```

MCS:

```powershell
Set-Location 'C:\dev\mes_project\mcs_backserver'
.\gradlew.bat bootRun
```

React:

```powershell
Set-Location 'C:\dev\mes_project\mes_frontend'
npm run dev
```

React 빌드:

```powershell
Set-Location 'C:\dev\mes_project\mes_frontend'
npm run build
```

## 주요 문서

문서 입구는 [docs/README.md](docs/README.md)입니다.

| 문서 | 용도 |
|---|---|
| [docs/PROJECT_ROADMAP.md](docs/PROJECT_ROADMAP.md) | 현재 상태와 다음 개발 순서 |
| [docs/MCS_DEVELOPMENT_PROGRESS.md](docs/MCS_DEVELOPMENT_PROGRESS.md) | MCS 기능별 진행 현황 |
| [docs/runbooks/DEMO_SCENARIOS.md](docs/runbooks/DEMO_SCENARIOS.md) | 정상/실패/복구 시연 순서 |
| [docs/design/AI_OPERATION_PLAN.md](docs/design/AI_OPERATION_PLAN.md) | Spring AI 기반 AI 기능 설계 |
| [docs/db/DB_FILES.md](docs/db/DB_FILES.md) | SQL, dump, patch 파일 위치 |
| [mcs/MCS_설계문서_v2.md](mcs/MCS_설계문서_v2.md) | MCS 원본 설계 문서 |
| [AGENTS.md](AGENTS.md) | Codex/AI 에이전트 작업 가이드 |

## 시연 흐름

```text
MES 작업오더 생성
-> MES 자재 요청
-> MCS 이동오더 자동 생성
-> MCS 경로 계산
-> PLC 이벤트 시뮬레이션
-> 성공: MCS 완료, MES 작업 시작 가능
-> 실패: MCS 실패, MES 작업 시작 차단
-> 복구: MCS 실패 건 취소, MES 재요청, PLC 성공 처리
```
