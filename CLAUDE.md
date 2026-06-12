# MES/MCS 프로젝트 개발 가이드

## 프로젝트 구조
- `mes_backserver/` : MES (포트 8080, 패키지 com.mes)
- `mcs_backserver/` : MCS (포트 8081, 패키지 com.mcs) ← 현재 개발 중
- **동일 DB 운영**: MES/MCS 모두 `MES_DB` 사용, MCS 테이블은 `MCS_` 접두어로 구분
- MCS 테이블에서 MES 마스터 테이블 FK 참조 (MST_PLANT, MST_WAREHOUSE, MST_ITEM, MST_VENDOR)
- COM_CODE_GRP/COM_CODE는 MES 기존 테이블 공유, MCS 전용 그룹은 `MCS_` 접두어

## 기술 스택
Windows 10, Java 21, Spring Boot 3.3.5, MyBatis 3.0.3, MariaDB, Thymeleaf, Gradle

## 핵심 패턴 (MES/MCS 공통)
- DTO: Lombok class (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor`), API응답: `ApiResponse.ok()/fail()`
  - getter는 `getXxx()` / boolean primitive는 `isXxx()` / 검증 어노테이션은 필드에 부착
  - MyBatis 매핑: `resultType` + 컬럼 별칭(`AS camelCase`) 방식 (setter 주입), `<resultMap>` 불필요
  - mes_backserver는 MES 전 DTO를 Lombok class로 통일함 (record 사용 안 함)
- 페이징: `SearchDto extends PageRequest` → `PageResponse.createPagedResponse()`
- 예외: `ErrorCode enum` + `BusinessException` + `GlobalExceptionHandler`
- 계층: `interfaces/api`(REST) + `interfaces/web`(Thymeleaf) + `application/service` + `infra/persistence/mybatis/mapper`
- Mapper: Interface(@Mapper) + XML, `classpath*:mapper/**/*.xml`

## 개발 진행 문서
- **반드시 참조**: `docs/MCS_DEVELOPMENT_PROGRESS.md` (MCS 개발 진행 상황)
- 설계 문서: `mcs/MCS_설계문서_v2.md` (DDL, ERD, API 명세)
- MCS SQL: `mcs/MCS_install.sql` (테이블+공통코드+더미데이터)

## 빌드 명령
```
powershell.exe -Command "Set-Location 'C:\dev\mes_project\mcs_backserver'; & .\gradlew.bat build -x test 2>&1"
```
