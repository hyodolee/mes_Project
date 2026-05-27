# 프로젝트 현재 상태와 다음 작업

이 문서는 다른 AI 에이전트나 개발자가 프로젝트를 이어받을 때 먼저 확인하는 작업 로드맵입니다.

## 한 줄 요약

MES/MCS 기본 기능과 통합 DB 기반은 준비되어 있고, 다음 핵심 작업은 `MCS 이동 관리 → PLC 이벤트 시뮬레이션 → 인터락/알림 → AI 운영 보조` 순서입니다.

## 현재 완료 상태

| 영역 | 상태 | 비고 |
|---|---|---|
| MES 기본 시스템 | 완료 | 기준정보, 생산계획/작업지시, 생산실적, 품질, 재고, 설비 기능 기반 구현 |
| MCS 기본 시스템 | 부분 완료 | Zone/Location, 로케이션 재고, 재고 조정, 입고, 출고 구현 |
| MES/MCS DB 통합 | 완료 | 동일 `MES_DB` 사용, MCS 테이블은 `MCS_` 접두어 |
| 현재 기준 DB 덤프 | 완료 | `db/dumps/current/dump-MES_DB-202605261055.sql` |
| SQL/문서 정리 | 완료 | `docs/README.md`, `docs/DB_FILES.md` 기준 |
| 재고 unique 보정 | 완료 | `INV_STOCK`, `MCS_LOCATION_STOCK` nullable unique 문제 보정 쿼리 적용 |
| AI 기능 방향 | 완료 | 운영 분석, 운영 조회, 이벤트 알림 3개로 정리 |

## 다음 작업 우선순위

### 1. MCS 이동 관리 완성

가장 먼저 해야 할 작업입니다. PLC 이벤트와 AI 알림은 이동 처리 로직이 있어야 자연스럽게 붙습니다.

| 작업 | 목표 |
|---|---|
| 이동 오더 CRUD | `MCS_TRANSFER_ORDER` 생성/조회/수정 |
| 이동 품목 관리 | `MCS_TRANSFER_ITEM` 등록/수정 |
| 상태 전이 | `REQUESTED → IN_PROGRESS → COMPLETED/CANCELLED` |
| 이동 완료 처리 | 출발 로케이션 재고 감소, 도착 로케이션 재고 증가 |
| 이력 기록 | `MCS_LOC_TRANS_HIS`에 `TF_OUT`, `TF_IN` 기록 |
| MES 연동 | `INV_STOCK`, `INV_TRANS_HIS` 동기화 |

완료 기준:

```text
이동 오더 생성
→ 이동 시작
→ 이동 완료
→ MCS 로케이션 재고 양쪽 반영
→ MES 창고/로케이션 재고 동기화
→ 이력 조회 가능
```

### 2. PLC 이벤트 시뮬레이터

실제 PLC 대신 Windows PowerShell 스크립트로 설비 이벤트를 발생시킵니다.

| 작업 | 목표 |
|---|---|
| PLC 이벤트 로그 테이블 | `MCS_PLC_EVENT_LOG` |
| 이벤트 수신 API | 예: `POST /api/plc/events` |
| PowerShell 스크립트 | 시나리오별 API 호출 |
| 이벤트 처리 서비스 | 이벤트 유형에 따라 MCS/MES 상태 변경 |

이벤트 예:

```text
TRANSFER_STARTED
TRANSFER_COMPLETED
TRANSFER_FAILED
EQUIPMENT_ERROR
PRODUCTION_STARTED
PRODUCTION_COMPLETED
```

### 3. 인터락/알림 기반

업무적으로 막아야 하는 조건을 룰 기반으로 처리하고, 발생 내역을 알림 이벤트로 남깁니다.

| 작업 | 목표 |
|---|---|
| `InterlockService` | 출고/이동/재고 처리 전 확정 룰 검사 |
| `MCS_INTERLOCK_LOG` | 인터락 차단 이력 저장 |
| `MCS_ALERT_EVENT` | 알림 대상 이벤트 생성 |
| 알림 목록 화면/API | 운영자가 문제 상황 확인 |

인터락 예:

```text
재고 부족이면 출고/이동 불가
BLOCKED 로케이션으로 이동 불가
이미 완료된 이동오더 재완료 불가
MES 작업지시가 취소 상태면 출고 진행 불가
MES 재고와 MCS 로케이션 재고 불일치 시 확정 보류
```

### 4. AI 운영 기능

AI는 상태를 직접 변경하지 않습니다. 룰 기반 시스템이 만든 데이터와 이벤트를 사람이 이해하기 쉬운 문장으로 정리합니다.

| 기능 | 설명 |
|---|---|
| AI 운영 분석 | 대시보드에서 현재 리스크, 지연, 우선 확인 대상을 자동 브리핑 |
| AI 운영 조회 | 사용자가 자연어로 작업/재고/정합성/리포트 조회 |
| AI 이벤트 알림 | 에러/인터락/지연 발생 시 원인/영향/조치 메시지 생성 |

구현 순서:

```text
1. AI 운영 분석 대시보드
2. AI 이벤트 알림
3. AI 운영 조회 챗봇
```

## 전체 개발 순서

```text
1. MCS 이동 관리 완성
2. 이동 완료 시 재고 정합성 보장
3. PLC 이벤트 수신 API와 로그 테이블
4. PowerShell PLC 시뮬레이터
5. 인터락 로그와 알림 이벤트
6. AI 운영 분석
7. AI 이벤트 알림
8. AI 운영 조회
```

## 주요 참고 문서

| 문서 | 용도 |
|---|---|
| `docs/MCS_DEVELOPMENT_PROGRESS.md` | MCS 기능별 진행 현황 |
| `docs/AI_OPERATION_PLAN.md` | AI 운영 기능 범위 |
| `docs/DB_FILES.md` | SQL 덤프/DDL/더미데이터 위치 |
| `mcs/MCS_설계문서_v2.md` | MCS 테이블과 연동 설계 |
| `mcs/MCS_더미데이터_업무흐름.md` | 더미데이터 기반 업무 흐름 |

## 설계 원칙

- MES/MCS는 같은 `MES_DB`를 사용합니다.
- MCS 전용 테이블은 `MCS_` 접두어를 사용합니다.
- 업무/이력 테이블은 `*_ID` 기반 surrogate key를 PK로 사용합니다.
- 업무적으로 중복되면 안 되는 조합은 nullable 없는 unique key로 관리합니다.
- 인터락 판단은 AI가 아니라 백엔드 룰 기반으로 처리합니다.
- AI는 분석/조회/알림 문장 생성에만 사용합니다.
