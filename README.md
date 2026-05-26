# MES/MCS 통합 제조 운영 프로젝트

MES(Manufacturing Execution System)와 MCS(Material Control System)를 같은 DB 기반으로 연동하고, 향후 Spring AI를 통해 운영 분석/조회/알림 기능을 확장하는 포트폴리오 프로젝트입니다.

## 프로젝트 목표

- MES는 생산계획, 작업지시, 기준정보, 생산/품질/재고의 상위 업무를 담당합니다.
- MCS는 창고, Zone, Location, 로케이션 재고, 입고/출고/이동 실행을 담당합니다.
- PLC 설비 연동은 실제 장비 대신 Windows PowerShell 기반 시뮬레이터로 대체합니다.
- AI는 업무 상태를 직접 변경하지 않고, 운영 데이터를 요약/분석/설명/알림 문장으로 변환하는 보조 역할을 담당합니다.

## 시스템 구성

| 영역 | 경로 | 포트 | 설명 |
|---|---|---:|---|
| MES | `mes_backserver/` | 8080 | 생산/기준정보 중심 백엔드 |
| MCS | `mcs_backserver/` | 8081 | 물류 실행/로케이션 재고 중심 백엔드 |
| MCS 설계/SQL | `mcs/` | - | MCS DDL, 더미데이터, 업무 흐름 문서 |
| 문서 | `docs/` | - | 현재 기준 문서와 보관 문서 |

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Persistence | MyBatis 3.0.3 |
| Database | MariaDB |
| View | Thymeleaf |
| Build | Gradle |
| AI 확장 | Spring AI 예정 |
| PLC 시뮬레이션 | Windows PowerShell 예정 |

## 현재 구현 요약

| 시스템 | 구현 내용 |
|---|---|
| MES | 기준정보, 생산계획/작업지시, 생산실적, 품질, 재고, 설비 기능 기반 구현 |
| MCS | Zone/Location, 로케이션 재고, 재고 조정, 입고, 출고 워크플로 구현 |
| 연동 | MES/MCS 동일 DB 운영, MCS 테이블은 `MCS_` 접두어 사용 |
| 다음 작업 | MCS 이동 관리, PLC 이벤트 수신, 인터락/알림, AI 운영 기능 |

## AI 기능 범위

| 기능 | 설명 |
|---|---|
| AI 운영 분석 | 대시보드에서 현재 리스크, 지연, 우선 확인 대상을 자동 브리핑 |
| AI 운영 조회 | 사용자가 자연어로 작업/재고/정합성/리포트를 조회 |
| AI 이벤트 알림 | PLC 에러, 인터락, 지연 발생 시 원인/영향/조치 메시지 생성 |

자세한 내용은 [AI_OPERATION_PLAN.md](docs/AI_OPERATION_PLAN.md)를 참고합니다.

## 주요 문서

문서의 현재 기준과 보관 문서는 [docs/README.md](docs/README.md)에 정리되어 있습니다.

| 문서 | 용도 |
|---|---|
| [docs/MCS_DEVELOPMENT_PROGRESS.md](docs/MCS_DEVELOPMENT_PROGRESS.md) | MCS 현재 개발 진행 상황 |
| [mcs/MCS_설계문서_v2.md](mcs/MCS_설계문서_v2.md) | MCS 테이블, ERD, API, 연동 설계 |
| [mcs/MCS_더미데이터_업무흐름.md](mcs/MCS_더미데이터_업무흐름.md) | MCS 더미데이터 기반 업무 시나리오 |
| [AGENTS.md](AGENTS.md) | Codex/에이전트 작업 가이드 |
| [CLAUDE.md](CLAUDE.md) | Claude 작업 가이드 |

## 실행 명령

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

MCS 빌드:

```powershell
Set-Location 'C:\dev\mes_project\mcs_backserver'
.\gradlew.bat build -x test
```

## 포트폴리오 데모 시나리오

```text
PLC 시뮬레이터
→ MES/MCS API 호출
→ 출고/이동/재고 상태 변경
→ 인터락 또는 이벤트 로그 저장
→ AI 운영 분석/조회/알림
```

최종 목표는 단순 CRUD가 아니라, 제조 현장의 생산 실행과 물류 실행, 설비 이벤트, 운영 모니터링, AI 보조 분석이 하나의 흐름으로 보이는 시스템입니다.
