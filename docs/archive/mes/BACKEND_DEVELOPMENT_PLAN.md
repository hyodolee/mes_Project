# MES 백엔드 개발 계획서 (Spring Boot + MyBatis + MariaDB)

- 작성일: 2026-02-28
- 대상 저장소: `/home/hyodo/workspace/mes_project`
- 문서 목적: 현재 폴더 기준으로 백엔드 신규 구축 계획을 정의하고, 다른 에이전트/개발자가 바로 이어서 실행 가능하도록 기준을 고정한다.

## 1. 현재 폴더 분석 결과

### 1.1 확인된 자산
- DB 설계/DDL/더미 중심 저장소 (실행 가능한 Spring Boot 소스는 현재 없음)
- 핵심 파일
  - `files/mes_ddl_01_master.sql` ~ `files/mes_ddl_05_equipment.sql`
  - `mes_database_design.md`
  - `DATABASE_SETUP.md`
  - `dummy/*.sql`
  - `mcs/MCS_설계문서_v2.md` (MES 연동 확장 설계 참고)

### 1.2 도메인 범위(기존 SQL 기준)
- 기준정보: `MST_*`, `COM_*`
- 생산: `PLN_*`, `PRD_*`
- 품질: `QC_*`
- 재고: `INV_*`
- 설비: `EQP_*`

## 2. 목표와 제약

- 백엔드 프레임워크: Spring Boot (Java 21, Gradle)
- 데이터 접근: MyBatis (Mapper Interface + XML)
- 기본 DB: MariaDB
- DB 유연성: 설정 변경으로 다른 RDBMS 전환 가능하도록 분리 설계
- 화면: 초기 Thymeleaf 서버 렌더링
- 전환성: 향후 React 전환 시 백엔드 재작업 최소화

## 3. 아키텍처 원칙

1. 도메인/서비스 로직은 View 기술(Thymeleaf/React)에 의존하지 않는다.
2. 컨트롤러를 `web`(Thymeleaf)와 `api`(JSON)로 분리한다.
3. SQL은 MyBatis XML로 관리하고, 서비스는 Mapper 세부 구현을 숨긴다.
4. DB 종속 기능은 `infra/persistence`에 격리한다.
5. 기능은 “기준정보 → 작업지시 → 생산실적 → 품질 → 재고” 순서로 증분 개발한다.

## 4. 권장 프로젝트 구조

```text
mes-backend/
  src/main/java/com/mes/
    global/
      config/
      exception/
      response/
    domain/
      master/
      planning/
      production/
      quality/
      inventory/
      equipment/
    application/
      service/
    interfaces/
      web/        # Thymeleaf Controller
      api/        # REST Controller (React 전환 대비)
    infra/
      persistence/
        mybatis/
          mapper/
          xml/
  src/main/resources/
    templates/
    static/
    db/migration/
    application.yml
    application-local.yml
    application-dev.yml
    application-prod.yml
```

## 5. DB 유연성 설계 (MariaDB 우선)

- `spring.datasource.*`는 프로파일별 외부화
- SQL 방언 차이를 위해 MyBatis `databaseIdProvider` 적용
- 공통 쿼리 + DB별 분기 XML (`*Mapper.xml`, `*Mapper-mariadb.xml` 등) 전략 채택 가능
- 트랜잭션/커넥션풀은 Spring 표준 사용(HikariCP)
- 스키마 변경은 Flyway 또는 Liquibase 도입 권장 (초기에는 Flyway 권장)

## 6. Thymeleaf → React 전환 전략

1. 초기 단계
- `interfaces/web`에서 Thymeleaf 화면 제공
- 동일 서비스 계층을 `interfaces/api`에서도 호출 가능하게 설계

2. 전환 단계
- React가 `interfaces/api` 사용
- Thymeleaf 페이지는 점진 제거
- 인증/권한/비즈니스 로직은 유지 (컨트롤러 레이어만 교체)

3. 규칙
- ViewModel과 API DTO 분리
- 템플릿 전용 로직을 서비스 계층에 넣지 않는다

## 7. 단계별 구현 로드맵

### Phase 0. 부트스트랩
- Spring Boot 프로젝트 생성
- 의존성: Web, Thymeleaf, Validation, MyBatis, MariaDB Driver, Actuator, Test
- 공통 예외/응답 포맷/로그 포맷 구성

### Phase 1. 기준정보(Master)
- 우선 테이블: `MST_COMPANY`, `MST_PLANT`, `MST_ITEM`, `MST_WORKCENTER`
- 기능: 조회/등록/수정/사용여부 변경 (완료: Company, Plant, Item)
- 특징: 
    - `MST_ITEM` 페이징 처리 및 검색 기능 구현 완료
    - 사용여부(Y/N) Select Box 전환 및 UI 최적화 완료
- Thymeleaf 목록/상세 + REST 조회 API 동시 제공

### Phase 2. 작업지시/생산계획
- `PLN_PROD_PLAN`, `PLN_WORK_ORDER`, `PLN_WO_MATERIAL`
- 작업지시 생성/상태전이/조회

### Phase 3. 생산실적
- `PRD_WORK_RESULT`, `PRD_PROCESS_RESULT`, `PRD_DEFECT_HIS`
- 실적 등록과 작업지시 연동

### Phase 4. 품질관리
- `QC_INSPECT_STD`, `QC_INSPECT_RESULT`, `QC_DEFECT_HIS`
- 검사결과 등록 및 불량 연계

### Phase 5. 재고관리
- `INV_STOCK`, `INV_TRANS_HIS`, `INV_LOT`
- 입출고 반영, LOT 추적

### Phase 6. 설비관리 + 운영화
- `EQP_OPER_STATUS`, `EQP_DOWNTIME`, `EQP_MAINT_HIS`
- 모니터링/배포 파이프라인/문서 고도화

## 8. 공통 개발 규칙

- 네이밍/코드 규칙은 기존 SQL 문서 규칙(`*_CD`, `*_DTM`, `USE_YN`)을 준수
- DTO, Entity(또는 VO), Mapper 파라미터를 명확히 분리
- 서비스 계층에서 트랜잭션 경계를 관리
- 모든 신규 기능은 최소 단위 테스트 + Mapper 통합 테스트 포함
- Thymeleaf 화면의 사용자 노출 문구(메뉴/버튼/컬럼/메시지)는 기본적으로 한국어를 사용한다.
- 사용자 메시지 표준: 등록 성공(`~가 등록되었습니다.`), 수정 성공(`~가 수정되었습니다.`), 삭제 성공(`~가 삭제되었습니다.`)
- 용어 표준 예시: Company→회사, Plant→공장, Production Plan→생산계획, Search→조회, Create→등록, Edit→수정, Save→저장, Cancel→취소
- 신규 화면 개발 시 체크: 페이지 `title`, 상단 타이틀(`topbar`), 검색 조건 라벨, 버튼, 테이블 헤더, 플래시 메시지까지 동일 용어로 일관 적용
- API 에러 응답 문구도 한국어 기준으로 관리한다. (`ApiResponse.fail.message`, Validation `FieldError.defaultMessage`)
- API 에러 메시지 작성 규칙:
  - 입력값 오류: `~는 필수입니다.`, `~는 N자 이하여야 합니다.`, `~는 Y 또는 N만 입력할 수 있습니다.`
  - 리소스 없음: `~를 찾을 수 없습니다.`
  - 처리 실패: `~에 실패했습니다.`
  - 경로/본문 불일치: `경로의 {키}와 요청 본문의 {키}가 일치하지 않습니다.`
- `ErrorCode` 기본 메시지와 `BusinessException` 커스텀 메시지는 동일 톤(존댓말, 마침표 포함)으로 유지한다.

## 9. 인수인계(다른 에이전트용) 빠른 시작

1. 우선 읽기 파일
- `docs/BACKEND_DEVELOPMENT_PLAN.md`
- `docs/BACKEND_PROGRESS_CHECK.md`
- `mes_database_design.md`
- `files/mes_ddl_*.sql`

2. 첫 구현 태스크
- Phase 0 부트스트랩 완료
- Phase 1의 `MST_COMPANY`, `MST_PLANT` CRUD부터 시작

3. 완료 판정 기준
- 로컬에서 앱 기동
- DB 연결 성공
- Thymeleaf 페이지 + REST API 동시 동작
- 체크 파일의 해당 항목 증빙(커밋/테스트/스크린샷) 기록
