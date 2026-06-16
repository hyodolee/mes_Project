# MES/MCS 통합 제조 운영 프로젝트

MES(Manufacturing Execution System)와 MCS(Material Control System)를 연동하고, Spring AI 기반 운영 보조 기능까지 확장하는 포트폴리오 프로젝트입니다.

## 프로젝트 목표

- MES는 생산계획, 작업오더, 기준정보, 생산 실적 흐름을 담당합니다.
- MCS는 창고, Zone, Location, 재고, 자재 이동, 경로 최적화 흐름을 담당합니다.
- PLC/설비 이벤트는 실제 설비 대신 PowerShell 시뮬레이터로 재현합니다.
- AI는 상태를 직접 변경하지 않고, 운영 데이터를 분석하고 설명하는 보조 역할로 붙입니다.

## AI 기능 범위

처음 정의한 AI 기능은 아래 3가지입니다.

| 구분 | 역할 | 예시 |
|---|---|---|
| AI 운영 조회 | 사용자가 자연어로 MES/MCS 상태를 질문합니다. | "오늘 실패한 이동오더 있어?", "이 작업오더 왜 시작 안 돼?" |
| AI 운영 분석 | 대시보드나 상세 화면에서 현재 상태, 리스크, 원인, 조치 방향을 요약합니다. | 작업오더 분석, MCS 이동 실패 원인 분석, 생산 지연 리스크 요약 |
| AI 이벤트 알림 | PLC 오류, 인터락, 이동 실패 같은 이벤트를 감지해 알림 문구를 생성합니다. | 문자/이메일/화면 알림 메시지 생성 |

현재 구현된 AI는 1차 버전으로, MES 작업오더 화면에서 특정 작업오더를 분석하는 **AI 작업오더 분석** 기능입니다. 이 기능은 위 3가지 중 **AI 운영 분석**의 첫 구현입니다.

## 시스템 구성

| 영역 | 경로 | 포트 | 역할 |
|---|---|---:|---|
| MES+MCS backend | `mes_backserver/` | 8080 | 생산/기준정보 + 자재 이동/로케이션 재고 통합 API (MCS는 `com.mes.mcs`로 병합) |
| React frontend | `mes_frontend/` | 3000 | MES/MCS 운영 화면 |
| MCS schema | `mcs/` | - | MCS 기준 설치 SQL과 설계 문서 |
| Documents | `docs/` | - | 설계, 시연, 실행, DB 파일 정리 |

## 현재 구현 요약

- MES 작업오더 생성, 시작, 완료, 취소
- MES 작업오더에서 MCS 자재 이동 요청
- MCS 이동오더 자동 생성, LOT/Location/경로 자동 배정
- MCS 경로 관리와 경로 최적화 계산
- PLC 시뮬레이터 기반 성공/실패/복구 이벤트 처리
- MCS 자재 이동 완료 전 MES 작업 시작 차단
- React 화면에서 MES/MCS/PLC 상태 확인
- Spring AI 기반 AI 작업오더 분석 1차 구현

## AI 개발 방향

| 순서 | 기능 | 상태 |
|---:|---|---|
| 1 | AI 작업오더 분석 | 1차 구현 완료, 2차 고도화 예정 |
| 2 | AI 운영 조회 | 예정 |
| 3 | AI 이벤트 알림 | 예정 |
| 4 | RAG 운영 지식 검색 | 별도 예정 |

Fine-tuning은 AI 작업오더 분석 품질 향상을 위해 시도했지만, 현재 OpenAI organization 정책상 새 fine-tuning job 생성이 제한되어 보류합니다. 만들어둔 50건의 사례 데이터셋은 향후 오픈소스 모델 fine-tuning 실험 또는 AI 분석 프롬프트 품질 개선에 활용할 수 있습니다.

## 실행

DB 비밀번호와 API Key는 Git에 저장하지 않습니다. MES/MCS backend 실행 전 필요한 환경변수를 설정합니다.

```powershell
$env:MES_DB_PASSWORD = "your-password"
```

MES+MCS (단일 백엔드):

```powershell
Set-Location 'C:\dev\mes_project\mes_backserver'
.\gradlew.bat bootRun
```

React:

```powershell
Set-Location 'C:\dev\mes_project\mes_frontend'
npm run dev
```

React build:

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
| [docs/design/AI_OPERATION_PLAN.md](docs/design/AI_OPERATION_PLAN.md) | AI 운영 조회/분석/알림 설계 |
| [docs/ai/fine-tuning/AI_FINE_TUNING_PLAN.md](docs/ai/fine-tuning/AI_FINE_TUNING_PLAN.md) | AI fine-tuning 시도와 데이터셋 |
| [docs/db/DB_FILES.md](docs/db/DB_FILES.md) | SQL, dump, patch 파일 위치 |
| [mcs/MCS_설계문서_v2.md](mcs/MCS_설계문서_v2.md) | MCS 기본 설계 문서 |
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
