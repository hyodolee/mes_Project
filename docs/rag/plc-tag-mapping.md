# PLC 태그 매핑표

## 문서 목적

이 문서는 MCS가 수신하는 JSON payload 필드와 PLC 내부 태그/주소의 매핑 관계를 정의합니다.
AI는 필드 누락이나 값 불일치가 발생했을 때 이 문서를 근거로 어떤 PLC 태그와 송신 로직을 확인해야 하는지 안내합니다.

## 매핑 원칙

- MCS 필드명은 camelCase JSON 필드명을 사용합니다.
- PLC 태그명은 현장 가독성을 위해 대문자 snake case를 사용합니다.
- PLC 주소는 예시이며 실제 현장 주소는 설비별 PLC 프로그램에서 확정합니다.
- 필수 필드는 이벤트 타입별로 다르므로 통신 정의서의 "이벤트별 필수 필드"와 함께 확인합니다.

## 공통 태그 매핑

| MCS JSON 필드 | PLC 태그명 | PLC 주소 예시 | 데이터 타입 | 설명 | 누락 시 증상 |
|---|---|---|---|---|---|
| `targetId` | `TRANSFER_ID` | `DB100.DBD0` | DINT | MCS 이동오더 ID | 어떤 이동오더의 이벤트인지 식별 불가 |
| `equipmentCd` | `EQUIPMENT_CD` | `DB100.STRING20` | STRING | 이벤트를 보낸 설비 코드 | 설비 추적 불가 |
| `eventType` | `EVENT_TYPE` | `DB110.STRING30` | STRING | PLC 이벤트 타입 | MCS 처리 분기 불가 |
| `eventStatus` | `EVENT_STATUS` | `DB110.STRING20` | STRING | 이벤트 상태 | 정상/오류/인터락 판단 불가 |
| `locationCd` | `CURRENT_LOCATION_CD` | `DB120.STRING20` | STRING | 현재 감지 위치 | 현재 위치 확인 불가 |
| `fromLocationCd` | `FROM_LOCATION_CD` | `DB120.STRING20` | STRING | 출발 위치 | 이동 경로 검증 약화 |
| `toLocationCd` | `TO_LOCATION_CD` | `DB120.STRING20` | STRING | 도착 위치 | 이동 시작 또는 경로 확인 불가 |
| `lotNo` | `LOT_NO` | `DB130.STRING40` | STRING | 이동 대상 LOT 번호 | 재고/LOT 이동 추적 불가 |
| `errorCode` | `ERROR_CODE` | `DB140.STRING30` | STRING | 오류 코드 | 오류 원인 식별 불가 |
| `message` | `EVENT_MESSAGE` | `DB150.STRING100` | STRING | 오류 또는 이벤트 메시지 | 운영자 조치 근거 부족 |
| `eventDtm` | `EVENT_DTM` | `DB160.DATE_AND_TIME` | DATETIME | PLC 이벤트 발생 시각 | 이벤트 발생 시점 추적 약화 |

## 이벤트별 핵심 태그

### TRANSFER_STARTED

| 목적 | MCS 필드 | PLC 태그 | 확인 위치 |
|---|---|---|---|
| 이동오더 식별 | `targetId` | `TRANSFER_ID` | 작업 시작 명령 수신부 |
| 현재 위치 확인 | `locationCd` | `CURRENT_LOCATION_CD` | 위치 센서 판정부 |
| 도착 위치 확인 | `toLocationCd` | `TO_LOCATION_CD` | 이동 명령/목적지 설정부 |
| LOT 확인 | `lotNo` | `LOT_NO` | 자재 인식/LOT 전달부 |

### TRANSFER_COMPLETED

| 목적 | MCS 필드 | PLC 태그 | 확인 위치 |
|---|---|---|---|
| 이동오더 식별 | `targetId` | `TRANSFER_ID` | 완료 이벤트 생성부 |
| 완료 위치 확인 | `locationCd` | `CURRENT_LOCATION_CD` | 도착 위치 센서 판정부 |
| LOT 확인 | `lotNo` | `LOT_NO` | 자재 인식/LOT 전달부 |

### EQUIPMENT_ERROR

| 목적 | MCS 필드 | PLC 태그 | 확인 위치 |
|---|---|---|---|
| 설비 식별 | `equipmentCd` | `EQUIPMENT_CD` | 설비 공통 설정 블록 |
| 오류 코드 | `errorCode` | `ERROR_CODE` | 알람 코드 생성부 |
| 오류 메시지 | `message` | `EVENT_MESSAGE` | 알람 메시지 매핑부 |

### INTERLOCK_BLOCKED

| 목적 | MCS 필드 | PLC 태그 | 확인 위치 |
|---|---|---|---|
| 설비 식별 | `equipmentCd` | `EQUIPMENT_CD` | 설비 공통 설정 블록 |
| 인터락 코드 | `errorCode` | `ERROR_CODE` | 인터락 조건 판정부 |
| 인터락 설명 | `message` | `EVENT_MESSAGE` | 인터락 메시지 매핑부 |

## 정상 PLC ST 코드 샘플

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

## 누락 오류 PLC ST 코드 샘플

아래 코드는 이동 시작 이벤트에서 도착 위치와 LOT 매핑이 누락된 예시입니다.

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

## AI가 태그 매핑을 설명하는 방식

AI는 태그 누락 질문에 답할 때 PLC 주소만 나열하지 않고, 현장 담당자가 확인할 지점을 함께 설명합니다.

예시:

```text
도착 위치가 빠진 경우에는 TO_LOCATION_CD 태그가 payload.toLocationCd로 들어가는지 확인해야 합니다.
이 태그는 이동 명령의 목적지 설정부에서 만들어지고, TRANSFER_STARTED 이벤트 생성 시 함께 실려야 합니다.
```
