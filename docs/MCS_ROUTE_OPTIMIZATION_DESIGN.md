# MCS Route Optimization Design

## 1. 목적

현재 MCS는 로케이션 재고, 입고, 출고, 자재 이동 오더를 관리한다. 여기에 MCS다운 핵심 기능인 **경로 최적화**를 추가한다.

목표는 사용자가 출발/도착 로케이션을 직접 정하는 단순 이동에서 끝나지 않고, MCS가 설비/구간 상태를 고려해 이동 경로를 계산하고 이동 오더에 반영하는 구조를 만드는 것이다.

```text
현재 구조
사용자 이동 등록 -> 이동 시작 -> 이동 완료 -> 재고 반영

확장 구조
이동 요청 -> 경로 후보 계산 -> 최적 경로 선택 -> 이동 오더 생성/시작
        -> PLC 이벤트 수신 -> 구간 상태 변경/인터락 판단
        -> 필요 시 경로 재계산 -> 이동 완료 -> 재고 반영
```

## 2. 설계 범위

### 이번 MVP에 포함

| 구분 | 내용 |
|---|---|
| 경로 마스터 | 로케이션 또는 경유 노드를 연결한 경로 그래프 관리 |
| 구간 상태 | 사용 가능, 혼잡, 정지, 인터락, 점검 상태 관리 |
| 경로 계산 | 출발 노드에서 도착 노드까지 최단/최소 비용 경로 계산 |
| 이동 오더 연동 | 이동 오더 생성 시 선택된 경로를 저장 |
| PLC 이벤트 연동 | 특정 구간 막힘/복구 이벤트를 받아 구간 상태 변경 |
| 시연 시나리오 | 정상 이동, 구간 막힘, 우회 경로 재계산, 경로 없음 |
| React 화면 | 경로 관리, 경로 최적화, 이동 오더 경로 표시 |

### 이번 MVP에서 제외

| 제외 항목 | 이유 |
|---|---|
| 실제 AGV/컨베이어 제어 | 포트폴리오 범위에서는 시뮬레이터로 대체 |
| 다중 차량 배차 최적화 | 난이도가 높아 2차 확장으로 분리 |
| 실시간 좌표 기반 이동 | 현재 시스템은 Location 단위 업무 모델이므로 노드/구간 단위로 시작 |
| 복잡한 AI 경로 예측 | 먼저 규칙 기반 최적화를 만든 뒤 AI 분석으로 확장 |

## 3. 핵심 개념

### Node

경로 계산의 지점이다. 실제 `MCS_LOCATION`과 연결될 수도 있고, 컨베이어 분기점, 버퍼, 리프트, 게이트 같은 물리적 경유 지점일 수도 있다.

예:

```text
NCM-01-01 파렛트1
NCM-01-02 파렛트2
CV-01 컨베이어 입구
CV-02 컨베이어 분기점
LIFT-01 리프트
```

### Edge

노드와 노드를 잇는 이동 가능 구간이다. 거리, 예상 시간, 우선순위, 혼잡도, 상태를 가진다.

예:

```text
NCM-01-01 -> CV-01
CV-01 -> CV-02
CV-02 -> NCM-01-02
CV-02 -> LIFT-01
```

### Cost

경로 계산에서 사용하는 비용이다. 기본은 이동 시간 또는 거리이며, 상태에 따라 가중치를 더한다.

```text
기본 비용 = TRAVEL_TIME_SEC
혼잡 구간 = 기본 비용 + CONGESTION_COST
점검/정지/인터락 구간 = 경로 후보에서 제외
```

## 4. 기존 기능과의 관계

경로 최적화는 기존 이동 관리를 대체하지 않는다. 기존 `MCS_TRANSFER_ORDER`는 **이동 실행 오더**로 유지하고, 경로 최적화는 그 앞단에서 “어떤 경로로 이동할지”를 결정한다.

```text
MCS_TRANSFER_ORDER
- 무엇을 어디서 어디로 옮길지
- 상태: REQUESTED, IN_PROGRESS, COMPLETED, CANCELLED
- 재고 반영 기준

MCS_TRANSFER_ROUTE
- 해당 이동 오더가 어떤 경로를 선택했는지
- 경로 단계별 진행 상태
- PLC 이벤트와 재계산 이력 기준
```

## 5. DB 설계

### 5.1 MCS_ROUTE_NODE

경로 계산용 노드 마스터.

| 컬럼 | 설명 |
|---|---|
| ROUTE_NODE_ID | PK |
| PLANT_CD | 공장 코드 |
| NODE_CD | 노드 코드 |
| NODE_NM | 노드명 |
| NODE_TYPE | LOCATION, CONVEYOR, BUFFER, LIFT, GATE |
| LOCATION_ID | 로케이션 노드일 때 `MCS_LOCATION.LOCATION_ID` |
| USE_YN | 사용 여부 |
| REG_USER_ID, REG_DTM, UPD_USER_ID, UPD_DTM | 감사 컬럼 |

### 5.2 MCS_ROUTE_EDGE

노드 간 이동 가능 구간.

| 컬럼 | 설명 |
|---|---|
| ROUTE_EDGE_ID | PK |
| PLANT_CD | 공장 코드 |
| EDGE_CD | 구간 코드 |
| EDGE_NM | 구간명 |
| FROM_NODE_ID | 출발 노드 |
| TO_NODE_ID | 도착 노드 |
| BIDIRECTIONAL_YN | 양방향 사용 여부 |
| DISTANCE_M | 거리 |
| TRAVEL_TIME_SEC | 예상 이동 시간 |
| BASE_COST | 기본 비용 |
| EDGE_STATUS | AVAILABLE, CONGESTED, BLOCKED, INTERLOCKED, MAINTENANCE |
| USE_YN | 사용 여부 |
| REG_USER_ID, REG_DTM, UPD_USER_ID, UPD_DTM | 감사 컬럼 |

### 5.3 MCS_ROUTE_EDGE_STATUS_HIS

구간 상태 변경 이력.

| 컬럼 | 설명 |
|---|---|
| STATUS_HIS_ID | PK |
| ROUTE_EDGE_ID | 구간 ID |
| PREV_STATUS | 이전 상태 |
| NEW_STATUS | 변경 상태 |
| REASON_TYPE | PLC, USER, SYSTEM |
| REASON_TEXT | 변경 사유 |
| REF_EVENT_ID | PLC 이벤트 ID |
| REG_USER_ID, REG_DTM | 등록 정보 |

### 5.4 MCS_TRANSFER_ROUTE

이동 오더별 선택된 경로 헤더.

| 컬럼 | 설명 |
|---|---|
| TRANSFER_ROUTE_ID | PK |
| TRANSFER_ID | `MCS_TRANSFER_ORDER.TRANSFER_ID` |
| ROUTE_STATUS | PLANNED, ACTIVE, COMPLETED, REPLANNED, FAILED |
| TOTAL_DISTANCE_M | 총 거리 |
| TOTAL_TIME_SEC | 총 예상 시간 |
| TOTAL_COST | 총 비용 |
| OPTIMIZE_RULE | SHORTEST_TIME, SHORTEST_DISTANCE, AVOID_CONGESTION |
| REPLAN_COUNT | 재계산 횟수 |
| REG_USER_ID, REG_DTM, UPD_USER_ID, UPD_DTM | 감사 컬럼 |

### 5.5 MCS_TRANSFER_ROUTE_STEP

이동 오더별 경로 상세 단계.

| 컬럼 | 설명 |
|---|---|
| ROUTE_STEP_ID | PK |
| TRANSFER_ROUTE_ID | 경로 헤더 ID |
| STEP_SEQ | 경로 순번 |
| ROUTE_EDGE_ID | 이동 구간 ID |
| FROM_NODE_ID | 출발 노드 |
| TO_NODE_ID | 도착 노드 |
| STEP_STATUS | WAITING, RUNNING, PASSED, BLOCKED, SKIPPED |
| EXPECTED_TIME_SEC | 예상 시간 |
| START_DTM | 단계 시작 시간 |
| END_DTM | 단계 종료 시간 |

## 6. 공통 코드

| 그룹 | 코드 |
|---|---|
| MCS_NODE_TYPE | LOCATION, CONVEYOR, BUFFER, LIFT, GATE |
| MCS_EDGE_STATUS | AVAILABLE, CONGESTED, BLOCKED, INTERLOCKED, MAINTENANCE |
| MCS_ROUTE_STATUS | PLANNED, ACTIVE, COMPLETED, REPLANNED, FAILED |
| MCS_ROUTE_STEP_STATUS | WAITING, RUNNING, PASSED, BLOCKED, SKIPPED |
| MCS_OPTIMIZE_RULE | SHORTEST_TIME, SHORTEST_DISTANCE, AVOID_CONGESTION |

## 7. 경로 계산 방식

### 7.1 기본 알고리즘

MVP에서는 Dijkstra 알고리즘을 사용한다.

이유:

| 항목 | 판단 |
|---|---|
| 구현 난이도 | 낮음 |
| 설명 가능성 | 높음 |
| 포트폴리오 설득력 | 충분함 |
| 가중치 반영 | 가능 |
| 실시간 상태 제외 | 가능 |

### 7.2 비용 계산 규칙

```text
AVAILABLE:
  cost = BASE_COST

CONGESTED:
  cost = BASE_COST + 100

BLOCKED / INTERLOCKED / MAINTENANCE:
  후보 경로에서 제외
```

최적화 옵션별 기준:

| 옵션 | 기준 |
|---|---|
| SHORTEST_TIME | TRAVEL_TIME_SEC 중심 |
| SHORTEST_DISTANCE | DISTANCE_M 중심 |
| AVOID_CONGESTION | 혼잡 구간 페널티를 크게 적용 |

## 8. API 설계

### 8.1 경로 마스터

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/mcs/route-nodes` | 노드 목록 조회 |
| POST | `/api/mcs/route-nodes` | 노드 등록 |
| PUT | `/api/mcs/route-nodes/{routeNodeId}` | 노드 수정 |
| DELETE | `/api/mcs/route-nodes/{routeNodeId}` | 노드 삭제 |
| GET | `/api/mcs/route-edges` | 구간 목록 조회 |
| POST | `/api/mcs/route-edges` | 구간 등록 |
| PUT | `/api/mcs/route-edges/{routeEdgeId}` | 구간 수정 |
| PATCH | `/api/mcs/route-edges/{routeEdgeId}/status` | 구간 상태 변경 |

### 8.2 경로 최적화

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/mcs/routes/optimize` | 출발/도착 기준 최적 경로 계산 |
| POST | `/api/mcs/transfers/{transferId}/routes` | 이동 오더에 경로 저장 |
| GET | `/api/mcs/transfers/{transferId}/routes` | 이동 오더 경로 조회 |
| POST | `/api/mcs/transfers/{transferId}/routes/replan` | 이동 중 경로 재계산 |

### 8.3 요청/응답 예시

요청:

```json
{
  "plantCd": "P001",
  "fromLocationId": 101,
  "toLocationId": 102,
  "optimizeRule": "SHORTEST_TIME"
}
```

응답:

```json
{
  "routeAvailable": true,
  "totalDistanceM": 42.5,
  "totalTimeSec": 95,
  "totalCost": 95,
  "steps": [
    {
      "stepSeq": 1,
      "edgeCd": "E-NCM01-CV01",
      "fromNodeCd": "NCM-01-01",
      "toNodeCd": "CV-01",
      "expectedTimeSec": 20
    }
  ]
}
```

## 9. PLC 이벤트 연동

기존 `MCS_PLC_EVENT_LOG`는 유지한다. 이벤트 타입만 경로 최적화 시나리오에 맞게 확장한다.

| 이벤트 타입 | 처리 |
|---|---|
| EDGE_BLOCKED | 특정 구간을 BLOCKED로 변경 |
| EDGE_RELEASED | 특정 구간을 AVAILABLE로 변경 |
| EDGE_CONGESTED | 특정 구간을 CONGESTED로 변경 |
| EDGE_INTERLOCKED | 특정 구간을 INTERLOCKED로 변경 |
| TRANSFER_STEP_STARTED | 현재 경로 step RUNNING 처리 |
| TRANSFER_STEP_PASSED | 현재 경로 step PASSED 처리 |

이동 중 현재 경로의 남은 step 중 하나가 `BLOCKED` 또는 `INTERLOCKED`가 되면 MCS는 재계산 후보를 만들 수 있다.

```text
PLC EDGE_BLOCKED 수신
-> MCS_ROUTE_EDGE.EDGE_STATUS = BLOCKED
-> 영향받는 ACTIVE 경로 조회
-> 남은 경로에 막힌 구간이 있으면 REPLAN 필요 표시
-> 사용자가 재계산 버튼 클릭 또는 자동 재계산
```

## 10. React 화면 설계

### 10.1 MCS 메뉴 추가

```text
MCS
  - MCS 대시보드
  - Zone 관리
  - Location 관리
  - 입고 관리
  - 출고 관리
  - 이동 관리
  - 경로 관리
  - 경로 최적화
  - 로케이션 재고
  - 재고 이력
  - PLC 이벤트
```

### 10.2 경로 관리 화면

목적: 노드와 구간을 관리한다.

구성:

| 영역 | 내용 |
|---|---|
| 검색 조건 | 공장, 노드/구간 코드, 상태 |
| 노드 목록 | NODE_CD, NODE_NM, NODE_TYPE, LOCATION |
| 구간 목록 | EDGE_CD, FROM_NODE, TO_NODE, STATUS, COST |
| 상태 변경 | 사용 가능, 혼잡, 막힘, 인터락, 점검 |

### 10.3 경로 최적화 화면

목적: 출발/도착 로케이션을 선택해 경로 후보를 계산한다.

구성:

| 영역 | 내용 |
|---|---|
| 조건 | 공장, 출발 Location, 도착 Location, 최적화 기준 |
| 결과 요약 | 총 거리, 총 시간, 총 비용, 경로 가능 여부 |
| 단계 목록 | step, from, to, 구간 상태, 예상 시간 |
| 액션 | 이동 오더 생성, 기존 이동 오더에 경로 적용 |

### 10.4 이동 관리 화면 확장

기존 이동 관리에 아래 정보를 추가한다.

| 위치 | 추가 정보 |
|---|---|
| 목록 | 경로 상태, 재계산 필요 여부 |
| 상세 | 선택 경로 step 목록 |
| 액션 | 경로 계산, 경로 재계산, 경로 보기 |

## 11. 시연 시나리오

### 시나리오 1. 정상 최적 경로 생성

```text
1. 출발 NCM-01-01, 도착 NCM-01-02 선택
2. 경로 최적화 실행
3. AVAILABLE 구간만 사용해 최단 경로 표시
4. 이동 오더 생성
5. 이동 시작/완료
6. 재고 반영 확인
```

### 시나리오 2. 구간 막힘 후 우회 경로

```text
1. 기본 최단 경로가 A-B-C라고 가정
2. PLC 시뮬레이터가 B-C 구간 EDGE_BLOCKED 전송
3. 구간 상태가 BLOCKED로 변경
4. 경로 최적화 재실행
5. A-D-C 우회 경로 표시
```

### 시나리오 3. 이동 중 인터락 발생

```text
1. 이동 오더 시작
2. 현재 선택 경로의 다음 구간에 EDGE_INTERLOCKED 발생
3. 이동 오더 화면에 재계산 필요 표시
4. 재계산 버튼 클릭
5. 새 경로 저장, 기존 남은 step은 SKIPPED 처리
```

### 시나리오 4. 경로 없음

```text
1. 모든 연결 구간을 BLOCKED 또는 MAINTENANCE 처리
2. 경로 최적화 실행
3. routeAvailable=false 반환
4. 화면에 이동 불가 사유 표시
```

## 12. 구현 순서

| 순서 | 작업 | 결과 |
|---|---|---|
| 1 | DDL 추가 | route node/edge/transfer route 테이블 생성 |
| 2 | 더미 경로 데이터 추가 | 시연 가능한 그래프 구성 |
| 3 | 백엔드 Route 도메인 추가 | DTO, Mapper, Service, API |
| 4 | Dijkstra 경로 계산 구현 | 최적 경로 응답 생성 |
| 5 | Transfer 연동 | 이동 오더에 경로 저장/조회 |
| 6 | PLC 이벤트 확장 | edge 상태 변경, 재계산 필요 판단 |
| 7 | React 경로 관리 화면 | 노드/구간 관리 |
| 8 | React 경로 최적화 화면 | 경로 계산/적용 |
| 9 | 이동 관리 화면 확장 | 선택 경로 표시 |
| 10 | PowerShell 시뮬레이터 확장 | edge blocked/released 시나리오 |

## 13. 구현 시 주의점

- 기존 이동 완료 시 재고 반영 로직은 유지한다.
- 경로 최적화는 이동 경로를 결정할 뿐, 재고 차감/증가는 기존 `TransferService` 완료 처리에서 수행한다.
- 구간 상태가 막혔다고 이미 완료된 이동 이력을 되돌리지 않는다.
- 이동 중 재계산은 남은 step에 대해서만 수행한다.
- 같은 Location을 출발/도착으로 선택하는 이동은 기존처럼 막는다.
- `MCS_LOCATION.CURRENT_USAGE`는 경로 계산과 직접 연결하지 않는다. 재고량은 재고 도메인의 책임이다.

## 14. 포트폴리오 설명 문장

면접이나 README에서는 아래처럼 설명할 수 있다.

```text
MCS는 단순히 자재 이동 오더만 등록하는 구조가 아니라,
Location과 설비 구간을 그래프로 모델링하고 구간 상태, 혼잡도, 인터락 정보를 반영해
최적 이동 경로를 계산하도록 설계했습니다.

PLC 이벤트 시뮬레이터를 통해 특정 구간의 막힘이나 복구를 발생시키고,
MCS는 해당 이벤트를 반영해 경로 재계산 또는 운영자 알림으로 이어지도록 확장할 수 있습니다.
```

