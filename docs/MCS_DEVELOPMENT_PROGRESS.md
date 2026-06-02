# MCS 개발 진행 현황

MCS는 MES와 같은 `MES_DB`를 사용하며, MCS 전용 테이블은 `MCS_` 접두어로 구분합니다. MES 마스터 테이블(`MST_PLANT`, `MST_WAREHOUSE`, `MST_ITEM`, `MST_VENDOR`)과 공통코드(`COM_CODE_GRP`, `COM_CODE`)를 공유합니다.

## 현재 상태 요약

| 구분 | 상태 | 비고 |
|---|---|---|
| 프로젝트 부트스트랩 | 완료 | Spring Boot, MyBatis, 공통 응답/예외/페이징 구성 |
| MES 마스터 조회 | 완료 | Plant, Warehouse, Item, Vendor, ComCode 조회 |
| Zone/Location | 완료 | CRUD, 화면, API, Mapper/XML |
| 로케이션 재고 | 완료 | 재고 조회, 재고 조정, 재고 이력 |
| 입고 워크플로 | 완료 | 입고오더/품목, 상태 전이, 재고 반영 |
| 출고 워크플로 | 완료 | 출고오더/품목, 상태 전이, 재고 차감 |
| 이동 관리 | 완료 | 이동오더/품목, 시작/완료/취소/실패 처리 |
| 경로 관리 | 완료 | 노드/구간/상태 관리 |
| 경로 최적화 | 완료 | 최단 시간, 혼잡 회피, 막힘 구간 제외 |
| PLC 이벤트 | 완료 | PowerShell 시뮬레이터와 이벤트 수신 API |
| MES 자재 요청 연동 | 완료 | MES 작업오더에서 MCS 이동오더 생성 |
| React 화면 | 완료 | MCS 주요 화면 React 구현 |
| AI 운영 기능 | 예정 | 운영 분석, 자연어 조회, 이벤트 알림 |

## 완료된 Phase

| Phase | 내용 | 상태 |
|---|---|---|
| 0 | MCS 프로젝트 부트스트랩 | 완료 |
| 1 | MES 마스터 조회, Zone/Location CRUD | 완료 |
| 2 | 로케이션 재고, 재고 조정, 재고 이력 | 완료 |
| 3 | 입고 워크플로 | 완료 |
| 4 | 출고 워크플로 | 완료 |
| 5 | 이동 관리 | 완료 |
| 6 | 경로 관리/경로 최적화 | 완료 |
| 7 | PLC 이벤트 시뮬레이터 | 완료 |
| 8 | MES 작업오더 자재 요청 연동 | 완료 |

## 핵심 업무 흐름

```text
MES 작업오더 생성
-> MES 자재 요청
-> MCS 이동오더 생성
-> MCS 재고 LOT/Location/경로 배정
-> MCS 이동 시작
-> PLC 성공/실패 이벤트
-> 성공이면 MCS 이동 완료 및 MES 작업 시작 허용
-> 실패이면 MCS 이동 실패 및 MES 작업 시작 차단
```

## 다음 Phase

### Phase 9. AI 운영 분석

| 작업 | 상태 |
|---|---|
| 운영 스냅샷 조회 API | 예정 |
| Spring AI 분석 서비스 | 예정 |
| 작업오더/MCS 이동/PLC 이벤트 기반 문제 요약 | 예정 |
| React AI 분석 패널 | 예정 |

### Phase 10. 자연어 운영 조회

| 작업 | 상태 |
|---|---|
| 사용자 질문 API | 예정 |
| 질문 의도 분류 | 예정 |
| MES/MCS 조회 결과 요약 | 예정 |
| React 챗봇형 조회 화면 | 예정 |

### Phase 11. AI 이벤트 알림

| 작업 | 상태 |
|---|---|
| 이벤트 알림 저장 구조 | 예정 |
| AI 알림 문장 생성 | 예정 |
| React 알림 목록 | 예정 |
| 이메일/문자 연동 | 후순위 |

## 주요 설계 기준

- DTO는 Java Record를 기본으로 사용합니다.
- API 응답은 `ApiResponse.ok()` / `ApiResponse.fail()` 패턴을 사용합니다.
- 페이징은 `SearchDto extends PageRequest`와 `PageResponse.createPagedResponse()` 패턴을 사용합니다.
- 예외는 `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`로 처리합니다.
- 계층 구조는 `interfaces/api`, `interfaces/web`, `application/service`, `infra/persistence/mybatis/mapper`를 유지합니다.
- Mapper는 Interface + XML 조합을 사용하고, XML 위치는 `classpath*:mapper/**/*.xml`입니다.

## 참고 문서

| 문서 | 용도 |
|---|---|
| [../mcs/MCS_설계문서_v2.md](../mcs/MCS_설계문서_v2.md) | MCS DDL, ERD, REST API, MES 연동 로직 |
| [../mcs/MCS_더미데이터_업무흐름.md](../mcs/MCS_더미데이터_업무흐름.md) | 더미데이터 기준 업무 흐름 |
| [design/AI_OPERATION_PLAN.md](design/AI_OPERATION_PLAN.md) | PLC/AI/알림 확장 계획 |
| [runbooks/DEMO_SCENARIOS.md](runbooks/DEMO_SCENARIOS.md) | 시연 시나리오 |
