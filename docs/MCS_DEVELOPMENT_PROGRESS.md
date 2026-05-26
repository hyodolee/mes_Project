# MCS 개발 진행 현황

MCS는 MES와 같은 `MES_DB`를 사용하며, MCS 전용 테이블은 `MCS_` 접두어로 구분합니다. MES 마스터 테이블(`MST_PLANT`, `MST_WAREHOUSE`, `MST_ITEM`, `MST_VENDOR`)과 공통코드(`COM_CODE_GRP`, `COM_CODE`)는 공유합니다.

## 현재 상태 요약

| 구분 | 상태 | 비고 |
|---|---|---|
| 프로젝트 부트스트랩 | 완료 | Spring Boot, MyBatis, Thymeleaf, 공통 응답/예외/페이징 구성 |
| MES 마스터 조회 | 완료 | Plant, Warehouse, Item, Vendor, ComCode 읽기 전용 조회 |
| Zone/Location | 완료 | CRUD, 화면, API, Mapper/XML |
| 로케이션 재고 | 완료 | 재고 조회, 재고 조정, 이력 조회 |
| 입고 워크플로 | 완료 | 입고 오더/품목, 상태 전이, MES 재고 연동 뼈대 |
| 출고 워크플로 | 완료 | 출고 오더/품목, 상태 전이, MES 재고 차감 뼈대 |
| 이동 관리 | 다음 작업 | 이동 오더/품목, 양쪽 로케이션 재고 반영 |
| PLC 이벤트 | 예정 | PowerShell 시뮬레이터 기반 이벤트 수신 |
| AI 운영 기능 | 예정 | 운영 분석, 운영 조회, 이벤트 알림 |

## 완료된 Phase

| Phase | 내용 | 상태 |
|---|---|---|
| 0 | MCS 프로젝트 부트스트랩 | 완료 |
| 1 | MES 마스터 조회, Zone/Location CRUD | 완료 |
| 2 | 로케이션 재고, 재고 조정, 재고 이력 | 완료 |
| 3 | 입고 워크플로 | 완료 |
| 4 | 출고 워크플로 | 완료 |

## 다음 Phase

### Phase 5. 이동 관리

| 작업 | 상태 |
|---|---|
| 이동 오더 CRUD | 예정 |
| 이동 품목 관리 | 예정 |
| 상태 전이: `REQUESTED → IN_PROGRESS → COMPLETED/CANCELLED` | 예정 |
| 이동 완료 시 출발 로케이션 재고 차감 | 예정 |
| 이동 완료 시 도착 로케이션 재고 증가 | 예정 |
| `MCS_LOC_TRANS_HIS`에 `TF_OUT`, `TF_IN` 기록 | 예정 |
| MES `INV_STOCK`, `INV_TRANS_HIS` 동기화 | 예정 |

### Phase 6. PLC 이벤트/인터락 기반

| 작업 | 상태 |
|---|---|
| `MCS_PLC_EVENT_LOG` 설계 | 예정 |
| PLC 이벤트 수신 API | 예정 |
| PowerShell PLC 시뮬레이터 | 예정 |
| 인터락 룰 정의 | 예정 |
| `MCS_INTERLOCK_LOG` 저장 | 예정 |
| `MCS_ALERT_EVENT` 생성 | 예정 |

### Phase 7. AI 운영 기능

| 기능 | 설명 | 상태 |
|---|---|---|
| AI 운영 분석 | 대시보드 자동 브리핑 | 예정 |
| AI 운영 조회 | 자연어 기반 작업/재고/정합성 조회 | 예정 |
| AI 이벤트 알림 | 에러/인터락/지연 발생 시 원인/영향/조치 메시지 생성 | 예정 |

자세한 AI 기능 범위는 [AI_OPERATION_PLAN.md](AI_OPERATION_PLAN.md)를 참고합니다.

## 주요 설계 기준

- DTO는 Java Record를 기본으로 사용합니다.
- API 응답은 `ApiResponse.ok()` / `ApiResponse.fail()`을 사용합니다.
- 페이징은 `SearchDto extends PageRequest`와 `PageResponse.createPagedResponse()` 패턴을 사용합니다.
- 예외는 `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`로 처리합니다.
- 계층 구조는 `interfaces/api`, `interfaces/web`, `application/service`, `infra/persistence/mybatis/mapper`를 유지합니다.
- Mapper는 Interface + XML 조합을 사용하고, XML 위치는 `classpath*:mapper/**/*.xml`입니다.

## 참고 문서

| 문서 | 용도 |
|---|---|
| [../mcs/MCS_설계문서_v2.md](../mcs/MCS_설계문서_v2.md) | MCS DDL, ERD, REST API, MES 연동 로직 |
| [../mcs/MCS_더미데이터_업무흐름.md](../mcs/MCS_더미데이터_업무흐름.md) | 더미데이터 기준 업무 흐름 |
| [AI_OPERATION_PLAN.md](AI_OPERATION_PLAN.md) | PLC/AI/알림 확장 계획 |
