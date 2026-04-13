# MCS (Material Control System) 설계 문서 v2
## MES 연동 기반 재설계

---

## 1. MES ↔ MCS 연동 분석

### 1.1 MES 기존 테이블 중 MCS가 공유(참조)해야 하는 것

| MES 테이블 | 역할 | MCS 사용 방식 |
|------------|------|---------------|
| `MST_PLANT` | 공장 마스터 | FK 참조 (다공장 지원) |
| `MST_ITEM` | 품목 마스터 | FK 참조 (자재 = MES의 품목) |
| `MST_WAREHOUSE` | 창고 마스터 | FK 참조 |
| `MST_VENDOR` | 거래처 마스터 | FK 참조 (공급업체) |
| `MST_WORKER` | 작업자 마스터 | FK 참조 (처리자) |
| `INV_LOT` | LOT 마스터 | FK 참조 |
| `INV_STOCK` | 재고 현황 | 연동 (MCS 입출고 시 동기화) |
| `INV_TRANS_HIS` | 입출고 이력 | 연동 (MCS 트랜잭션 발생 시 동시 기록) |
| `COM_CODE_GRP` / `COM_CODE` | 공통코드 | MCS 코드 그룹 추가 등록 |
| `COM_SEQ_NO` | 채번 관리 | MCS 채번 유형 추가 등록 |

### 1.2 기존 MCS 설계 대비 변경 사항 요약

| 항목 | 기존 MCS 설계 | 변경 후 (MES 연동) |
|------|--------------|-------------------|
| **네이밍** | lower_snake_case | **UPPER_SNAKE_CASE** (MES 규칙) |
| **자재 관리** | 자체 material 테이블 | **MST_ITEM 참조** (별도 생성 안 함) |
| **창고 관리** | 자체 warehouse 테이블 | **MST_WAREHOUSE 참조** |
| **거래처** | supplier VARCHAR 필드 | **MST_VENDOR FK 참조** |
| **LOT** | 자체 lot 테이블 | **INV_LOT 참조** |
| **사용자** | 자체 users 테이블 | **MST_WORKER 참조** |
| **상태값** | ENUM/VARCHAR 하드코딩 | **COM_CODE 공통코드 참조** |
| **채번** | Spring에서 생성 | **COM_SEQ_NO 채번 테이블 활용** |
| **감사 컬럼** | created_at, created_by | **REG_USER_ID, REG_DTM, UPD_USER_ID, UPD_DTM** |
| **다공장** | 미지원 | **PLANT_CD 필수** |
| **재고 연동** | 독립 재고 | **INV_STOCK, INV_TRANS_HIS와 동기화** |
| **카테고리** | material_category 테이블 | **MST_ITEM.ITEM_TYPE + ITEM_GRP** 활용 |
| **테이블 접두어** | 없음 | **MCS_ 접두어** |

### 1.3 MCS가 새로 추가하는 영역 (MES에 없는 것)

MES는 창고(WAREHOUSE) 단위까지만 관리하고, 그 아래 **구역(Zone) → 로케이션(Location)** 세분화가 없습니다.
또한 입고/출고의 **워크플로(상태 전이)**가 단순합니다 (예정→완료 정도).

MCS가 확장하는 영역:
- **Zone/Location**: 창고 내부 위치 계층화
- **입고 워크플로**: PLANNED → ARRIVED → INSPECTING → COMPLETED
- **출고 워크플로**: REQUESTED → ALLOCATED → PICKING → PICKED → SHIPPED
- **이동 관리**: 로케이션 간 자재 이동 전용 오더
- **로케이션 레벨 재고**: INV_STOCK(창고 레벨)의 하위 상세

---

## 2. 기술 스택 (변경 없음)

| 구분 | 기술 |
|------|------|
| Language | Java 21 (MES와 동일 LTS 버전) |
| Framework | Spring Boot 3.x |
| Data Access | **MyBatis 3 + mybatis-spring-boot-starter** |
| DB | MariaDB 10.11+ (MES와 **동일 DB 인스턴스** 사용) |
| Build | Gradle (Groovy DSL, MES와 동일) |
| API 문서 | Springdoc OpenAPI |
| 기타 | Lombok, MapStruct |

---

## 3. 프로젝트 구조

```
mcs/
├── build.gradle
├── src/main/java/com/mcs/
│   ├── McsApplication.java
│   │
│   ├── global/
│   │   ├── config/
│   │   │   ├── MyBatisConfig.java
│   │   │   └── SwaggerConfig.java
│   │   ├── common/
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── PageRequest.java
│   │   │   └── util/
│   │   │       └── SeqNoUtil.java         ← COM_SEQ_NO 활용 채번
│   │   └── exception/
│   │       ├── ErrorCode.java
│   │       ├── BusinessException.java
│   │       └── GlobalExceptionHandler.java
│   │
│   └── domain/
│       ├── zone/                          ← MCS 신규
│       │   ├── controller/
│       │   ├── service/
│       │   ├── mapper/ZoneMapper.java
│       │   └── dto/
│       │
│       ├── location/                      ← MCS 신규
│       │   ├── controller/
│       │   ├── service/
│       │   ├── mapper/LocationMapper.java
│       │   └── dto/
│       │
│       ├── inbound/                       ← MCS 확장 (INV_RECEIVE_PLAN 연동)
│       │   ├── controller/
│       │   ├── service/InboundService.java
│       │   ├── mapper/InboundMapper.java
│       │   └── dto/
│       │
│       ├── outbound/                      ← MCS 확장 (INV_ISSUE_PLAN 연동)
│       │   ├── controller/
│       │   ├── service/OutboundService.java
│       │   ├── mapper/OutboundMapper.java
│       │   └── dto/
│       │
│       ├── transfer/                      ← MCS 신규
│       │   ├── controller/
│       │   ├── service/
│       │   ├── mapper/TransferMapper.java
│       │   └── dto/
│       │
│       ├── inventory/                     ← MCS 확장 (INV_STOCK 연동)
│       │   ├── controller/
│       │   ├── service/InventoryService.java
│       │   ├── mapper/InventoryMapper.java
│       │   └── dto/
│       │
│       └── mes/                           ← MES 마스터 조회 전용
│           ├── mapper/MesItemMapper.java
│           ├── mapper/MesWarehouseMapper.java
│           ├── mapper/MesComCodeMapper.java
│           └── dto/
│
├── src/main/resources/
│   ├── application.yml
│   └── mapper/
│       ├── ZoneMapper.xml
│       ├── LocationMapper.xml
│       ├── InboundMapper.xml
│       ├── OutboundMapper.xml
│       ├── TransferMapper.xml
│       ├── InventoryMapper.xml
│       └── mes/
│           ├── MesItemMapper.xml
│           ├── MesWarehouseMapper.xml
│           └── MesComCodeMapper.xml
```

---

## 4. DDL - MCS 전용 테이블

> **MES 기존 테이블은 건드리지 않고, MCS_ 접두어 테이블만 추가합니다.**

```sql
-- ============================================================
-- MCS (Material Control System) DDL
-- MES DB(MES_DB)에 추가 생성
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 공통코드 그룹 추가 (COM_CODE_GRP에 INSERT)
-- ============================================================
INSERT INTO COM_CODE_GRP (GRP_CD, GRP_NM, GRP_DESC, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_IB_STATUS', '입고상태', 'MCS 입고 오더 상태', 'Y', 'SYSTEM', NOW()),
('MCS_IB_ITEM_STATUS', '입고품목상태', 'MCS 입고 품목별 상태', 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', '출고상태', 'MCS 출고 오더 상태', 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', '출고품목상태', 'MCS 출고 품목별 상태', 'Y', 'SYSTEM', NOW()),
('MCS_TF_STATUS', '이동상태', 'MCS 이동 오더 상태', 'Y', 'SYSTEM', NOW()),
('MCS_TF_ITEM_STATUS', '이동품목상태', 'MCS 이동 품목별 상태', 'Y', 'SYSTEM', NOW()),
('MCS_LOC_STATUS', '로케이션상태', 'MCS 로케이션 상태', 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', '구역유형', 'MCS 구역 유형', 'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', '재고거래유형', 'MCS 로케이션 재고 변동 유형', 'Y', 'SYSTEM', NOW());

-- ============================================================
-- 2. 공통코드 추가 (COM_CODE에 INSERT)
-- ============================================================
-- 입고 상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_IB_STATUS', 'PLANNED',    '입고예정', 1, 'Y', 'SYSTEM', NOW()),
('MCS_IB_STATUS', 'ARRIVED',    '도착',     2, 'Y', 'SYSTEM', NOW()),
('MCS_IB_STATUS', 'INSPECTING', '검수중',   3, 'Y', 'SYSTEM', NOW()),
('MCS_IB_STATUS', 'COMPLETED',  '완료',     4, 'Y', 'SYSTEM', NOW()),
('MCS_IB_STATUS', 'CANCELLED',  '취소',     5, 'Y', 'SYSTEM', NOW());

-- 입고 품목 상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_IB_ITEM_STATUS', 'PENDING',   '대기',     1, 'Y', 'SYSTEM', NOW()),
('MCS_IB_ITEM_STATUS', 'INSPECTED', '검수완료', 2, 'Y', 'SYSTEM', NOW()),
('MCS_IB_ITEM_STATUS', 'STOCKED',   '적치완료', 3, 'Y', 'SYSTEM', NOW()),
('MCS_IB_ITEM_STATUS', 'REJECTED',  '반품',     4, 'Y', 'SYSTEM', NOW());

-- 출고 상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_OB_STATUS', 'REQUESTED', '출고요청', 1, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'ALLOCATED', '할당완료', 2, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'PICKING',   '피킹중',  3, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'PICKED',    '피킹완료', 4, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'SHIPPED',   '출하완료', 5, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'CANCELLED', '취소',     6, 'Y', 'SYSTEM', NOW());

-- 출고 품목 상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_OB_ITEM_STATUS', 'PENDING',   '대기',     1, 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', 'ALLOCATED', '할당됨',   2, 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', 'PICKED',    '피킹완료', 3, 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', 'SHIPPED',   '출하완료', 4, 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', 'CANCELLED', '취소',     5, 'Y', 'SYSTEM', NOW());

-- 이동 상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_TF_STATUS', 'REQUESTED',   '이동요청', 1, 'Y', 'SYSTEM', NOW()),
('MCS_TF_STATUS', 'IN_PROGRESS', '이동중',   2, 'Y', 'SYSTEM', NOW()),
('MCS_TF_STATUS', 'COMPLETED',   '완료',     3, 'Y', 'SYSTEM', NOW()),
('MCS_TF_STATUS', 'CANCELLED',   '취소',     4, 'Y', 'SYSTEM', NOW());

-- 이동 품목 상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_TF_ITEM_STATUS', 'PENDING',   '대기',   1, 'Y', 'SYSTEM', NOW()),
('MCS_TF_ITEM_STATUS', 'MOVED',     '이동완료', 2, 'Y', 'SYSTEM', NOW()),
('MCS_TF_ITEM_STATUS', 'CANCELLED', '취소',   3, 'Y', 'SYSTEM', NOW());

-- 로케이션 상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_LOC_STATUS', 'EMPTY',   '비어있음', 1, 'Y', 'SYSTEM', NOW()),
('MCS_LOC_STATUS', 'PARTIAL', '일부사용', 2, 'Y', 'SYSTEM', NOW()),
('MCS_LOC_STATUS', 'FULL',    '가득참',   3, 'Y', 'SYSTEM', NOW()),
('MCS_LOC_STATUS', 'BLOCKED', '사용불가', 4, 'Y', 'SYSTEM', NOW());

-- 구역 유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_ZONE_TYPE', 'STORAGE',   '보관구역', 1, 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', 'RECEIVING', '입고구역', 2, 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', 'SHIPPING',  '출하구역', 3, 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', 'STAGING',   '임시구역', 4, 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', 'QC',        '검수구역', 5, 'Y', 'SYSTEM', NOW());

-- 로케이션 재고 변동 유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, ATTR1, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_INV_TX_TYPE', 'IB_IN',       '입고적치',   1, 'IN',   'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'OB_OUT',      '출고출하',   2, 'OUT',  'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'TF_OUT',      '이동출발',   3, 'OUT',  'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'TF_IN',       '이동도착',   4, 'IN',   'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'ADJ_PLUS',    '조정증가',   5, 'IN',   'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'ADJ_MINUS',   '조정감소',   6, 'OUT',  'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'PRD_ISSUE',   '생산투입',   7, 'OUT',  'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'PRD_RECEIPT', '생산입고',   8, 'IN',   'Y', 'SYSTEM', NOW());

-- ============================================================
-- 3. 채번 유형 추가 (COM_SEQ_NO용 - 실제 채번은 런타임)
--    MCS 채번: MCS-IB(입고), MCS-OB(출고), MCS-TF(이동)
--    ★ 접두어를 MCS- 로 시작하여 MES 채번(WO, PP 등)과 충돌 방지
--    ★ INV_TRANS_HIS.TRANS_NO도 MCS-IB-, MCS-OB-, MCS-TF- 접두어 사용
-- ============================================================

-- ============================================================
-- 4. MCS_ZONE - 구역 (MES에 없는 테이블)
-- ============================================================
CREATE TABLE MCS_ZONE (
    ZONE_ID         BIGINT          NOT NULL AUTO_INCREMENT COMMENT '구역ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    WAREHOUSE_CD    VARCHAR(20)     NOT NULL COMMENT '창고코드',
    ZONE_CD         VARCHAR(20)     NOT NULL COMMENT '구역코드',
    ZONE_NM         VARCHAR(100)    NOT NULL COMMENT '구역명',
    ZONE_TYPE       VARCHAR(20)     NOT NULL DEFAULT 'STORAGE'
                    COMMENT '구역유형 (COM_CODE: MCS_ZONE_TYPE)',
    SORT_SEQ        INT             NOT NULL DEFAULT 0 COMMENT '정렬순서',
    USE_YN          CHAR(1)         NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (ZONE_ID),
    UNIQUE KEY UK_MCS_ZONE (WAREHOUSE_CD, ZONE_CD),
    KEY IDX_MCS_ZONE_01 (PLANT_CD, WAREHOUSE_CD),
    CONSTRAINT FK_MCS_ZONE_PLANT FOREIGN KEY (PLANT_CD) REFERENCES MST_PLANT(PLANT_CD),
    CONSTRAINT FK_MCS_ZONE_WH FOREIGN KEY (WAREHOUSE_CD) REFERENCES MST_WAREHOUSE(WAREHOUSE_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 구역정보';

-- ============================================================
-- 5. MCS_LOCATION - 로케이션 (MES에 없는 테이블)
-- ============================================================
CREATE TABLE MCS_LOCATION (
    LOCATION_ID     BIGINT          NOT NULL AUTO_INCREMENT COMMENT '로케이션ID',
    ZONE_ID         BIGINT          NOT NULL COMMENT '구역ID',
    LOCATION_CD     VARCHAR(30)     NOT NULL COMMENT '로케이션코드 (예: A-01-03)',
    LOCATION_NM     VARCHAR(100)    DEFAULT NULL COMMENT '로케이션명',
    MAX_CAPACITY    DECIMAL(15,3)   DEFAULT 0.000 COMMENT '최대수용량',
    CURRENT_USAGE   DECIMAL(15,3)   DEFAULT 0.000 COMMENT '현재사용량',
    LOCATION_STATUS VARCHAR(20)     NOT NULL DEFAULT 'EMPTY'
                    COMMENT '로케이션상태 (COM_CODE: MCS_LOC_STATUS)',
    USE_YN          CHAR(1)         NOT NULL DEFAULT 'Y' COMMENT '사용여부',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (LOCATION_ID),
    UNIQUE KEY UK_MCS_LOCATION (ZONE_ID, LOCATION_CD),
    KEY IDX_MCS_LOCATION_01 (LOCATION_STATUS),
    CONSTRAINT FK_MCS_LOCATION_ZONE FOREIGN KEY (ZONE_ID) REFERENCES MCS_ZONE(ZONE_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 로케이션정보';

-- ============================================================
-- 6. MCS_LOCATION_STOCK - 로케이션 레벨 재고
--    (INV_STOCK은 창고 레벨, 이 테이블은 그 하위 상세)
-- ============================================================
CREATE TABLE MCS_LOCATION_STOCK (
    LOC_STOCK_ID    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '로케이션재고ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    LOCATION_ID     BIGINT          NOT NULL COMMENT '로케이션ID',
    ITEM_CD         VARCHAR(50)     NOT NULL COMMENT '품목코드',
    LOT_NO          VARCHAR(50)     DEFAULT NULL COMMENT 'LOT번호',
    STOCK_QTY       DECIMAL(15,3)   NOT NULL DEFAULT 0.000 COMMENT '재고수량',
    RESERVED_QTY    DECIMAL(15,3)   DEFAULT 0.000 COMMENT '예약수량',
    AVAILABLE_QTY   DECIMAL(15,3)   GENERATED ALWAYS AS (STOCK_QTY - COALESCE(RESERVED_QTY, 0)) STORED
                    COMMENT '가용수량',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (LOC_STOCK_ID),
    UNIQUE KEY UK_MCS_LOC_STOCK (LOCATION_ID, ITEM_CD, LOT_NO),
    KEY IDX_MCS_LOC_STOCK_01 (PLANT_CD, ITEM_CD),
    KEY IDX_MCS_LOC_STOCK_02 (ITEM_CD, LOT_NO),
    CONSTRAINT FK_MCS_LOC_STOCK_PLANT FOREIGN KEY (PLANT_CD) REFERENCES MST_PLANT(PLANT_CD),
    CONSTRAINT FK_MCS_LOC_STOCK_LOC FOREIGN KEY (LOCATION_ID) REFERENCES MCS_LOCATION(LOCATION_ID),
    CONSTRAINT FK_MCS_LOC_STOCK_ITEM FOREIGN KEY (ITEM_CD) REFERENCES MST_ITEM(ITEM_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 로케이션재고';

-- ============================================================
-- 7. MCS_LOC_TRANS_HIS - 로케이션 재고 트랜잭션 이력
--    (INV_TRANS_HIS는 창고 레벨, 이 테이블은 로케이션 레벨)
-- ============================================================
CREATE TABLE MCS_LOC_TRANS_HIS (
    LOC_TRANS_ID    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '로케이션거래ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    LOC_STOCK_ID    BIGINT          NOT NULL COMMENT '로케이션재고ID',
    TRANS_TYPE      VARCHAR(20)     NOT NULL
                    COMMENT '거래유형 (COM_CODE: MCS_INV_TX_TYPE)',
    TRANS_QTY       DECIMAL(15,3)   NOT NULL COMMENT '변동수량',
    BEFORE_QTY      DECIMAL(15,3)   NOT NULL COMMENT '변경전수량',
    AFTER_QTY       DECIMAL(15,3)   NOT NULL COMMENT '변경후수량',
    REF_TYPE        VARCHAR(20)     DEFAULT NULL COMMENT '참조유형 (IB/OB/TF/ADJ)',
    REF_NO          VARCHAR(50)     DEFAULT NULL COMMENT '참조번호',
    REF_ID          BIGINT          DEFAULT NULL COMMENT '참조ID',
    INV_TRANS_ID    BIGINT          DEFAULT NULL COMMENT 'MES INV_TRANS_HIS 연동 ID',
    TRANS_RMK       VARCHAR(500)    DEFAULT NULL COMMENT '비고',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',

    PRIMARY KEY (LOC_TRANS_ID),
    KEY IDX_MCS_LOC_TRANS_01 (PLANT_CD, REG_DTM),
    KEY IDX_MCS_LOC_TRANS_02 (LOC_STOCK_ID),
    KEY IDX_MCS_LOC_TRANS_03 (TRANS_TYPE),
    KEY IDX_MCS_LOC_TRANS_04 (REF_TYPE, REF_NO),
    CONSTRAINT FK_MCS_LOC_TRANS_PLANT FOREIGN KEY (PLANT_CD) REFERENCES MST_PLANT(PLANT_CD),
    CONSTRAINT FK_MCS_LOC_TRANS_STOCK FOREIGN KEY (LOC_STOCK_ID) REFERENCES MCS_LOCATION_STOCK(LOC_STOCK_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 로케이션재고이력';

-- ============================================================
-- 8. MCS_INBOUND_ORDER - 입고 오더
--    (MES INV_RECEIVE_PLAN과 연동 가능)
-- ============================================================
CREATE TABLE MCS_INBOUND_ORDER (
    INBOUND_ID      BIGINT          NOT NULL AUTO_INCREMENT COMMENT '입고ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    INBOUND_NO      VARCHAR(30)     NOT NULL COMMENT '입고번호 (채번: MCS-IB-yyyyMMdd-seq)',
    INBOUND_STATUS  VARCHAR(20)     NOT NULL DEFAULT 'PLANNED'
                    COMMENT '입고상태 (COM_CODE: MCS_IB_STATUS)',
    VENDOR_CD       VARCHAR(50)     DEFAULT NULL COMMENT '거래처코드',
    WAREHOUSE_CD    VARCHAR(20)     DEFAULT NULL COMMENT '입고 대상 창고',
    EXPECTED_DT     DATE            DEFAULT NULL COMMENT '입고예정일',
    ACTUAL_DT       DATETIME        DEFAULT NULL COMMENT '실제입고일시',
    RECEIVE_PLAN_ID BIGINT          DEFAULT NULL
                    COMMENT 'MES INV_RECEIVE_PLAN.RECEIVE_PLAN_ID 연동',
    INBOUND_RMK     VARCHAR(500)    DEFAULT NULL COMMENT '비고',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (INBOUND_ID),
    UNIQUE KEY UK_MCS_INBOUND (INBOUND_NO),
    KEY IDX_MCS_INBOUND_01 (PLANT_CD, EXPECTED_DT),
    KEY IDX_MCS_INBOUND_02 (INBOUND_STATUS),
    KEY IDX_MCS_INBOUND_03 (VENDOR_CD),
    CONSTRAINT FK_MCS_INBOUND_PLANT FOREIGN KEY (PLANT_CD) REFERENCES MST_PLANT(PLANT_CD),
    CONSTRAINT FK_MCS_INBOUND_VENDOR FOREIGN KEY (VENDOR_CD) REFERENCES MST_VENDOR(VENDOR_CD),
    CONSTRAINT FK_MCS_INBOUND_WH FOREIGN KEY (WAREHOUSE_CD) REFERENCES MST_WAREHOUSE(WAREHOUSE_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 입고오더';

-- ============================================================
-- 9. MCS_INBOUND_ITEM - 입고 품목
-- ============================================================
CREATE TABLE MCS_INBOUND_ITEM (
    INBOUND_ITEM_ID BIGINT          NOT NULL AUTO_INCREMENT COMMENT '입고품목ID',
    INBOUND_ID      BIGINT          NOT NULL COMMENT '입고ID',
    ITEM_CD         VARCHAR(50)     NOT NULL COMMENT '품목코드',
    LOT_NO          VARCHAR(50)     DEFAULT NULL COMMENT 'LOT번호 (입고확정 시 생성/지정)',
    LOCATION_ID     BIGINT          DEFAULT NULL COMMENT '적치 로케이션',
    EXPECTED_QTY    DECIMAL(15,3)   NOT NULL COMMENT '예정수량',
    ACTUAL_QTY      DECIMAL(15,3)   DEFAULT 0.000 COMMENT '실제수량',
    ITEM_STATUS     VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                    COMMENT '품목상태 (COM_CODE: MCS_IB_ITEM_STATUS)',
    ITEM_RMK        VARCHAR(500)    DEFAULT NULL COMMENT '비고',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (INBOUND_ITEM_ID),
    KEY IDX_MCS_IB_ITEM_01 (INBOUND_ID),
    KEY IDX_MCS_IB_ITEM_02 (ITEM_CD),
    CONSTRAINT FK_MCS_IB_ITEM_ORDER FOREIGN KEY (INBOUND_ID) REFERENCES MCS_INBOUND_ORDER(INBOUND_ID),
    CONSTRAINT FK_MCS_IB_ITEM_ITEM FOREIGN KEY (ITEM_CD) REFERENCES MST_ITEM(ITEM_CD),
    CONSTRAINT FK_MCS_IB_ITEM_LOC FOREIGN KEY (LOCATION_ID) REFERENCES MCS_LOCATION(LOCATION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 입고품목';

-- ============================================================
-- 10. MCS_OUTBOUND_ORDER - 출고 오더
--     (MES INV_ISSUE_PLAN과 연동 가능)
-- ============================================================
CREATE TABLE MCS_OUTBOUND_ORDER (
    OUTBOUND_ID     BIGINT          NOT NULL AUTO_INCREMENT COMMENT '출고ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    OUTBOUND_NO     VARCHAR(30)     NOT NULL COMMENT '출고번호 (채번: MCS-OB-yyyyMMdd-seq)',
    OUTBOUND_STATUS VARCHAR(20)     NOT NULL DEFAULT 'REQUESTED'
                    COMMENT '출고상태 (COM_CODE: MCS_OB_STATUS)',
    CUSTOMER_CD     VARCHAR(50)     DEFAULT NULL COMMENT '고객코드 (MST_VENDOR)',
    WAREHOUSE_CD    VARCHAR(20)     DEFAULT NULL COMMENT '출고 원천 창고',
    REQUEST_DT      DATETIME        DEFAULT NULL COMMENT '요청일시',
    SHIPPED_DT      DATETIME        DEFAULT NULL COMMENT '출하일시',
    DESTINATION     VARCHAR(300)    DEFAULT NULL COMMENT '목적지',
    ISSUE_PLAN_ID   BIGINT          DEFAULT NULL
                    COMMENT 'MES INV_ISSUE_PLAN.ISSUE_PLAN_ID 연동',
    WO_ID           BIGINT          DEFAULT NULL
                    COMMENT 'MES PLN_WORK_ORDER.WO_ID 연동 (생산출고 시)',
    OUTBOUND_RMK    VARCHAR(500)    DEFAULT NULL COMMENT '비고',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (OUTBOUND_ID),
    UNIQUE KEY UK_MCS_OUTBOUND (OUTBOUND_NO),
    KEY IDX_MCS_OUTBOUND_01 (PLANT_CD, REQUEST_DT),
    KEY IDX_MCS_OUTBOUND_02 (OUTBOUND_STATUS),
    KEY IDX_MCS_OUTBOUND_03 (WO_ID),
    CONSTRAINT FK_MCS_OUTBOUND_PLANT FOREIGN KEY (PLANT_CD) REFERENCES MST_PLANT(PLANT_CD),
    CONSTRAINT FK_MCS_OUTBOUND_WH FOREIGN KEY (WAREHOUSE_CD) REFERENCES MST_WAREHOUSE(WAREHOUSE_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 출고오더';

-- ============================================================
-- 11. MCS_OUTBOUND_ITEM - 출고 품목
-- ============================================================
CREATE TABLE MCS_OUTBOUND_ITEM (
    OUTBOUND_ITEM_ID BIGINT         NOT NULL AUTO_INCREMENT COMMENT '출고품목ID',
    OUTBOUND_ID     BIGINT          NOT NULL COMMENT '출고ID',
    ITEM_CD         VARCHAR(50)     NOT NULL COMMENT '품목코드',
    LOT_NO          VARCHAR(50)     DEFAULT NULL COMMENT 'LOT번호',
    LOCATION_ID     BIGINT          DEFAULT NULL COMMENT '출고 원천 로케이션',
    REQUESTED_QTY   DECIMAL(15,3)   NOT NULL COMMENT '요청수량',
    ALLOCATED_QTY   DECIMAL(15,3)   DEFAULT 0.000 COMMENT '할당수량',
    PICKED_QTY      DECIMAL(15,3)   DEFAULT 0.000 COMMENT '피킹수량',
    SHIPPED_QTY     DECIMAL(15,3)   DEFAULT 0.000 COMMENT '출하수량',
    ITEM_STATUS     VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                    COMMENT '품목상태 (COM_CODE: MCS_OB_ITEM_STATUS)',
    ITEM_RMK        VARCHAR(500)    DEFAULT NULL COMMENT '비고',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (OUTBOUND_ITEM_ID),
    KEY IDX_MCS_OB_ITEM_01 (OUTBOUND_ID),
    KEY IDX_MCS_OB_ITEM_02 (ITEM_CD),
    CONSTRAINT FK_MCS_OB_ITEM_ORDER FOREIGN KEY (OUTBOUND_ID) REFERENCES MCS_OUTBOUND_ORDER(OUTBOUND_ID),
    CONSTRAINT FK_MCS_OB_ITEM_ITEM FOREIGN KEY (ITEM_CD) REFERENCES MST_ITEM(ITEM_CD),
    CONSTRAINT FK_MCS_OB_ITEM_LOC FOREIGN KEY (LOCATION_ID) REFERENCES MCS_LOCATION(LOCATION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 출고품목';

-- ============================================================
-- 12. MCS_TRANSFER_ORDER - 이동 오더
-- ============================================================
CREATE TABLE MCS_TRANSFER_ORDER (
    TRANSFER_ID     BIGINT          NOT NULL AUTO_INCREMENT COMMENT '이동ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    TRANSFER_NO     VARCHAR(30)     NOT NULL COMMENT '이동번호 (채번: MCS-TF-yyyyMMdd-seq)',
    TRANSFER_STATUS VARCHAR(20)     NOT NULL DEFAULT 'REQUESTED'
                    COMMENT '이동상태 (COM_CODE: MCS_TF_STATUS)',
    FROM_LOCATION_ID BIGINT         NOT NULL COMMENT '출발 로케이션',
    TO_LOCATION_ID  BIGINT          NOT NULL COMMENT '도착 로케이션',
    TRANSFER_REASON VARCHAR(500)    DEFAULT NULL COMMENT '이동사유',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (TRANSFER_ID),
    UNIQUE KEY UK_MCS_TRANSFER (TRANSFER_NO),
    KEY IDX_MCS_TRANSFER_01 (PLANT_CD, TRANSFER_STATUS),
    CONSTRAINT FK_MCS_TRANSFER_PLANT FOREIGN KEY (PLANT_CD) REFERENCES MST_PLANT(PLANT_CD),
    CONSTRAINT FK_MCS_TRANSFER_FROM FOREIGN KEY (FROM_LOCATION_ID) REFERENCES MCS_LOCATION(LOCATION_ID),
    CONSTRAINT FK_MCS_TRANSFER_TO FOREIGN KEY (TO_LOCATION_ID) REFERENCES MCS_LOCATION(LOCATION_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 이동오더';

-- ============================================================
-- 13. MCS_TRANSFER_ITEM - 이동 품목
-- ============================================================
CREATE TABLE MCS_TRANSFER_ITEM (
    TRANSFER_ITEM_ID BIGINT         NOT NULL AUTO_INCREMENT COMMENT '이동품목ID',
    TRANSFER_ID     BIGINT          NOT NULL COMMENT '이동ID',
    ITEM_CD         VARCHAR(50)     NOT NULL COMMENT '품목코드',
    LOT_NO          VARCHAR(50)     DEFAULT NULL COMMENT 'LOT번호',
    TRANSFER_QTY    DECIMAL(15,3)   NOT NULL COMMENT '이동수량',
    ITEM_STATUS     VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                    COMMENT '품목상태 (COM_CODE: MCS_TF_ITEM_STATUS)',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (TRANSFER_ITEM_ID),
    KEY IDX_MCS_TF_ITEM_01 (TRANSFER_ID),
    CONSTRAINT FK_MCS_TF_ITEM_ORDER FOREIGN KEY (TRANSFER_ID) REFERENCES MCS_TRANSFER_ORDER(TRANSFER_ID),
    CONSTRAINT FK_MCS_TF_ITEM_ITEM FOREIGN KEY (ITEM_CD) REFERENCES MST_ITEM(ITEM_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 이동품목';

SET FOREIGN_KEY_CHECKS = 1;
```

---

## 5. ERD (연동 관계)

```
┌─────────── MES 기존 테이블 (참조만) ──────────────┐
│                                                      │
│  MST_PLANT ◄── MST_WAREHOUSE ◄── MST_VENDOR         │
│      │              │                                 │
│  MST_ITEM ◄── INV_LOT    INV_STOCK  INV_TRANS_HIS   │
│      │              │       ↕ 동기화     ↕ 동기화      │
│  COM_CODE      COM_SEQ_NO                             │
│                                                      │
└──────────────────────────────────────────────────────┘
         │              │             │
         ▼              ▼             ▼
┌─────────── MCS 신규 테이블 ──────────────────────────┐
│                                                      │
│  MCS_ZONE ◄── MCS_LOCATION ◄── MCS_LOCATION_STOCK   │
│                     │                 │               │
│                     │          MCS_LOC_TRANS_HIS      │
│                     │                                 │
│  MCS_INBOUND_ORDER ──► MCS_INBOUND_ITEM              │
│      (→ INV_RECEIVE_PLAN 연동)      │→ LOCATION_ID   │
│                                                      │
│  MCS_OUTBOUND_ORDER ──► MCS_OUTBOUND_ITEM            │
│      (→ INV_ISSUE_PLAN 연동)        │→ LOCATION_ID   │
│      (→ PLN_WORK_ORDER 연동)                          │
│                                                      │
│  MCS_TRANSFER_ORDER ──► MCS_TRANSFER_ITEM            │
│      FROM_LOCATION / TO_LOCATION                     │
└──────────────────────────────────────────────────────┘
```

---

## 6. MES ↔ MCS 연동 로직

### 6.1 입고 확정 시 (MCS → MES 동기화)

```java
/**
 * MCS 입고 확정 시 처리 흐름:
 * 1. MCS_INBOUND_ITEM → STOCKED 상태 변경
 * 2. MCS_LOCATION_STOCK → 재고 증가 (UPSERT)
 * 3. MCS_LOC_TRANS_HIS → 이력 기록
 * 4. INV_STOCK → MES 창고 레벨 재고 동기화 (증가)
 *    ★ LOCATION_ID(BIGINT) → LOCATION_CD(VARCHAR) 변환 필요
 *      MCS_LOCATION.LOCATION_CD를 조회하여 INV_STOCK.LOCATION_CD에 매핑
 * 5. INV_TRANS_HIS → MES 입출고 이력 기록
 *    ★ TRANS_NO 채번 시 MCS-IB- 접두어 사용 (MES 채번과 충돌 방지)
 * 6. INV_LOT → LOT 미존재 시 신규 생성
 */
@Transactional
public void completeInbound(Long inboundId) {
    // ... (상세 구현은 서비스에서)
}
```

### 6.2 출고 확정 시 (MCS → MES 동기화)

```java
/**
 * MCS 출고(출하) 확정 시 처리 흐름:
 * 1. MCS_OUTBOUND_ITEM → SHIPPED 상태 변경
 * 2. MCS_LOCATION_STOCK → 재고 차감
 * 3. MCS_LOC_TRANS_HIS → 이력 기록
 * 4. INV_STOCK → MES 창고 레벨 재고 동기화 (차감)
 *    ★ LOCATION_ID(BIGINT) → LOCATION_CD(VARCHAR) 변환 필요
 * 5. INV_TRANS_HIS → MES 입출고 이력 기록
 *    ★ TRANS_NO 채번 시 MCS-OB- 접두어 사용 (MES 채번과 충돌 방지)
 */
@Transactional
public void shipOutbound(Long outboundId) {
    // ...
}
```

### 6.3 생산 투입/입고 연동 (MES → MCS)

```java
/**
 * MES 작업지시(PLN_WORK_ORDER)에서 자재 투입 시:
 * 0. ★ WO_ID 존재 여부를 PLN_WORK_ORDER 테이블에서 검증 (FK 미설정이므로 서비스 레벨 검증 필수)
 * 1. MCS_OUTBOUND_ORDER 자동 생성 (WO_ID 연동)
 * 2. MCS에서 로케이션 레벨 피킹 처리
 * 3. MES INV_STOCK / INV_TRANS_HIS 동기화
 *    ★ TRANS_NO 채번 시 MCS-TF- 접두어 사용 (MES 채번과 충돌 방지)
 */
```

---

## 7. MyBatis Mapper 예시 (변경된 구조)

### 7.1 로케이션 재고 조회 - InventoryMapper.xml

```xml
<mapper namespace="com.mcs.domain.inventory.mapper.InventoryMapper">

    <!-- 품목별 로케이션 재고 상세 조회 -->
    <select id="selectLocationStock"
            resultType="com.mcs.domain.inventory.dto.LocationStockDto">
        SELECT
            ls.LOC_STOCK_ID,
            p.PLANT_NM,
            wh.WAREHOUSE_NM,
            z.ZONE_NM,
            z.ZONE_TYPE,
            l.LOCATION_CD,
            l.LOCATION_NM,
            i.ITEM_CD,
            i.ITEM_NM,
            i.ITEM_TYPE,
            i.UNIT,
            ls.LOT_NO,
            lot.LOT_STATUS,
            lot.EXPIRE_DT,
            ls.STOCK_QTY,
            ls.RESERVED_QTY,
            ls.AVAILABLE_QTY,
            ls.UPD_DTM
        FROM MCS_LOCATION_STOCK ls
            JOIN MCS_LOCATION l   ON ls.LOCATION_ID = l.LOCATION_ID
            JOIN MCS_ZONE z       ON l.ZONE_ID = z.ZONE_ID
            JOIN MST_WAREHOUSE wh ON z.WAREHOUSE_CD = wh.WAREHOUSE_CD
            JOIN MST_PLANT p      ON ls.PLANT_CD = p.PLANT_CD
            JOIN MST_ITEM i       ON ls.ITEM_CD = i.ITEM_CD
            LEFT JOIN INV_LOT lot ON ls.LOT_NO = lot.LOT_NO
        <where>
            ls.STOCK_QTY > 0
            <if test="plantCd != null and plantCd != ''">
                AND ls.PLANT_CD = #{plantCd}
            </if>
            <if test="warehouseCd != null and warehouseCd != ''">
                AND wh.WAREHOUSE_CD = #{warehouseCd}
            </if>
            <if test="itemCd != null and itemCd != ''">
                AND ls.ITEM_CD LIKE CONCAT('%', #{itemCd}, '%')
            </if>
            <if test="itemNm != null and itemNm != ''">
                AND i.ITEM_NM LIKE CONCAT('%', #{itemNm}, '%')
            </if>
            <if test="lotNo != null and lotNo != ''">
                AND ls.LOT_NO LIKE CONCAT('%', #{lotNo}, '%')
            </if>
        </where>
        ORDER BY wh.WAREHOUSE_NM, z.ZONE_NM, l.LOCATION_CD
        LIMIT #{size} OFFSET #{offset}
    </select>

    <!-- 로케이션 재고 증가 (UPSERT) -->
    <insert id="increaseLocationStock">
        INSERT INTO MCS_LOCATION_STOCK
            (PLANT_CD, LOCATION_ID, ITEM_CD, LOT_NO, STOCK_QTY, REG_USER_ID)
        VALUES
            (#{plantCd}, #{locationId}, #{itemCd}, #{lotNo}, #{qty}, #{userId})
        ON DUPLICATE KEY UPDATE
            STOCK_QTY = STOCK_QTY + #{qty},
            UPD_USER_ID = #{userId},
            UPD_DTM = NOW()
    </insert>

    <!-- 로케이션 재고 차감 (가용수량 체크 포함) -->
    <update id="decreaseLocationStock">
        UPDATE MCS_LOCATION_STOCK
        SET STOCK_QTY = STOCK_QTY - #{qty},
            UPD_USER_ID = #{userId},
            UPD_DTM = NOW()
        WHERE LOCATION_ID = #{locationId}
          AND ITEM_CD = #{itemCd}
          AND LOT_NO = #{lotNo}
          AND (STOCK_QTY - COALESCE(RESERVED_QTY, 0)) >= #{qty}
    </update>

    <!--
        MES INV_STOCK 동기화 (입고 시)
        ★ locationCd는 MCS_LOCATION.LOCATION_CD(VARCHAR)를 서비스에서 조회하여 전달
           (MCS 내부는 LOCATION_ID(BIGINT)로 관리하므로 변환 필요)
        ★ UK_INV_STOCK = (PLANT_CD, WAREHOUSE_CD, ITEM_CD, LOT_NO, LOCATION_CD)
    -->
    <insert id="syncMesStockIncrease">
        INSERT INTO INV_STOCK
            (PLANT_CD, WAREHOUSE_CD, LOCATION_CD, ITEM_CD, LOT_NO,
             STOCK_QTY, UNIT, STOCK_STATUS, LAST_IN_DT, REG_USER_ID)
        VALUES
            (#{plantCd}, #{warehouseCd}, #{locationCd}, #{itemCd}, #{lotNo},
             #{qty}, #{unit}, '정상', CURDATE(), #{userId})
        ON DUPLICATE KEY UPDATE
            STOCK_QTY = STOCK_QTY + #{qty},
            LAST_IN_DT = CURDATE(),
            UPD_USER_ID = #{userId},
            UPD_DTM = NOW()
    </insert>

    <!--
        MES INV_TRANS_HIS 이력 기록
        ★ TRANS_NO는 MCS 전용 접두어 사용 (MCS-IB-, MCS-OB-, MCS-TF-)
           MES 채번(WO, PP 등)과 충돌 방지
    -->
    <insert id="insertMesTransHis"
            parameterType="com.mcs.domain.inventory.dto.MesTransHisDto">
        INSERT INTO INV_TRANS_HIS (
            PLANT_CD, TRANS_NO, TRANS_DT, TRANS_TYPE, TRANS_REASON,
            ITEM_CD, LOT_NO, TRANS_QTY, UNIT,
            FROM_WAREHOUSE_CD, TO_WAREHOUSE_CD,
            FROM_LOCATION_CD, TO_LOCATION_CD,
            BEFORE_QTY, AFTER_QTY,
            REF_TYPE, REF_NO, VENDOR_CD,
            TRANS_USER_ID, TRANS_RMK, REG_USER_ID
        ) VALUES (
            #{plantCd}, #{transNo}, #{transDt}, #{transType}, #{transReason},
            #{itemCd}, #{lotNo}, #{transQty}, #{unit},
            #{fromWarehouseCd}, #{toWarehouseCd},
            #{fromLocationCd}, #{toLocationCd},
            #{beforeQty}, #{afterQty},
            #{refType}, #{refNo}, #{vendorCd},
            #{transUserId}, #{transRmk}, #{regUserId}
        )
    </insert>

</mapper>
```

### 7.2 MES 품목 조회 - MesItemMapper.xml

```xml
<mapper namespace="com.mcs.domain.mes.mapper.MesItemMapper">

    <!-- MES 품목 마스터 조회 (MCS에서 참조용) -->
    <select id="selectItemList"
            resultType="com.mcs.domain.mes.dto.ItemDto">
        SELECT
            i.ITEM_CD,
            i.PLANT_CD,
            i.ITEM_NM,
            i.ITEM_SPEC,
            i.ITEM_TYPE,
            i.ITEM_GRP,
            i.UNIT,
            i.SAFETY_STOCK_QTY,
            i.MAIN_VENDOR_CD,
            v.VENDOR_NM AS MAIN_VENDOR_NM
        FROM MST_ITEM i
            LEFT JOIN MST_VENDOR v ON i.MAIN_VENDOR_CD = v.VENDOR_CD
        <where>
            i.USE_YN = 'Y'
            <if test="plantCd != null">AND i.PLANT_CD = #{plantCd}</if>
            <if test="itemCd != null">AND i.ITEM_CD LIKE CONCAT('%', #{itemCd}, '%')</if>
            <if test="itemNm != null">AND i.ITEM_NM LIKE CONCAT('%', #{itemNm}, '%')</if>
            <if test="itemType != null">AND i.ITEM_TYPE = #{itemType}</if>
        </where>
        ORDER BY i.ITEM_CD
    </select>

</mapper>
```

---

## 8. REST API (변경)

### 8.1 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| **MES 마스터 조회 (읽기 전용)** |
| GET | `/api/mes/items` | MES 품목 조회 |
| GET | `/api/mes/warehouses` | MES 창고 조회 |
| GET | `/api/mes/vendors` | MES 거래처 조회 |
| GET | `/api/mes/codes/{grpCd}` | 공통코드 조회 |
| **구역 관리** |
| GET | `/api/zones` | 구역 목록 |
| POST | `/api/zones` | 구역 등록 |
| PUT | `/api/zones/{zoneId}` | 구역 수정 |
| **로케이션 관리** |
| GET | `/api/locations` | 로케이션 목록 |
| POST | `/api/locations` | 로케이션 등록 |
| PUT | `/api/locations/{locationId}` | 로케이션 수정 |
| **입고** |
| GET | `/api/inbounds` | 입고 목록 |
| GET | `/api/inbounds/{id}` | 입고 상세 |
| POST | `/api/inbounds` | 입고 등록 |
| PUT | `/api/inbounds/{id}/arrive` | 도착 처리 |
| PUT | `/api/inbounds/{id}/inspect` | 검수 처리 |
| PUT | `/api/inbounds/{id}/complete` | 입고 확정 (→ MES 연동) |
| PUT | `/api/inbounds/{id}/cancel` | 입고 취소 |
| **출고** |
| GET | `/api/outbounds` | 출고 목록 |
| GET | `/api/outbounds/{id}` | 출고 상세 |
| POST | `/api/outbounds` | 출고 요청 |
| PUT | `/api/outbounds/{id}/allocate` | 재고 할당 |
| PUT | `/api/outbounds/{id}/pick` | 피킹 완료 |
| PUT | `/api/outbounds/{id}/ship` | 출하 확정 (→ MES 연동) |
| PUT | `/api/outbounds/{id}/cancel` | 출고 취소 |
| **이동** |
| GET | `/api/transfers` | 이동 목록 |
| POST | `/api/transfers` | 이동 요청 |
| PUT | `/api/transfers/{id}/execute` | 이동 실행 (→ MES 연동) |
| PUT | `/api/transfers/{id}/cancel` | 이동 취소 |
| **재고** |
| GET | `/api/inventory/location-stock` | 로케이션별 재고 |
| GET | `/api/inventory/summary` | 품목별 재고 요약 |
| POST | `/api/inventory/adjust` | 재고 조정 (→ MES 연동) |
| GET | `/api/inventory/transactions` | 로케이션 재고 이력 |

---

## 9. application.yml (변경)

```yaml
server:
  port: 8081                                # MES가 8080이면 MCS는 8081
  servlet:
    context-path: /mcs/api

spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/MES_DB  # ★ MES와 동일 DB
    username: mcs_user
    password: mcs_password
    driver-class-name: org.mariadb.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.mcs.domain
  configuration:
    map-underscore-to-camel-case: true
    jdbc-type-for-null: NULL
    default-fetch-size: 100
    default-statement-timeout: 30

logging:
  level:
    com.mcs: DEBUG
```

---

## 10. 구현 우선순위 (수정)

| 단계 | 기능 | 핵심 포인트 |
|------|------|------------|
| **Phase 1** | DB 스키마 생성 + 프로젝트 셋업 | MCS_ 테이블 + COM_CODE 추가, MES 마스터 조회 Mapper |
| **Phase 2** | Zone / Location CRUD | MES 창고 하위에 구역/로케이션 생성 |
| **Phase 3** | 로케이션 재고 + 재고 조정 | MCS_LOCATION_STOCK CRUD, INV_STOCK 동기화 |
| **Phase 4** | 입고 워크플로 | PLANNED→COMPLETED + LOT 생성 + MES 연동 |
| **Phase 5** | 출고 워크플로 | REQUESTED→SHIPPED + 예약/피킹 + MES 연동 |
| **Phase 6** | 이동 관리 | 로케이션 간 이동 + 양쪽 재고 반영 |

---

## 11. MES 연동 주의사항

### 11.1 채번 충돌 방지
- MES는 `WO`, `PP`, `QC` 등의 접두어로 `COM_SEQ_NO` 채번 사용
- MCS는 반드시 **MCS- 접두어**로 채번하여 `INV_TRANS_HIS.TRANS_NO`(UNIQUE KEY) 충돌 방지
  - 입고: `MCS-IB-yyyyMMdd-seq`
  - 출고: `MCS-OB-yyyyMMdd-seq`
  - 이동: `MCS-TF-yyyyMMdd-seq`

### 11.2 LOCATION_ID ↔ LOCATION_CD 매핑
- MCS 내부: `LOCATION_ID`(BIGINT) 기반 관리
- MES `INV_STOCK.LOCATION_CD`: `VARCHAR(50)` 문자열 코드
- MCS 서비스에서 MES 재고 동기화 시 반드시 `MCS_LOCATION.LOCATION_CD`를 조회하여 변환 후 전달

### 11.3 PLN_WORK_ORDER 참조 검증
- `MCS_OUTBOUND_ORDER.WO_ID`는 FK 제약조건 미설정 (의도적 느슨 연동)
- 서비스 레이어에서 `PLN_WORK_ORDER` 테이블의 `WO_ID` 존재 여부를 반드시 검증
- 존재하지 않는 `WO_ID` 입력 시 `BusinessException` 발생 처리

### 11.4 DB 환경
- MES와 동일 DB 인스턴스 사용: `MES_DB`
- MCS 전용 테이블은 모두 `MCS_` 접두어로 구분
- MES 기존 테이블은 **읽기/동기화만** 수행하며 스키마 변경 금지
