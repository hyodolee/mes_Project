# MES 백엔드 설계 문서
## Spring Boot + MyBatis 기반 제조실행시스템

---

## 📋 목차
1. [시스템 개요](#1-시스템-개요)
2. [기술 스택](#2-기술-스택)
3. [프로젝트 구조](#3-프로젝트-구조)
4. [아키텍처 설계](#4-아키텍처-설계)
5. [공통 모듈 설계](#5-공통-모듈-설계)
6. [도메인별 설계](#6-도메인별-설계)
7. [API 설계 가이드](#7-api-설계-가이드)
8. [데이터베이스 연동](#8-데이터베이스-연동)
9. [보안 설계](#9-보안-설계)
10. [프로젝트 초기 설정](#10-프로젝트-초기-설정)

---

## 1. 시스템 개요

### 1.1 MES 시스템 설명
**MES (Manufacturing Execution System)**: 제조 현장의 생산 활동을 실시간으로 관리하는 시스템

### 1.2 핵심 기능 영역
```
┌─────────────────────────────────────────────────────────────┐
│                        MES 시스템                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ 기준정보관리  │  │ 생산계획관리  │  │ 생산실적관리  │      │
│  │  (Master)    │  │  (Planning)  │  │ (Production) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                  │                  │             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  품질관리     │  │  재고관리     │  │  설비관리     │      │
│  │  (Quality)   │  │ (Inventory)  │  │ (Equipment)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 개발 목표
- ✅ **유지보수성**: 도메인 중심 패키지 구조로 기능별 독립성 확보
- ✅ **확장성**: 새로운 기능 추가 시 기존 코드 영향 최소화
- ✅ **가독성**: 명확한 네이밍과 계층 구조로 누가 봐도 이해 가능
- ✅ **일관성**: 공통 모듈과 코딩 컨벤션으로 일관된 코드 스타일

---

## 2. 기술 스택

### 2.1 백엔드 기술

| 구분 | 기술 | 버전 | 선정 이유 |
|------|------|------|----------|
| **언어** | Java | 21 | LTS 버전, 최신 기능 지원 |
| **프레임워크** | Spring Boot | 3.3.5 | 빠른 개발, Auto Configuration |
| **데이터 접근** | MyBatis | 3.0.x | 복잡한 SQL 직접 제어, XML 매핑 |
| **데이터베이스** | MariaDB | 10.11+ | 오픈소스, MySQL 호환 |
| **빌드 도구** | Gradle | 8.10.2 | 의존성 관리 편리, Java 21 완벽 지원 |
| **문서화** | Springdoc OpenAPI | 2.6.0 | Swagger UI 자동 생성 |
| **유틸리티** | Lombok | 1.18.34 | 보일러플레이트 자동 생성 (Java 21 호환성 확보) |
| **매핑** | MapStruct | 1.5.x | DTO ↔ Entity 변환 자동화 |
| **보안** | Spring Security | 6.x | 최신 보안 표준 적용 |

### 2.2 프론트엔드 기술 (향후)
| 구분 | 기술 | 비고 |
|------|------|------|
| **프레임워크** | React / Vue.js | SPA 방식 |
| **상태관리** | Redux / Vuex | - |
| **UI 라이브러리** | Ant Design / Vuetify | - |

### 2.3 개발 환경
- **IDE**: IntelliJ IDEA / VS Code
- **JDK**: OpenJDK 21
- **DB 클라이언트**: DBeaver / HeidiSQL
- **API 테스트**: Postman / Swagger UI
- **버전 관리**: Git

---

## 3. 프로젝트 구조

### 3.1 전체 디렉토리 구조

```
mes-backend/
├── build.gradle                     # Gradle 빌드 설정
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/com/mes/
│   │   │   ├── MesBackserverApplication.java    # 메인 애플리케이션
│   │   │   │
│   │   │   ├── global/                          # 전역 설정 및 공통 모듈
│   │   │   │   ├── config/                      # 설정 클래스
│   │   │   │   ├── exception/                   # 예외 처리
│   │   │   │   ├── response/                    # 응답 포맷
│   │   │   │   └── common/dto/                  # 공통 DTO (PageRequest, PageResponse)
│   │   │   │
│   │   │   ├── domain/                          # 도메인 모델 및 DTO
│   │   │   │   ├── master/                      # 기준정보
│   │   │   │   └── planning/                    # 생산계획
│   │   │   │
│   │   │   ├── application/service/             # 비즈니스 로직
│   │   │   │
│   │   │   ├── interfaces/                      # 인터페이스 계층
│   │   │   │   ├── web/                         # Thymeleaf Controller
│   │   │   │   └── api/                         # REST Controller
│   │   │   │
│   │   │   └── infra/persistence/mybatis/mapper # MyBatis Mapper 인터페이스
│   │   │
│   │   └── resources/
│   │       ├── mapper/                          # SQL 매퍼 XML
│   │       ├── static/                          # 정적 리소스 (CSS, JS)
│   │       ├── templates/                       # Thymeleaf 템플릿
│   │       └── application.yml                  # 설정 파일
```

### 3.2 패키지 구조 설명

#### 📁 `global` 패키지
- **역할**: 애플리케이션 전역에서 사용되는 설정, 공통 유틸, 예외 처리
- **특징**: 도메인과 무관하게 모든 곳에서 재사용 가능

#### 📁 `domain` 패키지
- **역할**: 비즈니스 도메인별로 기능 분리
- **특징**: 각 도메인은 독립적이며, controller → service → mapper 계층 구조
- **하위 구조**:
  - `controller/`: REST API 엔드포인트
  - `service/`: 비즈니스 로직
  - `mapper/`: MyBatis 인터페이스 (DAO 역할)
  - `dto/`: 데이터 전송 객체

---

## 4. 아키텍처 설계

### 4.1 계층형 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│                     (Controller)                             │
│  - REST API 엔드포인트                                         │
│  - 요청 검증 (@Valid)                                          │
│  - 응답 포맷 변환                                              │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                     Business Layer                           │
│                     (Service)                                │
│  - 비즈니스 로직 처리                                          │
│  - 트랜잭션 관리 (@Transactional)                             │
│  - 도메인 규칙 검증                                            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                     Persistence Layer                        │
│                     (Mapper / DAO)                           │
│  - 데이터베이스 접근                                           │
│  - MyBatis SQL 매핑                                           │
│  - CRUD 작업                                                  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                     Database                                 │
│                     (MariaDB)                                │
│  - MES 테이블 (30개)                                          │
│  - 인덱스, 뷰, 프로시저                                        │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 각 계층의 책임

| 계층 | 클래스 | 주요 책임 | 사용 어노테이션 |
|------|--------|----------|---------------|
| **Presentation** | Controller | HTTP 요청/응답 처리, 입력 검증 | `@RestController`, `@RequestMapping` |
| **Business** | Service | 비즈니스 로직, 트랜잭션 관리 | `@Service`, `@Transactional` |
| **Persistence** | Mapper | SQL 실행, DB CRUD | `@Mapper` (MyBatis) |

### 4.3 데이터 흐름 (요청 → 응답)

```
[Client]
   │
   │ HTTP Request (JSON)
   ▼
[Controller]
   │ 1. @Valid로 요청 검증
   │ 2. DTO 파싱
   ▼
[Service]
   │ 3. 비즈니스 로직 수행
   │ 4. Mapper 호출
   ▼
[Mapper]
   │ 5. SQL 실행
   │ 6. ResultSet → DTO 매핑
   ▼
[Database]
   │
   │ 데이터 반환
   ▼
[Service]
   │ 7. 결과 가공
   ▼
[Controller]
   │ 8. ApiResponse로 래핑
   │ HTTP Response (JSON)
   ▼
[Client]
```

---

## 5. 공통 모듈 설계

### 5.1 통일된 API 응답 형식 (`ApiResponse`)

#### 📄 `ApiResponse.java`
```java
package com.mes.global.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * 통일된 API 응답 형식
 * - 성공/실패 여부, 메시지, 데이터를 일관되게 반환
 * - 프론트엔드에서 응답 파싱 로직 단순화
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;        // 성공 여부
    private String message;         // 메시지
    private T data;                 // 응답 데이터
    private LocalDateTime timestamp; // 응답 시간

    // ===== 정적 팩토리 메서드 =====

    /**
     * 성공 응답 (데이터 O)
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다.");
    }

    /**
     * 성공 응답 (데이터 O, 메시지 커스텀)
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = message;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    /**
     * 성공 응답 (데이터 X, 메시지만)
     */
    public static <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }

    /**
     * 실패 응답
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.timestamp = LocalDateTime.now();
        return response;
    }
}
```

**사용 예시**:
```java
// Controller에서
return ApiResponse.success(itemList, "품목 목록 조회 완료");
return ApiResponse.success("품목이 삭제되었습니다.");
return ApiResponse.error("품목을 찾을 수 없습니다.");
```

### 5.2 페이징 처리 (`PageRequest`, `PageResponse`)

#### 📄 `PageRequest.java`
```java
package com.mes.global.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageRequest {
    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }
}
```

#### 📄 `PageResponse.java`
```java
package com.mes.global.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    public static <T> PageResponse<T> of(List<T> content, int totalElements, PageRequest pageRequest) {
        int totalPages = (int) Math.ceil((double) totalElements / pageRequest.getSize());
        return new PageResponse<>(
            content,
            totalElements,
            totalPages,
            pageRequest.getPage(),
            pageRequest.getSize()
        );
    }
}
```

### 5.3 예외 처리 (`ErrorCode`, `BusinessException`, `GlobalExceptionHandler`)

#### 📄 `ErrorCode.java`
```java
package com.mes.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 * - HTTP 상태 코드와 메시지를 함께 관리
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ===== 공통 에러 =====
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "엔티티를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // ===== 품목 관련 =====
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다."),
    ITEM_CODE_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 품목 코드입니다."),

    // ===== 작업지시 관련 =====
    WORK_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "작업지시를 찾을 수 없습니다."),
    WORK_ORDER_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 시작된 작업지시입니다."),

    // ===== 재고 관련 =====
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),

    // ... 필요에 따라 추가
    ;

    private final HttpStatus status;
    private final String message;
}
```

#### 📄 `BusinessException.java`
```java
package com.mes.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외
 * - Service 계층에서 발생하는 업무 규칙 위반 시 사용
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

#### 📄 `GlobalExceptionHandler.java`
```java
package com.mes.global.exception;

import com.mes.global.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러
 * - 모든 예외를 이곳에서 일괄 처리하여 일관된 에러 응답 제공
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business Exception: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * Validation 실패 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("입력값 검증 실패");

        log.warn("Validation Exception: {}", errorMessage);
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(errorMessage));
    }

    /**
     * 그 외 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception", e);
        return ResponseEntity
            .internalServerError()
            .body(ApiResponse.error("서버 오류가 발생했습니다."));
    }
}
```

### 5.4 채번 유틸 (`SeqNoUtil`)

#### 📄 `SeqNoUtil.java`
```java
package com.mes.global.common.util;

import com.mes.domain.common.mapper.CommonCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 채번 유틸리티
 * - COM_SEQ_NO 테이블을 사용하여 일련번호 생성
 * - 형식: PREFIX + YYYYMMDD + 순번
 * - 예시: WO20250213001, PP20250213001
 */
@Component
@RequiredArgsConstructor
public class SeqNoUtil {

    private final CommonCodeMapper commonCodeMapper;

    /**
     * 일련번호 생성
     * @param seqType 채번 유형 (예: WO, PP, QC)
     * @param prefix 접두어
     * @param seqLength 순번 자릿수
     * @return 생성된 번호
     */
    @Transactional
    public String generateSeqNo(String seqType, String prefix, int seqLength) {
        LocalDate today = LocalDate.now();
        String seqDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // COM_SEQ_NO 테이블에서 현재 순번 조회 및 증가
        Integer currentSeq = commonCodeMapper.getAndIncrementSeqNo(seqType, today);

        // 순번 포맷팅 (예: 1 → 001)
        String seqNo = String.format("%0" + seqLength + "d", currentSeq);

        return prefix + seqDate + seqNo;
    }
}
```

---

## 6. 도메인별 설계

### 6.1 도메인 모듈 구조 (예: 품목 관리)

```
domain/
└── master/
    └── item/
        ├── controller/
        │   └── ItemController.java       # REST API
        ├── service/
        │   ├── ItemService.java          # 인터페이스
        │   └── ItemServiceImpl.java      # 구현체
        ├── mapper/
        │   └── ItemMapper.java           # MyBatis Mapper 인터페이스
        └── dto/
            ├── ItemDto.java              # 품목 정보 DTO
            ├── ItemSearchDto.java        # 검색 조건 DTO
            ├── ItemCreateDto.java        # 생성 요청 DTO
            └── ItemUpdateDto.java        # 수정 요청 DTO
```

### 6.2 예시: 품목 관리 (Item) 구현

#### 📄 `ItemDto.java` (응답용)
```java
package com.mes.domain.master.item.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 품목 정보 DTO
 * - MST_ITEM 테이블과 1:1 매핑
 */
@Data
public class ItemDto {
    private String itemCd;              // 품목코드
    private String plantCd;             // 공장코드
    private String itemNm;              // 품목명
    private String itemSpec;            // 규격
    private String itemType;            // 품목유형 (원자재/반제품/완제품)
    private String itemGrp;             // 품목그룹
    private String unit;                // 기본단위
    private BigDecimal safetyStockQty;  // 안전재고수량
    private Integer leadTime;           // 리드타임
    private BigDecimal purchasePrice;   // 구매단가
    private BigDecimal salePrice;       // 판매단가
    private String vendorCd;            // 주거래처코드
    private String useYn;               // 사용여부
    private String regUserId;           // 등록자
    private LocalDateTime regDtm;       // 등록일시
}
```

#### 📄 `ItemSearchDto.java` (검색 조건)
```java
package com.mes.domain.master.item.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSearchDto extends PageRequest {
    private String itemNm;
    private String useYn;
}
```

#### 📄 `ItemCreateDto.java` (생성 요청)
```java
package com.mes.domain.master.item.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 품목 생성 요청 DTO
 */
@Data
public class ItemCreateDto {

    @NotBlank(message = "품목코드는 필수입니다.")
    @Size(max = 50, message = "품목코드는 50자를 초과할 수 없습니다.")
    private String itemCd;

    @NotBlank(message = "공장코드는 필수입니다.")
    private String plantCd;

    @NotBlank(message = "품목명은 필수입니다.")
    @Size(max = 200, message = "품목명은 200자를 초과할 수 없습니다.")
    private String itemNm;

    private String itemSpec;

    @NotBlank(message = "품목유형은 필수입니다.")
    private String itemType;

    private String itemGrp;

    @NotBlank(message = "단위는 필수입니다.")
    private String unit;

    private BigDecimal safetyStockQty;
    private Integer leadTime;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private String vendorCd;
}
```

#### 📄 `ItemMapper.java` (MyBatis 인터페이스)
```java
package com.mes.domain.master.item.mapper;

import com.mes.domain.master.item.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 품목 Mapper 인터페이스
 * - MyBatis가 XML과 연결하여 구현체 자동 생성
 */
@Mapper
public interface ItemMapper {

    /**
     * 품목 목록 조회 (페이징)
     */
    List<ItemDto> selectItemList(ItemSearchDto searchDto);

    /**
     * 품목 총 개수 조회
     */
    int countItems(ItemSearchDto searchDto);

    /**
     * 품목 상세 조회
     */
    ItemDto selectItemByCode(@Param("itemCd") String itemCd);

    /**
     * 품목 등록
     */
    int insertItem(ItemCreateDto dto);

    /**
     * 품목 수정
     */
    int updateItem(ItemUpdateDto dto);

    /**
     * 품목 삭제 (논리 삭제: USE_YN = 'N')
     */
    int deleteItem(@Param("itemCd") String itemCd);

    /**
     * 품목 코드 중복 체크
     */
    int existsByItemCd(@Param("itemCd") String itemCd);
}
```

#### 📄 `ItemService.java` (인터페이스)
```java
package com.mes.domain.master.item.service;

import com.mes.domain.master.item.dto.*;
import com.mes.global.common.dto.PageResponse;

/**
 * 품목 서비스 인터페이스
 */
public interface ItemService {

    /**
     * 품목 목록 조회
     */
    PageResponse<ItemDto> getItemList(ItemSearchDto searchDto);

    /**
     * 품목 상세 조회
     */
    ItemDto getItem(String itemCd);

    /**
     * 품목 등록
     */
    void createItem(ItemCreateDto dto);

    /**
     * 품목 수정
     */
    void updateItem(String itemCd, ItemUpdateDto dto);

    /**
     * 품목 삭제
     */
    void deleteItem(String itemCd);
}
```

#### 📄 `ItemServiceImpl.java` (구현체)
```java
package com.mes.application.service.master;

import com.mes.domain.master.item.dto.*;
import com.mes.infra.persistence.mybatis.mapper.master.ItemMapper;
import com.mes.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemMapper itemMapper;

    public PageResponse<ItemDto> getItemList(ItemSearchDto searchDto) {
        List<ItemDto> items = itemMapper.selectItemList(searchDto);
        int totalCount = itemMapper.countItems(searchDto);
        return PageResponse.of(items, totalCount, searchDto);
    }
    
    // ... CRUD 메서드 생략
}
```

### 6.3 UI/UX 개선 사항
- **사용여부 선택 방식**: 기존 텍스트 입력에서 Select Box(사용/미사용) 선택 방식으로 변경하여 데이터 무결성 및 편의성 향상.
- **레이아웃 최적화**: 사용여부 등 짧은 입력 필드에 대해 `.field-use-yn` 클래스를 적용하여 고정 너비(120px)로 최적화.
- **페이징 네비게이션**: 목록 하단에 직관적인 숫자형 페이지 이동 버튼 구현.

#### 📄 `ItemController.java` (REST API)
```java
package com.mes.domain.master.item.controller;

import com.mes.domain.master.item.dto.*;
import com.mes.domain.master.item.service.ItemService;
import com.mes.global.common.dto.ApiResponse;
import com.mes.global.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 품목 관리 API
 */
@Tag(name = "품목 관리", description = "품목 CRUD API")
@RestController
@RequestMapping("/api/master/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * 품목 목록 조회
     */
    @Operation(summary = "품목 목록 조회", description = "검색 조건에 따른 품목 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ApiResponse<PageResponse<ItemDto>> getItemList(
            @ModelAttribute ItemSearchDto searchDto) {

        PageResponse<ItemDto> result = itemService.getItemList(searchDto);
        return ApiResponse.success(result, "품목 목록 조회 완료");
    }

    /**
     * 품목 상세 조회
     */
    @Operation(summary = "품목 상세 조회", description = "품목 코드로 상세 정보를 조회합니다.")
    @GetMapping("/{itemCd}")
    public ApiResponse<ItemDto> getItem(@PathVariable String itemCd) {
        ItemDto result = itemService.getItem(itemCd);
        return ApiResponse.success(result, "품목 상세 조회 완료");
    }

    /**
     * 품목 등록
     */
    @Operation(summary = "품목 등록", description = "새로운 품목을 등록합니다.")
    @PostMapping
    public ApiResponse<Void> createItem(@Valid @RequestBody ItemCreateDto dto) {
        itemService.createItem(dto);
        return ApiResponse.success("품목이 등록되었습니다.");
    }

    /**
     * 품목 수정
     */
    @Operation(summary = "품목 수정", description = "기존 품목 정보를 수정합니다.")
    @PutMapping("/{itemCd}")
    public ApiResponse<Void> updateItem(
            @PathVariable String itemCd,
            @Valid @RequestBody ItemUpdateDto dto) {

        itemService.updateItem(itemCd, dto);
        return ApiResponse.success("품목이 수정되었습니다.");
    }

    /**
     * 품목 삭제
     */
    @Operation(summary = "품목 삭제", description = "품목을 삭제합니다 (논리 삭제).")
    @DeleteMapping("/{itemCd}")
    public ApiResponse<Void> deleteItem(@PathVariable String itemCd) {
        itemService.deleteItem(itemCd);
        return ApiResponse.success("품목이 삭제되었습니다.");
    }
}
```

#### 📄 `ItemMapper.xml` (MyBatis SQL)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.mes.domain.master.item.mapper.ItemMapper">

    <!-- ResultMap 정의 -->
    <resultMap id="ItemResultMap" type="com.mes.domain.master.item.dto.ItemDto">
        <id property="itemCd" column="ITEM_CD"/>
        <result property="plantCd" column="PLANT_CD"/>
        <result property="itemNm" column="ITEM_NM"/>
        <result property="itemSpec" column="ITEM_SPEC"/>
        <result property="itemType" column="ITEM_TYPE"/>
        <result property="itemGrp" column="ITEM_GRP"/>
        <result property="unit" column="UNIT"/>
        <result property="safetyStockQty" column="SAFETY_STOCK_QTY"/>
        <result property="leadTime" column="LEAD_TIME"/>
        <result property="purchasePrice" column="PURCHASE_PRICE"/>
        <result property="salePrice" column="SALE_PRICE"/>
        <result property="vendorCd" column="VENDOR_CD"/>
        <result property="useYn" column="USE_YN"/>
        <result property="regUserId" column="REG_USER_ID"/>
        <result property="regDtm" column="REG_DTM"/>
    </resultMap>

    <!-- 공통 WHERE 조건 -->
    <sql id="searchCondition">
        WHERE 1=1
        <if test="plantCd != null and plantCd != ''">
            AND PLANT_CD = #{plantCd}
        </if>
        <if test="itemType != null and itemType != ''">
            AND ITEM_TYPE = #{itemType}
        </if>
        <if test="itemNm != null and itemNm != ''">
            AND ITEM_NM LIKE CONCAT('%', #{itemNm}, '%')
        </if>
        <if test="useYn != null and useYn != ''">
            AND USE_YN = #{useYn}
        </if>
    </sql>

    <!-- 품목 목록 조회 -->
    <select id="selectItemList" resultMap="ItemResultMap">
        SELECT
            ITEM_CD,
            PLANT_CD,
            ITEM_NM,
            ITEM_SPEC,
            ITEM_TYPE,
            ITEM_GRP,
            UNIT,
            SAFETY_STOCK_QTY,
            LEAD_TIME,
            PURCHASE_PRICE,
            SALE_PRICE,
            VENDOR_CD,
            USE_YN,
            REG_USER_ID,
            REG_DTM
        FROM MST_ITEM
        <include refid="searchCondition"/>
        ORDER BY REG_DTM DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <!-- 품목 총 개수 -->
    <select id="countItems" resultType="int">
        SELECT COUNT(*)
        FROM MST_ITEM
        <include refid="searchCondition"/>
    </select>

    <!-- 품목 상세 조회 -->
    <select id="selectItemByCode" resultMap="ItemResultMap">
        SELECT
            ITEM_CD,
            PLANT_CD,
            ITEM_NM,
            ITEM_SPEC,
            ITEM_TYPE,
            ITEM_GRP,
            UNIT,
            SAFETY_STOCK_QTY,
            LEAD_TIME,
            PURCHASE_PRICE,
            SALE_PRICE,
            VENDOR_CD,
            USE_YN,
            REG_USER_ID,
            REG_DTM
        FROM MST_ITEM
        WHERE ITEM_CD = #{itemCd}
    </select>

    <!-- 품목 등록 -->
    <insert id="insertItem">
        INSERT INTO MST_ITEM (
            ITEM_CD,
            PLANT_CD,
            ITEM_NM,
            ITEM_SPEC,
            ITEM_TYPE,
            ITEM_GRP,
            UNIT,
            SAFETY_STOCK_QTY,
            LEAD_TIME,
            PURCHASE_PRICE,
            SALE_PRICE,
            VENDOR_CD,
            USE_YN,
            REG_USER_ID,
            REG_DTM
        ) VALUES (
            #{itemCd},
            #{plantCd},
            #{itemNm},
            #{itemSpec},
            #{itemType},
            #{itemGrp},
            #{unit},
            #{safetyStockQty},
            #{leadTime},
            #{purchasePrice},
            #{salePrice},
            #{vendorCd},
            'Y',
            'SYSTEM',
            NOW()
        )
    </insert>

    <!-- 품목 수정 -->
    <update id="updateItem">
        UPDATE MST_ITEM
        SET
            ITEM_NM = #{itemNm},
            ITEM_SPEC = #{itemSpec},
            ITEM_TYPE = #{itemType},
            ITEM_GRP = #{itemGrp},
            UNIT = #{unit},
            SAFETY_STOCK_QTY = #{safetyStockQty},
            LEAD_TIME = #{leadTime},
            PURCHASE_PRICE = #{purchasePrice},
            SALE_PRICE = #{salePrice},
            VENDOR_CD = #{vendorCd},
            UPD_USER_ID = 'SYSTEM',
            UPD_DTM = NOW()
        WHERE ITEM_CD = #{itemCd}
    </update>

    <!-- 품목 삭제 (논리 삭제) -->
    <update id="deleteItem">
        UPDATE MST_ITEM
        SET
            USE_YN = 'N',
            UPD_USER_ID = 'SYSTEM',
            UPD_DTM = NOW()
        WHERE ITEM_CD = #{itemCd}
    </update>

    <!-- 품목 코드 중복 체크 -->
    <select id="existsByItemCd" resultType="int">
        SELECT COUNT(*)
        FROM MST_ITEM
        WHERE ITEM_CD = #{itemCd}
    </select>

</mapper>
```

### 6.3 도메인별 주요 기능 요약

| 도메인 | 주요 기능 | 핵심 테이블 |
|--------|----------|------------|
| **공통코드** | 코드그룹/코드 관리, 채번 관리 | COM_CODE_GRP, COM_CODE, COM_SEQ_NO |
| **기준정보** | 품목, BOM, 공정, 설비, 작업자, 거래처 관리 | MST_ITEM, MST_BOM, MST_ROUTING 등 |
| **생산계획** | 생산계획 수립, 작업지시 생성 | PLN_PROD_PLAN, PLN_WORK_ORDER |
| **생산실적** | 작업실적 등록, 공정실적 추적, 불량 기록 | PRD_WORK_RESULT, PRD_PROCESS_RESULT |
| **품질관리** | 검사기준 관리, 검사실적 등록, 불량 분석 | QC_INSPECT_STD, QC_INSPECT_RESULT |
| **재고관리** | 재고현황 조회, 입출고 처리, LOT 추적 | INV_STOCK, INV_TRANS_HIS, INV_LOT |
| **설비관리** | 가동현황 모니터링, 비가동 관리, 정비 이력 | EQP_OPER_STATUS, EQP_DOWNTIME |

---

## 7. API 설계 가이드

### 7.1 RESTful API 설계 원칙

| HTTP Method | 용도 | URI 예시 | 설명 |
|-------------|------|----------|------|
| **GET** | 조회 | `/api/master/items` | 품목 목록 조회 |
| **GET** | 상세 조회 | `/api/master/items/{itemCd}` | 품목 상세 조회 |
| **POST** | 생성 | `/api/master/items` | 품목 생성 |
| **PUT** | 전체 수정 | `/api/master/items/{itemCd}` | 품목 전체 수정 |
| **PATCH** | 부분 수정 | `/api/master/items/{itemCd}` | 품목 일부 필드만 수정 |
| **DELETE** | 삭제 | `/api/master/items/{itemCd}` | 품목 삭제 |

### 7.2 API 경로 규칙

```
/api/{domain}/{resource}
```

**예시**:
```
/api/master/items              # 품목
/api/master/boms               # BOM
/api/planning/work-orders      # 작업지시
/api/production/results        # 작업실적
/api/quality/inspections       # 검사실적
/api/inventory/stocks          # 재고현황
/api/equipment/operations      # 설비가동현황
```

### 7.3 요청/응답 예시

#### 📌 품목 목록 조회
**요청**:
```http
GET /api/master/items?page=1&size=20&itemType=원자재&itemNm=리튬
```

**응답**:
```json
{
  "success": true,
  "message": "품목 목록 조회 완료",
  "data": {
    "content": [
      {
        "itemCd": "RM-LCO-001",
        "plantCd": "P001",
        "itemNm": "LiCoO2 (리튬코발트산화물)",
        "itemSpec": "99.9%",
        "itemType": "원자재",
        "itemGrp": "양극활물질",
        "unit": "KG",
        "safetyStockQty": 1000.000,
        "leadTime": 30,
        "purchasePrice": 45000.00,
        "vendorCd": "V001",
        "useYn": "Y",
        "regUserId": "admin",
        "regDtm": "2025-02-13T10:30:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 1,
    "size": 20
  },
  "timestamp": "2025-02-13T14:25:30"
}
```

#### 📌 작업지시 생성
**요청**:
```http
POST /api/planning/work-orders
Content-Type: application/json

{
  "plantCd": "P001",
  "itemCd": "FG-CELL-001",
  "woQty": 1000,
  "workcenterCd": "WC001",
  "planStartDtm": "2025-02-14T08:00:00",
  "planEndDtm": "2025-02-14T18:00:00",
  "priority": 1
}
```

**응답**:
```json
{
  "success": true,
  "message": "작업지시가 생성되었습니다.",
  "data": {
    "woNo": "WO20250213001",
    "woId": 123
  },
  "timestamp": "2025-02-13T14:30:00"
}
```

### 7.4 에러 응답 예시

```json
{
  "success": false,
  "message": "품목을 찾을 수 없습니다: RM-XXX-999",
  "data": null,
  "timestamp": "2025-02-13T14:30:00"
}
```

---

## 8. 데이터베이스 연동

### 8.1 MyBatis 설정

#### 📄 `mybatis-config.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>
    <settings>
        <!-- 카멜케이스 ↔ 스네이크케이스 자동 매핑 -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>

        <!-- NULL 값 처리 -->
        <setting name="jdbcTypeForNull" value="NULL"/>

        <!-- 로그 -->
        <setting name="logImpl" value="SLF4J"/>
    </settings>

    <typeAliases>
        <!-- DTO 패키지 자동 스캔 -->
        <package name="com.mes.domain.*.*.dto"/>
    </typeAliases>
</configuration>
```

#### 📄 `MyBatisConfig.java`
```java
package com.mes.global.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis 설정
 */
@Configuration
@MapperScan(basePackages = "com.mes.domain.**.mapper")
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        // mybatis-config.xml 위치
        Resource configLocation = new PathMatchingResourcePatternResolver()
            .getResource("classpath:mybatis/mybatis-config.xml");
        sessionFactory.setConfigLocation(configLocation);

        // Mapper XML 파일 위치
        Resource[] mapperLocations = new PathMatchingResourcePatternResolver()
            .getResources("classpath:mybatis/mapper/**/*Mapper.xml");
        sessionFactory.setMapperLocations(mapperLocations);

        return sessionFactory.getObject();
    }
}
```

### 8.2 데이터소스 설정

#### 📄 `application.yml`
```yaml
spring:
  application:
    name: mes-backend

  # 프로필 설정
  profiles:
    active: dev

  # 데이터소스
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://localhost:3306/mes_backend_server?useUnicode=true&characterEncoding=utf8mb4
    username: mes_user
    password: mes_password

    # HikariCP 설정
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

# MyBatis 설정
mybatis:
  configuration:
    map-underscore-to-camel-case: true
    jdbc-type-for-null: NULL
  type-aliases-package: com.mes.domain.**.dto

# 로깅
logging:
  level:
    com.mes: DEBUG
    com.mes.domain.**.mapper: TRACE
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# 서버 설정
server:
  port: 8080
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

---

## 9. 보안 설계

### 9.1 인증/인가 (Spring Security)

#### 📄 `SecurityConfig.java`
```java
package com.mes.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * - 현재는 기본 설정만, 향후 JWT 인증 추가 예정
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (REST API에서는 불필요)
            .csrf(csrf -> csrf.disable())

            // 세션 사용 안 함 (JWT 사용 시)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()      // 인증 API는 모두 허용
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger 허용
                .anyRequest().authenticated()                      // 나머지는 인증 필요
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 9.2 CORS 설정

#### 📄 `WebConfig.java`
```java
package com.mes.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")  // 프론트엔드 주소
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 10. 프로젝트 초기 설정

### 10.1 Gradle 빌드 설정

#### 📄 `build.gradle`
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.mes'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'
    
    compileOnly 'org.projectlombok:lombok:1.18.34'
    annotationProcessor 'org.projectlombok:lombok:1.18.34'
    
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 10.2 프로젝트 생성 절차

#### Step 1: Spring Initializr
1. https://start.spring.io/ 접속
2. 설정:
   - Project: Gradle - Groovy
   - Language: Java
   - Spring Boot: 3.3.x
   - Java: 21
   - Dependencies: Spring Web, Validation, Thymeleaf, Lombok

#### Step 2: 의존성 추가
- `build.gradle`에 MyBatis, MariaDB, Swagger 추가 (Lombok 버전 고정)

#### Step 3: 패키지 구조 생성
```bash
mkdir -p src/main/java/com/mes/{global/{config,common,exception},domain}
mkdir -p src/main/resources/mybatis/mapper
```

#### Step 4: 설정 파일 작성
- `application.yml` 작성 (DB 연결 정보)
- `mybatis-config.xml` 작성
- `MyBatisConfig.java` 작성

#### Step 5: 공통 모듈 작성
- `ApiResponse`, `PageRequest`, `PageResponse` 작성
- `ErrorCode`, `BusinessException`, `GlobalExceptionHandler` 작성

#### Step 6: 도메인 모듈 개발
- 품목 관리부터 시작 (가장 기본적인 마스터 데이터)
- Controller → Service → Mapper → XML 순서로 작성

---

## 11. 다음 단계

### 11.1 우선순위별 개발 순서

```
1순위: 공통 모듈 개발
  ├── ApiResponse, 예외 처리
  ├── MyBatis 설정
  └── Swagger 설정

2순위: 기준정보 개발 (Master Data)
  ├── 공통코드 관리
  ├── 품목 관리
  ├── BOM 관리
  ├── 공정 관리
  └── 설비 관리

3순위: 생산계획 개발
  ├── 생산계획 관리
  └── 작업지시 관리

4순위: 생산실적 개발
  ├── 작업실적 등록
  ├── 공정실적 추적
  └── 불량 이력

5순위: 품질관리 개발
  ├── 검사기준 관리
  ├── 검사실적 등록
  └── 불량 분석

6순위: 재고관리 개발
  ├── 재고현황 조회
  ├── 입출고 처리
  └── LOT 추적

7순위: 설비관리 개발
  ├── 가동현황 모니터링
  ├── 비가동 관리
  └── 정비 관리
```

### 11.2 추가 고려 사항

- **인증/인가**: JWT 기반 로그인 구현
- **파일 업로드**: 엑셀 대량 등록, 파일 첨부 기능
- **실시간 모니터링**: WebSocket 또는 SSE 활용
- **보고서**: JasperReports 또는 Apache POI로 Excel/PDF 생성
- **스케줄러**: Spring Batch 또는 Quartz로 배치 작업
- **캐싱**: Redis 또는 Caffeine으로 성능 최적화

---

## 📚 참고 자료

- **Spring Boot 공식 문서**: https://spring.io/projects/spring-boot
- **MyBatis 공식 문서**: https://mybatis.org/mybatis-3/
- **Springdoc OpenAPI**: https://springdoc.org/
- **MariaDB 문서**: https://mariadb.com/kb/en/documentation/

---

## ✅ 체크리스트

프로젝트 생성 시 확인 사항:
- [x] JDK 21 설치
- [x] MariaDB 설치 및 DB 생성
- [x] Gradle 빌드 성공 (Lombok 1.18.34 적용)
- [x] application.yml DB 연결 정보 설정
- [x] MyBatis 설정 완료
- [ ] Swagger UI 접속 확인 (http://localhost:8080/swagger-ui.html)
- [x] 품목 관리 CRUD/검색/페이징 테스트 성공

---

**문서 버전**: 1.1
**작성일**: 2026-03-31
**작성자**: Gemini CLI
