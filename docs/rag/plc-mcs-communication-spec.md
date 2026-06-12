# PLC-MCS 통신 정의서

## 문서 목적

이 문서는 PLC가 MCS로 설비 이벤트를 전달할 때 사용하는 통신 규격을 정의합니다.
AI 운영 챗봇과 장애 분석 기능은 이 문서를 근거로 이벤트 의미, 필수 필드, 상태 전이, payload 누락 원인을 설명합니다.

## 적용 범위

| 항목 | 내용 |
|---|---|
| 송신 주체 | PLC 또는 PLC 시뮬레이터 |
| 수신 주체 | MCS Backserver |
| 주요 대상 | 자재 이동 이벤트, 설비 상태 이벤트, 오류/인터락 이벤트 |
| 통신 데이터 | JSON payload |
| 저장 위치 | `MCS_PLC_EVENT` |

## 이벤트 타입 정의

| 이벤트 타입 | 의미 | MCS 처리 | AI 분석 키워드 |
|---|---|---|---|
| `TRANSFER_STARTED` | PLC가 자재 이동 시작을 보고 | 이동오더를 `IN_PROGRESS`로 변경 | 이동 시작, 반송 시작, 도착지 누락 |
| `TRANSFER_COMPLETED` | PLC가 자재 이동 완료를 보고 | 이동오더를 `COMPLETED`로 변경하고 재고 반영 | 이동 완료, 재고 반영, LOT 확인 |
| `EQUIPMENT_RUNNING` | 설비가 정상 동작 중임을 보고 | 이벤트 로그만 저장 | 설비 가동, 정상 신호 |
| `EQUIPMENT_ERROR` | 설비 오류 발생 | 관련 이동오더를 `FAILED`로 변경 | 장비 오류, 설비 고장, 오류 코드 |
| `ARRIVED_WRONG_LOCATION` | 자재가 예상 위치와 다른 위치에서 감지 | 관련 이동오더를 `FAILED`로 변경 | 오도착, 위치 불일치 |
| `INTERLOCK_BLOCKED` | 인터락 조건으로 이동 차단 | 관련 이동오더를 `FAILED`로 변경 | 인터락, 안전 정지, 조건 미충족 |

## 이벤트 상태 정의

| 상태 | 의미 | 처리 기준 |
|---|---|---|
| `NORMAL` | 정상 이벤트 | 이벤트 타입에 맞게 이동 상태 갱신 |
| `ERROR` | 오류 이벤트 | 실패 처리 또는 운영자 확인 필요 |
| `INTERLOCK` | 인터락 이벤트 | 이동 차단 및 현장 조건 확인 필요 |

## 공통 Payload 구조

```json
{
  "equipmentCd": "CV-001",
  "eventType": "TRANSFER_STARTED",
  "eventStatus": "NORMAL",
  "targetType": "TRANSFER",
  "targetId": 12,
  "locationCd": "NCM-01-01",
  "fromLocationCd": "NCM-01-01",
  "toLocationCd": "NCM-01-02",
  "lotNo": "LOT-SIM-001",
  "errorCode": "",
  "message": "Transfer 12 started",
  "eventDtm": "2026-06-09T11:26:39"
}
```

## Payload 필드 정의

| 필드 | 필수 여부 | 설명 | 예시 |
|---|---:|---|---|
| `equipmentCd` | 필수 | 이벤트를 보낸 설비 코드 | `CV-001` |
| `eventType` | 필수 | PLC 이벤트 타입 | `TRANSFER_STARTED` |
| `eventStatus` | 필수 | 이벤트 상태 | `NORMAL`, `ERROR`, `INTERLOCK` |
| `targetType` | 조건부 | 이벤트 대상 유형 | `TRANSFER` |
| `targetId` | 조건부 필수 | MCS 이동오더 ID | `12` |
| `locationCd` | 조건부 필수 | 현재 감지 위치 | `NCM-01-01` |
| `fromLocationCd` | 선택 | 출발 위치 | `NCM-01-01` |
| `toLocationCd` | 조건부 필수 | 도착 위치 | `NCM-01-02` |
| `lotNo` | 조건부 필수 | 이동 대상 LOT 번호 | `LOT-SIM-001` |
| `errorCode` | 오류 시 필수 | PLC 오류 코드 | `MOTOR_OVERLOAD` |
| `message` | 오류 시 필수 | 이벤트 설명 또는 오류 메시지 | `Motor overload detected` |
| `eventDtm` | 선택 | PLC 이벤트 발생 시각 | `2026-06-09T11:26:39` |

## 이벤트별 필수 필드

| 이벤트 타입 | 필수 필드 | 누락 시 영향 |
|---|---|---|
| `TRANSFER_STARTED` | `eventType`, `eventStatus`, `targetId`, `equipmentCd`, `locationCd`, `toLocationCd`, `lotNo` | MCS가 이동 대상, 현재 위치, 도착 위치, LOT를 확정할 수 없어 이동 시작 처리를 보류 |
| `TRANSFER_COMPLETED` | `eventType`, `eventStatus`, `targetId`, `equipmentCd`, `locationCd`, `lotNo` | 완료 위치와 LOT 확인이 불가능해 재고 반영 보류 |
| `EQUIPMENT_RUNNING` | `eventType`, `eventStatus`, `targetId`, `equipmentCd` | 설비 가동 이벤트 추적 불가 |
| `EQUIPMENT_ERROR` | `eventType`, `eventStatus`, `targetId`, `equipmentCd`, `errorCode`, `message` | 오류 원인 식별 불가, 운영자 조치 지연 |
| `ARRIVED_WRONG_LOCATION` | `eventType`, `eventStatus`, `targetId`, `equipmentCd`, `locationCd`, `errorCode`, `message` | 실제 감지 위치 확인 불가 |
| `INTERLOCK_BLOCKED` | `eventType`, `eventStatus`, `targetId`, `equipmentCd`, `errorCode`, `message` | 인터락 조건 확인 불가 |

## 정상 이동 시작 예시

```json
{
  "equipmentCd": "CV-001",
  "eventType": "TRANSFER_STARTED",
  "eventStatus": "NORMAL",
  "targetType": "TRANSFER",
  "targetId": 12,
  "locationCd": "NCM-01-01",
  "fromLocationCd": "NCM-01-01",
  "toLocationCd": "NCM-01-02",
  "lotNo": "LOT-SIM-001",
  "message": "Transfer 12 started"
}
```

## 오류 이동 시작 예시

아래 payload는 `toLocationCd`와 `lotNo`가 누락된 오류 예시입니다.

```json
{
  "equipmentCd": "CV-001",
  "eventType": "TRANSFER_STARTED",
  "eventStatus": "NORMAL",
  "targetType": "TRANSFER",
  "targetId": 12,
  "locationCd": "NCM-01-01",
  "fromLocationCd": "NCM-01-01",
  "message": "Transfer started"
}
```

AI는 이 경우 다음 근거로 답변해야 합니다.

- `TRANSFER_STARTED` 이벤트에서 `toLocationCd`와 `lotNo`는 필수 필드입니다.
- `toLocationCd`가 없으면 MCS는 도착 위치를 확정할 수 없습니다.
- `lotNo`가 없으면 MCS는 어떤 LOT를 이동시키는지 확정할 수 없습니다.
- PLC 송신 payload 생성부에서 `TO_LOCATION_CD`, `LOT_NO` 태그 매핑을 확인해야 합니다.

## MCS 처리 결과 기준

| 처리 결과 | 의미 | AI 설명 기준 |
|---|---|---|
| `SUCCESS` | MCS가 이벤트를 정상 처리 | 정상 처리된 이벤트로 설명 |
| `VALIDATION_FAILED` | 필수 필드 또는 값 검증 실패 | 누락 필드, 필요한 이유, PLC 태그 확인 위치를 설명 |
| `FAILED` | 처리 중 오류 발생 | 오류 메시지와 관련 이동오더 상태를 함께 설명 |

## AI 답변 기준

AI는 PLC-MCS 통신 오류를 설명할 때 다음 순서를 지킵니다.

1. 어떤 이벤트에서 문제가 발생했는지 설명합니다.
2. 어떤 필드가 누락되었거나 잘못되었는지 설명합니다.
3. 해당 필드가 MCS 처리에 왜 필요한지 설명합니다.
4. PLC 태그 매핑표 기준 어떤 태그를 확인해야 하는지 설명합니다.
5. 운영자가 확인할 화면 또는 조치 방향을 제안합니다.

예시 답변:

```text
TRANSFER_STARTED 이벤트에서 도착 위치와 LOT 정보가 빠져 있어요.
이동 시작 처리에는 도착 위치와 이동 대상 LOT가 필요해서, MCS가 이동 상태를 확정하기 어렵습니다.
PLC 송신부에서 TO_LOCATION_CD와 LOT_NO 태그가 payload에 들어가는지 확인해보세요.
```
