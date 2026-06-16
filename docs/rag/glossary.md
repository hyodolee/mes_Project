# 현장 용어 ↔ 시스템 용어 사전

## 문서 목적

이 문서는 현장 담당자가 쓰는 자연어 표현과 MCS/PLC 시스템 용어(이벤트 타입, 필드, PLC 태그)를 연결합니다.
AI는 사용자가 기술 필드명을 모르고 현장 용어로 질문해도 이 매핑을 근거로 올바른 이벤트·필드·태그를 찾습니다.

> 이 사전은 검색 보강용입니다. 필드의 정식 정의는 [plc-mcs-communication-spec.md](plc-mcs-communication-spec.md),
> 태그 매핑은 [plc-tag-mapping.md](plc-tag-mapping.md), 조치 절차는 [plc-troubleshooting-sop.md](plc-troubleshooting-sop.md)를 따릅니다.

## 필드 동의어

| 현장/자연어 표현 | MCS 필드 | PLC 태그 |
|---|---|---|
| 도착지, 목적지, 보낼 위치, 도착 위치 | `toLocationCd` | `TO_LOCATION_CD` |
| 출발지, 시작 위치, 보내는 위치 | `fromLocationCd` | `FROM_LOCATION_CD` |
| 현재 위치, 지금 위치, 감지 위치 | `locationCd` | `CURRENT_LOCATION_CD` |
| 로트, LOT, 자재 번호, 묶음 번호 | `lotNo` | `LOT_NO` |
| 이동오더, 작업 번호, 이송 번호, 대상 번호 | `targetId` | `TRANSFER_ID` |
| 설비, 장비, 호기, 컨베이어 | `equipmentCd` | `EQUIPMENT_CD` |
| 오류 코드, 알람 코드, 에러 번호 | `errorCode` | `ERROR_CODE` |
| 오류 메시지, 알람 내용, 에러 설명 | `message` | `EVENT_MESSAGE` |

## 이벤트 동의어

| 현장/자연어 표현 | 이벤트 타입 |
|---|---|
| 이동 시작, 반송 시작, 이송 시작 | `TRANSFER_STARTED` |
| 이동 완료, 반송 완료, 도착 완료 | `TRANSFER_COMPLETED` |
| 설비 가동, 정상 동작, 운전 중 | `EQUIPMENT_RUNNING` |
| 설비 오류, 장비 고장, 설비 정지, 알람 | `EQUIPMENT_ERROR` |
| 오도착, 다른 곳 도착, 위치 불일치 | `ARRIVED_WRONG_LOCATION` |
| 인터락, 안전 정지, 조건 미충족, 이동 차단 | `INTERLOCK_BLOCKED` |

## 처리 결과 동의어

| 현장/자연어 표현 | MCS 처리 결과 |
|---|---|
| 정상 처리, 잘 됨 | `SUCCESS` |
| 데이터 누락, 필수 값 빠짐, 정보 없음 | `VALIDATION_FAILED` |
| 처리 실패, 오류로 실패 | `FAILED` |

## 상태 동의어

| 현장/자연어 표현 | 이동오더 상태 |
|---|---|
| 요청됨, 대기 | `REQUESTED` |
| 이동 중, 진행 중 | `IN_PROGRESS` |
| 완료됨, 끝남 | `COMPLETED` |
| 실패함, 멈춤 | `FAILED` |
| 취소됨 | `CANCELLED` |

## 사용 예시

| 사용자 질문(현장 용어) | AI가 연결해야 할 시스템 용어 |
|---|---|
| "자재 이동이 실패했는데 목적지 정보가 없대요" | `TRANSFER_STARTED` + `toLocationCd`(`TO_LOCATION_CD`) 누락 → `VALIDATION_FAILED` |
| "어떤 묶음을 옮기는지 안 나와요" | `lotNo`(`LOT_NO`) 누락 |
| "컨베이어가 멈췄어요" | `EQUIPMENT_ERROR` + `errorCode`(`ERROR_CODE`) 확인 |
| "안전 때문에 이동이 안 돼요" | `INTERLOCK_BLOCKED` |
