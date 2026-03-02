# MES 백엔드 진행 체크 파일

- 기준일: 2026-02-28
- 프로젝트: `/home/hyodo/workspace/mes_project`
- 사용법: 완료 시 체크박스(`- [x]`)와 증빙을 함께 갱신한다.

## 1. 초기 셋업

- [x] Spring Boot 프로젝트 생성 (`mes_backserver`, Java 21 기준)
- [x] Gradle 의존성 설정 (Web, Thymeleaf, MyBatis, MariaDB, Validation, Test)
- [x] `application-*.yml` 프로파일 분리 (`local/dev/prod`)
- [x] 공통 예외 처리/응답 포맷 구현
- [x] 로컬 기동 확인 (`bootRun` 로그 기준)

증빙:
- 커밋: (미커밋, 워킹트리 작업 상태)
- 실행 명령: `./gradlew -v`, `./gradlew test`, `./gradlew bootRun`
- 결과: Gradle 8.10.2 동작 확인, test 성공(BUILD SUCCESSFUL), Spring Boot 기동 로그에서 Tomcat 8080 및 Actuator endpoint 노출 확인

## 2. DB 및 영속성

- [x] MariaDB 연결 설정 완료
- [x] MyBatis 기본 설정 및 Mapper Scan 완료
- [x] DB 전환 대비 `databaseIdProvider` 또는 동등 전략 적용
- [x] 마이그레이션 도구(Flyway/Liquibase) 초기화
- [ ] `files/mes_ddl_*.sql` 기준 핵심 테이블 매핑 시작

증빙:
- 커밋: (미커밋, 워킹트리 작업 상태)
- 테스트: 애플리케이션 기동 로그, `./gradlew test`
- 결과: `@MapperScan` 및 `mybatis.mapper-locations` 설정 적용됨(현재 Mapper 미구현으로 경고 출력은 정상), `DatabaseIdProvider`로 mariadb/mysql/postgresql/oracle 식별자 매핑 추가, Flyway 활성화 및 baseline 전략 구성 완료, `V3__add_master_indexes.sql` 추가/적용 검증

## 3. 도메인별 구현

### 3.1 기준정보
- [x] `MST_COMPANY` CRUD
- [x] `MST_PLANT` CRUD
- [ ] `MST_ITEM` 조회/검색
- [x] Thymeleaf 화면 + REST API 동시 제공

### 3.2 생산계획/작업지시
- [x] `PLN_PROD_PLAN` 조회/등록
- [ ] `PLN_WORK_ORDER` 생성/상태 변경
- [ ] 작업지시별 자재(`PLN_WO_MATERIAL`) 연계

### 3.3 생산실적
- [ ] `PRD_WORK_RESULT` 등록
- [ ] `PRD_PROCESS_RESULT` 등록
- [ ] `PRD_DEFECT_HIS` 반영

### 3.4 품질
- [ ] `QC_INSPECT_STD` 조회
- [ ] `QC_INSPECT_RESULT` 등록
- [ ] `QC_DEFECT_HIS` 연계

### 3.5 재고
- [ ] `INV_STOCK` 조회
- [ ] `INV_TRANS_HIS` 입출고 반영
- [ ] `INV_LOT` 추적 조회

### 3.6 설비
- [ ] `EQP_OPER_STATUS` 조회
- [ ] `EQP_DOWNTIME` 등록
- [ ] `EQP_MAINT_HIS` 등록/조회

증빙:
- 커밋: (미커밋, 워킹트리 작업 상태)
- API 목록: `/api/v1/master/companies`, `/api/v1/master/companies/{companyCd}`, `/api/v1/master/plants`, `/api/v1/master/plants/{plantCd}`, `/api/v1/planning/prod-plans`, `/api/v1/planning/prod-plans/{planId}`
- 화면 경로: `/master/companies`, `/master/companies/new`, `/master/plants`, `/master/plants/new`, `/planning/prod-plans`, `/planning/prod-plans/new`

## 4. React 전환 대비 항목

- [ ] `interfaces/web`(Thymeleaf)와 `interfaces/api`(JSON) 분리
- [ ] 서비스 계층의 View 비의존성 확인
- [ ] DTO 분리(ViewModel/API)
- [ ] React 연결용 API 명세(OpenAPI) 공개

증빙:
- 문서:
- 확인 결과:

## 5. 품질/운영

- [ ] 단위 테스트 작성
- [ ] Mapper 통합 테스트 작성
- [ ] 기본 보안(인증/권한) 정책 정리
- [ ] 로깅/모니터링(Actuator) 점검
- [ ] 배포 전략(도커/환경변수) 문서화

증빙:
- 테스트 리포트:
- 배포 문서:

## 6. 최종 완료 기준 (Definition of Done)

- [ ] 핵심 업무 흐름: 기준정보 → 작업지시 → 생산실적 → 품질 → 재고가 엔드투엔드 동작
- [ ] MariaDB 기준 운영 가능
- [ ] 설정 변경만으로 DB 전환 가능한 구조 확보
- [ ] Thymeleaf 운영 + React 전환 가능한 API/레이어 분리 완료
- [ ] 인수인계 문서 최신화

최종 메모:
- 마지막 업데이트:
- 담당자/에이전트:
- 리스크:
