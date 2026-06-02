# React Frontend Migration Plan

이 문서는 MES/MCS 프로젝트의 Thymeleaf 화면을 React 프론트엔드로 전환하기 위한 기준 문서입니다.
다른 AI 에이전트나 개발자가 이어서 작업할 때 이 문서를 먼저 확인합니다.

## 목표

기존 구조를 아래 구조로 전환합니다.

```text
Spring Boot MES/MCS = API 서버
React Frontend      = 통합 운영 화면
```

React 화면은 MES/MCS/PLC/AI 기능을 하나의 운영 대시보드에서 다루는 포트폴리오용 통합 프론트엔드입니다.

## 선택한 템플릿

| 항목 | 선택 |
|---|---|
| 템플릿 | Mantis Free MUI React Admin Template |
| UI 라이브러리 | MUI |
| 라우팅 | React Router |
| API 상태 관리 | TanStack Query |
| HTTP 클라이언트 | Axios |
| 언어 | TypeScript 권장 |

참고:

- https://codedthemes.com/item/mantis-free-mui-admin-template/
- https://github.com/codedthemes/mantis-free-react-admin-template

## 선택 이유

- MUI 기반이라 현업 React 관리자 화면 느낌이 좋습니다.
- CoreUI보다 더 깔끔하고 현대적인 디자인입니다.
- Argon/Horizon보다 운영 시스템에 덜 과하고 업무 화면에 적합합니다.
- 테이블, 카드, 상태 배지, 차트, 사이드바를 활용하기 좋습니다.
- MES/MCS처럼 데이터가 많은 운영 화면을 포트폴리오용으로 보기 좋게 만들 수 있습니다.

## 목표 구조

```text
mes_project/
├─ mes_backserver/      # MES API 서버, port 8080
├─ mcs_backserver/      # MCS API 서버, port 8081
├─ mes_frontend/        # React + Mantis 프론트엔드
├─ scripts/plc/         # PLC 시뮬레이터
└─ docs/
```

React 프론트는 MES/MCS API를 모두 호출합니다.

```text
React Frontend
├─ MES API 호출
├─ MCS API 호출
├─ PLC 이벤트 API 호출
└─ AI 분석 API 호출 예정
```

## 메뉴 구조

```text
Dashboard
├─ 오늘 이동 오더 현황
├─ 최근 PLC 이벤트
├─ 재고 부족/인터락 요약
└─ 설비 오류 요약

MCS
├─ 이동 관리
├─ 로케이션 재고
├─ 재고 이력
└─ PLC 이벤트 로그

MES
├─ 작업지시
├─ 품목
├─ 창고
└─ 공장

AI
├─ 운영 분석
├─ 자연어 조회
└─ 이벤트 알림 요약
```

## 1차 개발 범위

처음부터 MES 전체를 React로 옮기지 않습니다.
1차 목표는 현재 구현한 MCS 이동/PLC 시뮬레이션 흐름을 React에서 시연 가능하게 만드는 것입니다.

```text
React에서 이동 오더 생성
→ 품목 추가
→ PowerShell PLC 시뮬레이터 실행
→ React에서 PLC 이벤트 로그 확인
→ React에서 이동 상태 COMPLETED 확인
→ React에서 로케이션 재고 변경 확인
```

1차 화면:

| 화면 | 목적 |
|---|---|
| Dashboard | 이동/PLC/재고 상태 요약 |
| MCS 이동관리 목록 | 이동 오더 조회 |
| MCS 이동관리 상세 | 기본정보, 품목, 이동 처리 이력 확인 |
| MCS 이동등록 | 출발/도착 로케이션 선택 |
| 로케이션 재고 | 이동 전후 재고 확인 |
| 재고 이력 | `TF_OUT`, `TF_IN` 확인 |
| PLC 이벤트 로그 | 스크립트가 보낸 이벤트 확인 |

## 필요한 API

React 전환 전 또는 병행해서 아래 API를 정리해야 합니다.

### 이동 관리

```text
GET    /api/transfers
GET    /api/transfers/{id}
POST   /api/transfers
PUT    /api/transfers/{id}
DELETE /api/transfers/{id}

GET    /api/transfers/{id}/items
POST   /api/transfers/{id}/items
DELETE /api/transfers/{id}/items/{itemId}

POST   /api/transfers/{id}/status
GET    /api/transfers/{id}/histories
```

### 재고

```text
GET /api/inventory/stocks
GET /api/inventory/transactions
```

### PLC 이벤트

```text
POST /api/plc/events
GET  /api/plc/events
GET  /api/plc/events/{eventId}
```

현재 구현 상태:

```text
POST /api/plc/events 구현됨
GET /api/plc/events 미구현
이동관리 REST API는 추가 정리 필요
```

## 프론트 폴더 구조 제안

```text
mes_frontend/
├─ src/
│  ├─ api/
│  │  ├─ client.ts
│  │  ├─ mcs/
│  │  │  ├─ transfers.ts
│  │  │  ├─ inventory.ts
│  │  │  └─ plcEvents.ts
│  │  └─ mes/
│  ├─ layouts/
│  ├─ routes/
│  ├─ pages/
│  │  ├─ dashboard/
│  │  ├─ mcs/
│  │  │  ├─ transfers/
│  │  │  ├─ inventory/
│  │  │  └─ plc-events/
│  │  ├─ mes/
│  │  └─ ai/
│  ├─ components/
│  ├─ hooks/
│  └─ types/
```

## 작업 순서

### Phase 1. React 기반 구성

```text
1. mes_frontend 폴더 생성
2. Mantis Free MUI React Admin Template 적용
3. 기본 샘플 페이지 정리
4. MES/MCS 메뉴 구조 반영
5. dev 서버 실행 확인
```

### Phase 2. API 클라이언트 구성

```text
1. Axios 인스턴스 생성
2. MCS baseURL 설정
3. MES baseURL 설정
4. TanStack Query 설정
5. 공통 에러 처리 방식 정의
```

### Phase 3. MCS 이동관리 API 보강

```text
1. 이동 오더 목록 API
2. 이동 오더 상세 API
3. 이동 오더 생성 API
4. 이동 품목 추가/삭제 API
5. 이동 상태 변경 API
6. 이동 처리 이력 조회 API
```

### Phase 4. MCS 이동관리 React 화면

```text
1. 이동 목록 화면
2. 이동 등록 화면
3. 이동 상세 화면
4. 품목 추가/삭제
5. 이동 시작/완료 버튼
6. 이동 처리 이력 표시
```

### Phase 5. PLC 이벤트 로그 화면

```text
1. GET /api/plc/events 구현
2. PLC 이벤트 로그 목록 화면
3. eventType / eventStatus 배지 표시
4. targetId 기준 이동 오더 연결
5. 오류/인터락 이벤트 강조
```

### Phase 6. 재고 화면

```text
1. 로케이션 재고 목록
2. 재고 이력 목록
3. 품목/로케이션/LOT 검색
4. 이동 완료 후 재고 변경 확인
```

### Phase 7. 대시보드

```text
1. 오늘 이동 오더 수
2. 진행 중 이동 오더 수
3. 최근 PLC 이벤트
4. 오류/인터락 이벤트 수
5. 재고 부족 또는 지연 요약
```

## 시연 목표

React 전환 1차 완료 후 아래 시연이 가능해야 합니다.

```text
1. React에서 이동 오더 생성
2. 이동 품목 추가
3. PowerShell PLC 시뮬레이터 실행
4. React PLC 이벤트 로그에서 TRANSFER_STARTED / TRANSFER_COMPLETED 확인
5. React 이동관리에서 상태 COMPLETED 확인
6. React 로케이션 재고에서 출발지 감소 / 도착지 증가 확인
7. React 이동 처리 이력에서 TF_OUT / TF_IN 확인
```

## 주의 사항

- Thymeleaf 화면을 바로 삭제하지 않습니다.
- React 화면이 안정화될 때까지 Thymeleaf는 백업/비교용으로 유지합니다.
- React가 필요한 API를 먼저 정의하고, 화면은 API 기준으로 개발합니다.
- PLC 시뮬레이터는 `PlcApi` 모드를 기준으로 사용합니다.
- `DirectMcs` 모드는 임시 테스트용이며 최종 시연에서는 사용하지 않습니다.
- MES/MCS 백엔드는 같은 `MES_DB`를 사용합니다.
- MCS 테이블은 `MCS_` 접두어를 유지합니다.

## 다음 작업

```text
1. mes_frontend 생성
2. Mantis 템플릿 적용
3. React dev 서버 실행 확인
4. MCS 이동관리 REST API 보강
5. React MCS 이동관리 목록 화면 구현
```
