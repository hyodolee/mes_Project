# PLC-MCS 통신 레퍼런스

이 문서는 RAG 기반 PLC 오류 분석을 준비하기 위한 테스트용 현장 문서입니다. 실제 현장 문서가 들어오기 전까지 통신 정의서, 태그 매핑표, PLC 송신 코드 샘플 역할을 합니다.

## 1. 이벤트 정의

| 이벤트 | 설명 | MCS 처리 |
|---|---|---|
| `TRANSFER_STARTED` | PLC가 자재 이동 시작을 보고합니다. | 이동오더를 `IN_PROGRESS`로 변경 |
| `TRANSFER_COMPLETED` | PLC가 자재 이동 완료를 보고합니다. | 이동오더를 `COMPLETED`로 변경하고 재고 반영 |
| `EQUIPMENT_RUNNING` | 설비가 동작 중임을 보고합니다. | 이벤트 로그만 저장 |
| `EQUIPMENT_ERROR` | 설비 오류가 발생했습니다. | 이동오더를 `FAILED`로 변경 |
| `ARRIVED_WRONG_LOCATION` | 자재가 예상 위치와 다른 곳에서 감지됐습니다. | 이동오더를 `FAILED`로 변경 |
| `INTERLOCK_BLOCKED` | 인터락 조건으로 이동이 차단됐습니다. | 이동오더를 `FAILED`로 변경 |

## 2. 필수 필드 정의

| 이벤트 | 필수 필드 |
|---|---|
| `TRANSFER_STARTED` | `targetId`, `equipmentCd`, `locationCd`, `toLocationCd`, `lotNo` |
| `TRANSFER_COMPLETED` | `targetId`, `equipmentCd`, `locationCd`, `lotNo` |
| `EQUIPMENT_RUNNING` | `targetId`, `equipmentCd`, `eventType` |
| `EQUIPMENT_ERROR` | `targetId`, `equipmentCd`, `errorCode`, `message` |
| `ARRIVED_WRONG_LOCATION` | `targetId`, `equipmentCd`, `errorCode`, `message` |
| `INTERLOCK_BLOCKED` | `targetId`, `equipmentCd`, `errorCode`, `message` |

## 3. Payload 정의

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

## 4. PLC 태그 매핑표

| MCS 필드 | PLC 태그 예시 | 설명 |
|---|---|---|
| `targetId` | `DB100.DBD0` / `TRANSFER_ID` | MCS 이동오더 ID |
| `equipmentCd` | `DB100.STRING20` / `EQUIPMENT_CD` | 이벤트를 보낸 설비 코드 |
| `eventType` | `DB110.STRING30` / `EVENT_TYPE` | PLC 이벤트 타입 |
| `eventStatus` | `DB110.STRING20` / `EVENT_STATUS` | NORMAL, ERROR, INTERLOCK |
| `locationCd` | `DB120.STRING20` / `CURRENT_LOCATION_CD` | 현재 감지 Location |
| `fromLocationCd` | `DB120.STRING20` / `FROM_LOCATION_CD` | 출발 Location |
| `toLocationCd` | `DB120.STRING20` / `TO_LOCATION_CD` | 도착 Location |
| `lotNo` | `DB130.STRING40` / `LOT_NO` | 이동 대상 LOT 번호 |
| `errorCode` | `DB140.STRING30` / `ERROR_CODE` | 오류 코드 |
| `message` | `DB150.STRING100` / `EVENT_MESSAGE` | 오류 또는 이벤트 메시지 |

## 5. 대표 오류와 확인 위치

| 오류 | 원인 후보 | PLC 확인 위치 |
|---|---|---|
| `toLocationCd` 누락 | 도착지 태그가 payload에 매핑되지 않음 | `TRANSFER_STARTED` payload 생성부, `TO_LOCATION_CD` 태그 |
| `lotNo` 누락 | LOT 태그가 payload에 매핑되지 않음 | `TRANSFER_STARTED`/`TRANSFER_COMPLETED` payload 생성부, `LOT_NO` 태그 |
| `targetId` 누락 | MCS 이동오더 ID를 PLC에 전달하지 못함 | 작업 시작 명령 수신부, `TRANSFER_ID` 태그 |
| `equipmentCd` 누락 | 설비 코드 상수 또는 태그 초기화 누락 | 설비별 공통 송신 블록 |
| `errorCode` 누락 | 오류 이벤트 생성 시 오류 코드 매핑 누락 | `EQUIPMENT_ERROR`/`INTERLOCK_BLOCKED` 이벤트 생성부 |

## 6. 정상 PLC ST 코드 샘플

```pascal
payload.equipmentCd := EQUIPMENT_CD;
payload.eventType := 'TRANSFER_STARTED';
payload.eventStatus := 'NORMAL';
payload.targetType := 'TRANSFER';
payload.targetId := TRANSFER_ID;
payload.locationCd := CURRENT_LOCATION_CD;
payload.fromLocationCd := FROM_LOCATION_CD;
payload.toLocationCd := TO_LOCATION_CD;
payload.lotNo := LOT_NO;
payload.message := CONCAT('Transfer ', DINT_TO_STRING(TRANSFER_ID), ' started');

SendMcsEvent(payload);
```

## 7. 오류 PLC ST 코드 샘플

아래 코드는 `toLocationCd`와 `lotNo` 매핑이 누락된 테스트용 오류 예시입니다.

```pascal
payload.equipmentCd := EQUIPMENT_CD;
payload.eventType := 'TRANSFER_STARTED';
payload.eventStatus := 'NORMAL';
payload.targetType := 'TRANSFER';
payload.targetId := TRANSFER_ID;
payload.locationCd := CURRENT_LOCATION_CD;
payload.fromLocationCd := FROM_LOCATION_CD;
(* payload.toLocationCd := TO_LOCATION_CD; 누락 *)
(* payload.lotNo := LOT_NO; 누락 *)
payload.message := 'Transfer started';

SendMcsEvent(payload);
```

## 8. AI 분석 기준

AI는 PLC 검증 실패가 발생하면 다음 순서로 설명해야 합니다.

1. 어떤 이벤트에서 문제가 발생했는지
2. 어떤 필드가 누락됐는지
3. 그 필드가 왜 필요한지
4. 태그 매핑표 기준 어떤 PLC 태그를 확인해야 하는지
5. PLC 송신 payload 생성부에서 어떤 매핑을 확인해야 하는지

예시 답변:

```text
TRANSFER_STARTED 이벤트에서 도착 Location(toLocationCd)이 누락되었습니다.
통신 정의서 기준 해당 필드는 이동 시작 시 필수입니다.
태그 매핑표 기준 TO_LOCATION_CD 태그가 JSON payload의 toLocationCd로 매핑되어야 합니다.
PLC 송신 payload 생성부에서 TO_LOCATION_CD 대입 로직을 확인하세요.
```
