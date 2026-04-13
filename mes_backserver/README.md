# MES 백엔드 프로젝트 (mes_backserver)

Spring Boot + MyBatis + MariaDB + Thymeleaf 기반의 제조실행시스템(MES) 백엔드 프로젝트입니다.

## 개발 환경 (Environment)

- **Java**: 21 (OpenJDK)
- **Spring Boot**: 3.3.5
- **Build Tool**: Gradle 8.10.2
- **Database**: MariaDB 10.11+
- **Lombok**: 1.18.34 (Java 21 호환성을 위해 버전 고정)

## 주요 구현 현황

- **공통 모듈**:
    - `ApiResponse`, `PageRequest`, `PageResponse` (페이징/응답 표준화)
    - 전역 예외 처리 (`GlobalExceptionHandler`, `BusinessException`)
- **기준정보 (Master Data)**:
    - **회사 관리**: 목록 조회, 등록, 수정
    - **공장 관리**: 목록 조회, 등록, 수정
    - **품목 관리**: 목록 조회(검색/페이징), 등록, 수정
- **UI/UX 개선**:
    - 모든 기준정보 사용여부(Y/N)를 Select Box로 전환
    - 목록 하단 숫자형 페이징 네비게이션 적용
    - 입력 필드 너비 최적화 (`.field-use-yn`)

## 실행 방법

```bash
./gradlew bootRun
```

## 주요 화면 URL

- **홈**: `http://localhost:8080/`
- **회사 관리**: `http://localhost:8080/master/companies`
- **공장 관리**: `http://localhost:8080/master/plants`
- **품목 관리**: `http://localhost:8080/master/items`

## 주요 API URL (V1)

- **회사 API**: `/api/v1/master/companies`
- **공장 API**: `/api/v1/master/plants`
- **품목 API**: `/api/master/items` (V1 적용 중)
- **상태 확인**: `/actuator/health`

## 주의 사항

- **Lombok 이슈**: Java 21 사용 시 Lombok 버전이 낮으면 `Can't initialize javac processor` 에러가 발생할 수 있습니다. 현재 `build.gradle`에 `1.18.34`로 고정되어 있습니다. 만약 IDE에서 에러가 지속되면 `Java: Clean Java Language Server Workspace`를 수행해 주세요.
