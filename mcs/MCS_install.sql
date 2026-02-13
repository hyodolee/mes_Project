-- ============================================================
-- MCS (Material Control System) 설치 스크립트
-- 대상 DB: mes_backend_server (MES와 동일 DB 인스턴스)
-- 실행 순서: DDL → 공통코드 → 더미 데이터
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ************************************************************
-- PART 1: DDL - MCS 전용 테이블 생성
-- ************************************************************

-- ============================================================
-- 1-1. MCS_ZONE - 구역
-- ============================================================
DROP TABLE IF EXISTS MCS_TRANSFER_ITEM;
DROP TABLE IF EXISTS MCS_TRANSFER_ORDER;
DROP TABLE IF EXISTS MCS_OUTBOUND_ITEM;
DROP TABLE IF EXISTS MCS_OUTBOUND_ORDER;
DROP TABLE IF EXISTS MCS_INBOUND_ITEM;
DROP TABLE IF EXISTS MCS_INBOUND_ORDER;
DROP TABLE IF EXISTS MCS_LOC_TRANS_HIS;
DROP TABLE IF EXISTS MCS_LOCATION_STOCK;
DROP TABLE IF EXISTS MCS_LOCATION;
DROP TABLE IF EXISTS MCS_ZONE;

CREATE TABLE MCS_ZONE (
    ZONE_ID         BIGINT          NOT NULL AUTO_INCREMENT COMMENT '구역ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    WAREHOUSE_CD    VARCHAR(20)     NOT NULL COMMENT '창고코드',
    ZONE_CD         VARCHAR(20)     NOT NULL COMMENT '구역코드',
    ZONE_NM         VARCHAR(100)    NOT NULL COMMENT '구역명',
    ZONE_TYPE       VARCHAR(20)     NOT NULL DEFAULT 'STORAGE'
                    COMMENT '구역유형 (MCS_ZONE_TYPE)',
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
-- 1-2. MCS_LOCATION - 로케이션
-- ============================================================
CREATE TABLE MCS_LOCATION (
    LOCATION_ID     BIGINT          NOT NULL AUTO_INCREMENT COMMENT '로케이션ID',
    ZONE_ID         BIGINT          NOT NULL COMMENT '구역ID',
    LOCATION_CD     VARCHAR(30)     NOT NULL COMMENT '로케이션코드',
    LOCATION_NM     VARCHAR(100)    DEFAULT NULL COMMENT '로케이션명',
    MAX_CAPACITY    DECIMAL(15,3)   DEFAULT 0.000 COMMENT '최대수용량',
    CURRENT_USAGE   DECIMAL(15,3)   DEFAULT 0.000 COMMENT '현재사용량',
    LOCATION_STATUS VARCHAR(20)     NOT NULL DEFAULT 'EMPTY'
                    COMMENT '로케이션상태 (MCS_LOC_STATUS)',
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
-- 1-3. MCS_LOCATION_STOCK - 로케이션 레벨 재고
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
-- 1-4. MCS_LOC_TRANS_HIS - 로케이션 재고 트랜잭션
-- ============================================================
CREATE TABLE MCS_LOC_TRANS_HIS (
    LOC_TRANS_ID    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '로케이션거래ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    LOC_STOCK_ID    BIGINT          NOT NULL COMMENT '로케이션재고ID',
    TRANS_TYPE      VARCHAR(20)     NOT NULL COMMENT '거래유형 (MCS_INV_TX_TYPE)',
    TRANS_QTY       DECIMAL(15,3)   NOT NULL COMMENT '변동수량',
    BEFORE_QTY      DECIMAL(15,3)   NOT NULL COMMENT '변경전수량',
    AFTER_QTY       DECIMAL(15,3)   NOT NULL COMMENT '변경후수량',
    REF_TYPE        VARCHAR(20)     DEFAULT NULL COMMENT '참조유형 (IB/OB/TF/ADJ)',
    REF_NO          VARCHAR(50)     DEFAULT NULL COMMENT '참조번호',
    REF_ID          BIGINT          DEFAULT NULL COMMENT '참조ID',
    TRANS_RMK       VARCHAR(500)    DEFAULT NULL COMMENT '비고',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (LOC_TRANS_ID),
    KEY IDX_MCS_LOC_TRANS_01 (PLANT_CD, REG_DTM),
    KEY IDX_MCS_LOC_TRANS_02 (LOC_STOCK_ID),
    KEY IDX_MCS_LOC_TRANS_03 (REF_TYPE, REF_NO),
    CONSTRAINT FK_MCS_LOC_TRANS_PLANT FOREIGN KEY (PLANT_CD) REFERENCES MST_PLANT(PLANT_CD),
    CONSTRAINT FK_MCS_LOC_TRANS_STOCK FOREIGN KEY (LOC_STOCK_ID) REFERENCES MCS_LOCATION_STOCK(LOC_STOCK_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 로케이션재고이력';

-- ============================================================
-- 1-5. MCS_INBOUND_ORDER - 입고 오더
-- ============================================================
CREATE TABLE MCS_INBOUND_ORDER (
    INBOUND_ID      BIGINT          NOT NULL AUTO_INCREMENT COMMENT '입고ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    INBOUND_NO      VARCHAR(30)     NOT NULL COMMENT '입고번호',
    INBOUND_STATUS  VARCHAR(20)     NOT NULL DEFAULT 'PLANNED'
                    COMMENT '입고상태 (MCS_IB_STATUS)',
    VENDOR_CD       VARCHAR(50)     DEFAULT NULL COMMENT '거래처코드',
    WAREHOUSE_CD    VARCHAR(20)     DEFAULT NULL COMMENT '입고 대상 창고',
    EXPECTED_DT     DATE            DEFAULT NULL COMMENT '입고예정일',
    ACTUAL_DT       DATETIME        DEFAULT NULL COMMENT '실제입고일시',
    RECEIVE_PLAN_ID BIGINT          DEFAULT NULL COMMENT 'MES INV_RECEIVE_PLAN 연동ID',
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
-- 1-6. MCS_INBOUND_ITEM - 입고 품목
-- ============================================================
CREATE TABLE MCS_INBOUND_ITEM (
    INBOUND_ITEM_ID BIGINT          NOT NULL AUTO_INCREMENT COMMENT '입고품목ID',
    INBOUND_ID      BIGINT          NOT NULL COMMENT '입고ID',
    ITEM_CD         VARCHAR(50)     NOT NULL COMMENT '품목코드',
    LOT_NO          VARCHAR(50)     DEFAULT NULL COMMENT 'LOT번호',
    LOCATION_ID     BIGINT          DEFAULT NULL COMMENT '적치 로케이션',
    EXPECTED_QTY    DECIMAL(15,3)   NOT NULL COMMENT '예정수량',
    ACTUAL_QTY      DECIMAL(15,3)   DEFAULT 0.000 COMMENT '실제수량',
    ITEM_STATUS     VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                    COMMENT '품목상태 (MCS_IB_ITEM_STATUS)',
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
-- 1-7. MCS_OUTBOUND_ORDER - 출고 오더
-- ============================================================
CREATE TABLE MCS_OUTBOUND_ORDER (
    OUTBOUND_ID     BIGINT          NOT NULL AUTO_INCREMENT COMMENT '출고ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    OUTBOUND_NO     VARCHAR(30)     NOT NULL COMMENT '출고번호',
    OUTBOUND_STATUS VARCHAR(20)     NOT NULL DEFAULT 'REQUESTED'
                    COMMENT '출고상태 (MCS_OB_STATUS)',
    CUSTOMER_CD     VARCHAR(50)     DEFAULT NULL COMMENT '고객코드',
    WAREHOUSE_CD    VARCHAR(20)     DEFAULT NULL COMMENT '출고 원천 창고',
    REQUEST_DT      DATETIME        DEFAULT NULL COMMENT '요청일시',
    SHIPPED_DT      DATETIME        DEFAULT NULL COMMENT '출하일시',
    DESTINATION     VARCHAR(300)    DEFAULT NULL COMMENT '목적지',
    ISSUE_PLAN_ID   BIGINT          DEFAULT NULL COMMENT 'MES INV_ISSUE_PLAN 연동ID',
    WO_ID           BIGINT          DEFAULT NULL COMMENT 'MES PLN_WORK_ORDER 연동ID',
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
-- 1-8. MCS_OUTBOUND_ITEM - 출고 품목
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
                    COMMENT '품목상태 (MCS_OB_ITEM_STATUS)',
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
-- 1-9. MCS_TRANSFER_ORDER - 이동 오더
-- ============================================================
CREATE TABLE MCS_TRANSFER_ORDER (
    TRANSFER_ID     BIGINT          NOT NULL AUTO_INCREMENT COMMENT '이동ID',
    PLANT_CD        VARCHAR(20)     NOT NULL COMMENT '공장코드',
    TRANSFER_NO     VARCHAR(30)     NOT NULL COMMENT '이동번호',
    TRANSFER_STATUS VARCHAR(20)     NOT NULL DEFAULT 'REQUESTED'
                    COMMENT '이동상태 (MCS_TF_STATUS)',
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
-- 1-10. MCS_TRANSFER_ITEM - 이동 품목
-- ============================================================
CREATE TABLE MCS_TRANSFER_ITEM (
    TRANSFER_ITEM_ID BIGINT         NOT NULL AUTO_INCREMENT COMMENT '이동품목ID',
    TRANSFER_ID     BIGINT          NOT NULL COMMENT '이동ID',
    ITEM_CD         VARCHAR(50)     NOT NULL COMMENT '품목코드',
    LOT_NO          VARCHAR(50)     DEFAULT NULL COMMENT 'LOT번호',
    TRANSFER_QTY    DECIMAL(15,3)   NOT NULL COMMENT '이동수량',
    ITEM_STATUS     VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                    COMMENT '품목상태 (MCS_TF_ITEM_STATUS)',
    REG_USER_ID     VARCHAR(50)     NOT NULL COMMENT '등록자ID',
    REG_DTM         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    UPD_USER_ID     VARCHAR(50)     DEFAULT NULL COMMENT '수정자ID',
    UPD_DTM         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    PRIMARY KEY (TRANSFER_ITEM_ID),
    KEY IDX_MCS_TF_ITEM_01 (TRANSFER_ID),
    CONSTRAINT FK_MCS_TF_ITEM_ORDER FOREIGN KEY (TRANSFER_ID) REFERENCES MCS_TRANSFER_ORDER(TRANSFER_ID),
    CONSTRAINT FK_MCS_TF_ITEM_ITEM FOREIGN KEY (ITEM_CD) REFERENCES MST_ITEM(ITEM_CD)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='MCS 이동품목';


-- ************************************************************
-- PART 2: 공통코드 등록 (MES COM_CODE_GRP / COM_CODE에 추가)
-- ************************************************************

-- 그룹코드 추가
INSERT IGNORE INTO COM_CODE_GRP (GRP_CD, GRP_NM, GRP_DESC, USE_YN, REG_USER_ID, REG_DTM) VALUES
('MCS_IB_STATUS',      '입고상태',       'MCS 입고 오더 상태',         'Y', 'SYSTEM', NOW()),
('MCS_IB_ITEM_STATUS', '입고품목상태',   'MCS 입고 품목별 상태',       'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS',      '출고상태',       'MCS 출고 오더 상태',         'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', '출고품목상태',   'MCS 출고 품목별 상태',       'Y', 'SYSTEM', NOW()),
('MCS_TF_STATUS',      '이동상태',       'MCS 이동 오더 상태',         'Y', 'SYSTEM', NOW()),
('MCS_TF_ITEM_STATUS', '이동품목상태',   'MCS 이동 품목별 상태',       'Y', 'SYSTEM', NOW()),
('MCS_LOC_STATUS',     '로케이션상태',   'MCS 로케이션 상태',          'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE',      '구역유형',       'MCS 구역 유형',              'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE',    '재고거래유형',   'MCS 로케이션 재고 변동 유형', 'Y', 'SYSTEM', NOW());

-- 상세코드 추가
INSERT IGNORE INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, ATTR1, USE_YN, REG_USER_ID, REG_DTM) VALUES
-- 입고 상태
('MCS_IB_STATUS', 'PLANNED',    '입고예정', 1, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_IB_STATUS', 'ARRIVED',    '도착',     2, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_IB_STATUS', 'INSPECTING', '검수중',   3, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_IB_STATUS', 'COMPLETED',  '완료',     4, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_IB_STATUS', 'CANCELLED',  '취소',     5, NULL, 'Y', 'SYSTEM', NOW()),
-- 입고 품목 상태
('MCS_IB_ITEM_STATUS', 'PENDING',   '대기',     1, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_IB_ITEM_STATUS', 'INSPECTED', '검수완료', 2, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_IB_ITEM_STATUS', 'STOCKED',   '적치완료', 3, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_IB_ITEM_STATUS', 'REJECTED',  '반품',     4, NULL, 'Y', 'SYSTEM', NOW()),
-- 출고 상태
('MCS_OB_STATUS', 'REQUESTED', '출고요청', 1, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'ALLOCATED', '할당완료', 2, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'PICKING',   '피킹중',  3, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'PICKED',    '피킹완료', 4, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'SHIPPED',   '출하완료', 5, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_STATUS', 'CANCELLED', '취소',     6, NULL, 'Y', 'SYSTEM', NOW()),
-- 출고 품목 상태
('MCS_OB_ITEM_STATUS', 'PENDING',   '대기',     1, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', 'ALLOCATED', '할당됨',   2, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', 'PICKED',    '피킹완료', 3, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', 'SHIPPED',   '출하완료', 4, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_OB_ITEM_STATUS', 'CANCELLED', '취소',     5, NULL, 'Y', 'SYSTEM', NOW()),
-- 이동 상태
('MCS_TF_STATUS', 'REQUESTED',   '이동요청', 1, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_TF_STATUS', 'IN_PROGRESS', '이동중',   2, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_TF_STATUS', 'COMPLETED',   '완료',     3, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_TF_STATUS', 'CANCELLED',   '취소',     4, NULL, 'Y', 'SYSTEM', NOW()),
-- 이동 품목 상태
('MCS_TF_ITEM_STATUS', 'PENDING',   '대기',     1, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_TF_ITEM_STATUS', 'MOVED',     '이동완료', 2, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_TF_ITEM_STATUS', 'CANCELLED', '취소',     3, NULL, 'Y', 'SYSTEM', NOW()),
-- 로케이션 상태
('MCS_LOC_STATUS', 'EMPTY',   '비어있음', 1, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_LOC_STATUS', 'PARTIAL', '일부사용', 2, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_LOC_STATUS', 'FULL',    '가득참',   3, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_LOC_STATUS', 'BLOCKED', '사용불가', 4, NULL, 'Y', 'SYSTEM', NOW()),
-- 구역 유형
('MCS_ZONE_TYPE', 'STORAGE',   '보관구역', 1, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', 'RECEIVING', '입고구역', 2, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', 'SHIPPING',  '출하구역', 3, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', 'STAGING',   '임시구역', 4, NULL, 'Y', 'SYSTEM', NOW()),
('MCS_ZONE_TYPE', 'QC',        '검수구역', 5, NULL, 'Y', 'SYSTEM', NOW()),
-- 재고 거래 유형
('MCS_INV_TX_TYPE', 'IB_IN',       '입고적치', 1, 'IN',  'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'OB_OUT',      '출고출하', 2, 'OUT', 'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'TF_OUT',      '이동출발', 3, 'OUT', 'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'TF_IN',       '이동도착', 4, 'IN',  'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'ADJ_PLUS',    '조정증가', 5, 'IN',  'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'ADJ_MINUS',   '조정감소', 6, 'OUT', 'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'PRD_ISSUE',   '생산투입', 7, 'OUT', 'Y', 'SYSTEM', NOW()),
('MCS_INV_TX_TYPE', 'PRD_RECEIPT', '생산입고', 8, 'IN',  'Y', 'SYSTEM', NOW());


-- ************************************************************
-- PART 3: 더미 데이터 (리튬이온 배터리 제조 - battery_dummy_01~05 연동)
-- ************************************************************

-- ============================================================
-- 3-1. MCS_ZONE - 구역
--      MES 배터리 창고:
--        P001(오창1공장/셀생산): WH001(양극소재), WH002(음극소재), WH003(전해액/분리막),
--                                WH004(전극재공), WH005(셀재공), WH006(완성셀), WH007(불량품)
--        P002(대전2공장/팩조립): WH008(셀입고), WH009(팩부품), WH010(모듈재공),
--                                WH011(완성팩), WH012(불량품)
-- ============================================================
INSERT INTO MCS_ZONE (ZONE_ID, PLANT_CD, WAREHOUSE_CD, ZONE_CD, ZONE_NM, ZONE_TYPE, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
-- WH001 (P001 양극소재창고 - 항온항습)
( 1, 'P001', 'WH001', 'Z-NCM', 'NCM활물질 보관구역',         'STORAGE',   1, 'Y', 'W015'),
( 2, 'P001', 'WH001', 'Z-ALF', '알루미늄포일/첨가제 구역',   'STORAGE',   2, 'Y', 'W015'),
( 3, 'P001', 'WH001', 'Z-R1',  '입고 대기구역',              'RECEIVING', 3, 'Y', 'W015'),

-- WH002 (P001 음극소재창고 - 항온항습)
( 4, 'P001', 'WH002', 'Z-GRP', '음극활물질 보관구역',         'STORAGE',   1, 'Y', 'W015'),
( 5, 'P001', 'WH002', 'Z-CUF', '구리포일 보관구역',           'STORAGE',   2, 'Y', 'W015'),

-- WH003 (P001 전해액/분리막창고 - 방폭)
( 6, 'P001', 'WH003', 'Z-ELY', '전해액 보관구역(방폭)',       'STORAGE',   1, 'Y', 'W015'),
( 7, 'P001', 'WH003', 'Z-SEP', '분리막/NMP 보관구역',         'STORAGE',   2, 'Y', 'W015'),
( 8, 'P001', 'WH003', 'Z-QC1', '원자재 검수구역',             'QC',        3, 'Y', 'W015'),

-- WH004 (P001 전극재공창고 - 드라이룸)
( 9, 'P001', 'WH004', 'Z-SLR', '슬러리/전극시트 구역',        'STORAGE',   1, 'Y', 'W016'),
(10, 'P001', 'WH004', 'Z-PRS', '압연/슬릿전극 구역',          'STORAGE',   2, 'Y', 'W016'),

-- WH005 (P001 셀재공창고)
(11, 'P001', 'WH005', 'Z-JEL', '젤리롤/드라이셀 구역',        'STORAGE',   1, 'Y', 'W016'),
(12, 'P001', 'WH005', 'Z-WET', '웻셀/화성셀 구역',            'STORAGE',   2, 'Y', 'W016'),
(13, 'P001', 'WH005', 'Z-STG', '임시보관 구역',               'STAGING',   3, 'Y', 'W016'),

-- WH006 (P001 완성셀창고)
(14, 'P001', 'WH006', 'Z-CEL', '완성셀 보관구역',             'STORAGE',   1, 'Y', 'W015'),
(15, 'P001', 'WH006', 'Z-S1',  '출하 대기구역 (→대전)',       'SHIPPING',  2, 'Y', 'W015'),

-- WH008 (P002 셀입고창고)
(16, 'P002', 'WH008', 'Z-IN',  '셀입고 보관구역',             'STORAGE',   1, 'Y', 'W019'),
(17, 'P002', 'WH008', 'Z-R2',  '입고 대기구역',               'RECEIVING', 2, 'Y', 'W019'),

-- WH009 (P002 팩부품창고)
(18, 'P002', 'WH009', 'Z-BMS', 'BMS/커넥터 구역',             'STORAGE',   1, 'Y', 'W019'),
(19, 'P002', 'WH009', 'Z-HSG', '하우징/냉각/버스바 구역',     'STORAGE',   2, 'Y', 'W019'),

-- WH010 (P002 모듈재공창고)
(20, 'P002', 'WH010', 'Z-MOD', '모듈 보관구역',               'STORAGE',   1, 'Y', 'W019'),

-- WH011 (P002 완성팩창고)
(21, 'P002', 'WH011', 'Z-PCK', '완성팩 보관구역',             'STORAGE',   1, 'Y', 'W021'),
(22, 'P002', 'WH011', 'Z-S2',  '출하 대기구역',               'SHIPPING',  2, 'Y', 'W021');

-- ============================================================
-- 3-2. MCS_LOCATION - 로케이션
-- ============================================================
INSERT INTO MCS_LOCATION (LOCATION_ID, ZONE_ID, LOCATION_CD, LOCATION_NM, MAX_CAPACITY, CURRENT_USAGE, LOCATION_STATUS, USE_YN, REG_USER_ID) VALUES
-- WH001 양극소재창고
( 1,  1, 'NCM-01-01', 'NCM811 파렛트1',         10000.000, 3500.000, 'PARTIAL', 'Y', 'W016'),
( 2,  1, 'NCM-01-02', 'NCM811 파렛트2',         10000.000,    0.000, 'EMPTY',   'Y', 'W016'),
( 3,  2, 'ALF-01-01', '알루미늄포일 랙',           500.000,  170.000, 'PARTIAL', 'Y', 'W016'),
( 4,  2, 'BND-01-01', '바인더/도전재/파우치 선반', 20000.000, 9135.000, 'PARTIAL', 'Y', 'W016'),
( 5,  3, 'R1-01',     '양극소재 입고대기',        9999.000,    0.000, 'EMPTY',   'Y', 'W016'),

-- WH002 음극소재창고
( 6,  4, 'GRP-01-01', '음극활물질 파렛트1',      10000.000, 4200.000, 'PARTIAL', 'Y', 'W016'),
( 7,  5, 'CUF-01-01', '구리포일 랙',               500.000,  175.000, 'PARTIAL', 'Y', 'W016'),

-- WH003 전해액/분리막창고 (방폭)
( 8,  6, 'ELY-01-01', '전해액 탱크1',             5000.000, 2600.000, 'PARTIAL', 'Y', 'W016'),
( 9,  7, 'SEP-01-01', '분리막 랙1',                200.000,   82.000, 'PARTIAL', 'Y', 'W016'),
(10,  7, 'NMP-01-01', 'NMP 용매 탱크',            3000.000, 1600.000, 'PARTIAL', 'Y', 'W016'),
(11,  8, 'QC1-01',    '검수 대기장',              9999.000,    0.000, 'EMPTY',   'Y', 'W016'),

-- WH004 전극재공창고 (드라이룸)
(12,  9, 'SLR-01-01', '슬러리/전극 적치1',        2000.000,  515.000, 'PARTIAL', 'Y', 'W016'),
(13,  9, 'SLR-01-02', '슬러리/전극 적치2',        2000.000,  416.000, 'PARTIAL', 'Y', 'W016'),
(14, 10, 'PRS-01-01', '압연/슬릿전극 적치1',      5000.000, 2940.000, 'PARTIAL', 'Y', 'W016'),
(15, 10, 'PRS-01-02', '압연/슬릿전극 적치2',      5000.000, 3688.000, 'PARTIAL', 'Y', 'W016'),

-- WH005 셀재공창고
(16, 11, 'JEL-01-01', '젤리롤 적치',              3000.000, 1500.000, 'PARTIAL', 'Y', 'W016'),
(17, 12, 'WET-01-01', '웻셀 적치',                2000.000,  800.000, 'PARTIAL', 'Y', 'W016'),
(18, 12, 'FMC-01-01', '화성셀 적치',              2000.000,  900.000, 'PARTIAL', 'Y', 'W016'),
(19, 13, 'STG-01',    '임시보관',                  9999.000,    0.000, 'EMPTY',   'Y', 'W016'),

-- WH006 완성셀창고
(20, 14, 'CEL-01-01', '완성셀 파렛트1',           3000.000, 1067.000, 'PARTIAL', 'Y', 'W015'),
(21, 14, 'CEL-01-02', '완성셀 파렛트2',           3000.000, 1164.000, 'PARTIAL', 'Y', 'W015'),
(22, 15, 'S1-01',     '출하대기 (대전행)',         9999.000,  500.000, 'PARTIAL', 'Y', 'W015'),

-- WH008 셀입고창고 (대전)
(23, 16, 'IN-01-01',  '셀입고 파렛트1',           2000.000,  500.000, 'PARTIAL', 'Y', 'W019'),
(24, 17, 'R2-01',     '입고 대기',                9999.000,    0.000, 'EMPTY',   'Y', 'W019'),

-- WH009 팩부품창고 (대전)
(25, 18, 'BMS-01-01', 'BMS/커넥터 랙',             500.000,  229.000, 'PARTIAL', 'Y', 'W019'),
(26, 19, 'HSG-01-01', '하우징/냉각/버스바 랙',   10000.000, 4161.000, 'PARTIAL', 'Y', 'W019'),

-- WH010 모듈재공창고 (대전)
(27, 20, 'MOD-01-01', '모듈 적치1',                100.000,   18.000, 'PARTIAL', 'Y', 'W019'),

-- WH011 완성팩창고 (대전)
(28, 21, 'PCK-01-01', '완성팩 적치1 (EV)',          50.000,    7.000, 'PARTIAL', 'Y', 'W021'),
(29, 21, 'PCK-01-02', '완성팩 적치2 (ESS)',         50.000,    3.000, 'PARTIAL', 'Y', 'W021'),
(30, 22, 'S2-01',     '출하대기',                  9999.000,    0.000, 'EMPTY',   'Y', 'W021');

-- ============================================================
-- 3-3. MCS_LOCATION_STOCK - 로케이션 재고
--      MES INV_STOCK(창고 레벨)의 하위 상세 위치
--      battery_dummy_05_inventory.sql의 재고 데이터와 연동
-- ============================================================
INSERT INTO MCS_LOCATION_STOCK (LOC_STOCK_ID, PLANT_CD, LOCATION_ID, ITEM_CD, LOT_NO, STOCK_QTY, RESERVED_QTY, REG_USER_ID) VALUES
-- WH001 양극소재창고
( 1, 'P001',  1, 'RM-NCM-001', 'RM-NCM-001-20250118-001', 3500.000,  500.000, 'W016'),  -- 양극활물질 (생산투입 예약)
( 2, 'P001',  3, 'RM-ALF-001', 'RM-ALF-001-20250118-001',  170.000,   10.000, 'W016'),  -- 알루미늄포일
( 3, 'P001',  4, 'RM-PVD-001', 'RM-PVD-001-20250118-001',  450.000,   30.000, 'W016'),  -- PVDF 바인더
( 4, 'P001',  4, 'RM-CNT-001', 'RM-CNT-001-20250118-001',  185.000,   10.000, 'W016'),  -- CNT 도전재
( 5, 'P001',  4, 'RM-PCH-001', 'RM-PCH-001-20250118-001', 8500.000,  500.000, 'W016'),  -- 파우치필름

-- WH002 음극소재창고
( 6, 'P001',  6, 'RM-GRP-001', 'RM-GRP-001-20250118-001', 4200.000,  400.000, 'W016'),  -- 음극활물질
( 7, 'P001',  7, 'RM-CUF-001', 'RM-CUF-001-20250118-001',  175.000,   10.000, 'W016'),  -- 구리포일

-- WH003 전해액/분리막창고
( 8, 'P001',  8, 'RM-ELY-001', 'RM-ELY-001-20250118-001', 2600.000,  200.000, 'W016'),  -- 전해액
( 9, 'P001',  9, 'RM-SEP-001', 'RM-SEP-001-20250118-001',   82.000,    5.000, 'W016'),  -- 분리막

-- WH005 셀재공창고
(10, 'P001', 16, 'SF-JEL-001', 'SF-JEL-001-20250122-001', 1500.000,  500.000, 'W016'),  -- 젤리롤
(11, 'P001', 17, 'SF-WET-001', 'SF-WET-001-20250122-001',  800.000,    0.000, 'W016'),  -- 웻셀
(12, 'P001', 18, 'SF-FMC-001', 'SF-FMC-001-20250122-001',  900.000,    0.000, 'W016'),  -- 화성셀

-- WH006 완성셀창고
(13, 'P001', 20, 'FG-CEL-001', 'FG-CEL-001-20250120-001', 1067.000,  320.000, 'W015'),  -- 완성셀 1/20 (대전출하 예약)
(14, 'P001', 21, 'FG-CEL-001', 'FG-CEL-001-20250121-001', 1164.000,  352.000, 'W015'),  -- 완성셀 1/21 (대전출하 예약)

-- WH008 셀입고창고 (대전)
(15, 'P002', 23, 'FG-CEL-001', NULL,                        500.000,  100.000, 'W019'),  -- 셀입고 (모듈조립 예약)

-- WH009 팩부품창고 (대전)
(16, 'P002', 25, 'RM-BMS-001', 'RM-BMS-001-20250115-001',  138.000,   25.000, 'W019'),  -- BMS기판 EV
(17, 'P002', 25, 'RM-BMS-002', 'RM-BMS-002-20250115-001',   91.000,    4.000, 'W019'),  -- BMS기판 ESS
(18, 'P002', 26, 'RM-BUS-001', 'RM-BUS-001-20250115-001', 4070.000,  375.000, 'W019'),  -- 버스바
(19, 'P002', 26, 'RM-CAS-001', 'RM-CAS-001-20250115-001',   91.000,    4.000, 'W019'),  -- 팩케이스

-- WH010 모듈재공 (대전)
(20, 'P002', 27, 'FG-MOD-001', 'FG-MOD-001-20250122-001',   18.000,    6.000, 'W019'),  -- 모듈 (팩조립 예약)

-- WH011 완성팩 (대전)
(21, 'P002', 28, 'FG-PCK-001', 'FG-PCK-001-20250120-001',    3.000,    0.000, 'W021'),  -- EV팩 1/20
(22, 'P002', 28, 'FG-PCK-001', 'FG-PCK-001-20250121-001',    4.000,    0.000, 'W021'),  -- EV팩 1/21
(23, 'P002', 29, 'FG-PCK-002', 'FG-PCK-002-20250121-001',    3.000,    0.000, 'W021'),  -- ESS팩 1/21

-- WH003 전해액/분리막창고 - NMP 용매
(24, 'P001', 10, 'RM-NMP-001', 'RM-NMP-001-20250118-001', 1600.000,    0.000, 'W016'),  -- NMP 용매 (코팅공정 투입 후 잔량)

-- WH004 전극재공창고 (드라이룸)
(25, 'P001', 12, 'SF-CSL-001', 'SF-CSL-001-20250122-001',  515.000,    0.000, 'W016'),  -- NCM811 양극슬러리 (1/22 생산분)
(26, 'P001', 13, 'SF-ASL-001', 'SF-ASL-001-20250122-001',  416.000,    0.000, 'W016'),  -- 인조흑연 음극슬러리 (1/22 생산분)
(27, 'P001', 14, 'SF-CES-001', 'SF-CES-001-20250121-001', 2940.000, 1000.000, 'W016'),  -- NCM811 양극(슬리팅) (스태킹 투입 예약)
(28, 'P001', 15, 'SF-AES-001', 'SF-AES-001-20250121-001', 3038.000, 1000.000, 'W016'),  -- 흑연 음극(슬리팅) (스태킹 투입 예약)
(29, 'P001', 15, 'SF-CEL-001', 'SF-CEL-001-20250122-001',  650.000,  200.000, 'W016');  -- NCM811 양극전극시트 (압연 투입 예약)

-- ============================================================
-- 3-4. MCS_INBOUND_ORDER / ITEM - 입고
-- ============================================================
INSERT INTO MCS_INBOUND_ORDER (INBOUND_ID, PLANT_CD, INBOUND_NO, INBOUND_STATUS, VENDOR_CD, WAREHOUSE_CD, EXPECTED_DT, ACTUAL_DT, INBOUND_RMK, REG_USER_ID) VALUES
-- 완료된 입고
(1, 'P001', 'IB-20250118-0001', 'COMPLETED',  'V001', 'WH001', '2025-01-18', '2025-01-18 08:30:00', 'NCM811 양극활물질 5톤 입고 (에코프로비엠)',       'W015'),
(2, 'P001', 'IB-20250118-0002', 'COMPLETED',  'V002', 'WH002', '2025-01-18', '2025-01-18 09:30:00', '인조흑연 음극활물질 5톤 + 구리포일 입고',         'W015'),
(3, 'P001', 'IB-20250118-0003', 'COMPLETED',  'V004', 'WH003', '2025-01-18', '2025-01-18 10:00:00', '전해액 LiPF6 3000L 입고 (솔브레인)',              'W015'),
(4, 'P001', 'IB-20250118-0004', 'COMPLETED',  'V003', 'WH003', '2025-01-18', '2025-01-18 11:00:00', '세라믹코팅 분리막 100롤 입고 (SK아이테크)',        'W015'),
(5, 'P001', 'IB-20250118-0005', 'COMPLETED',  'V005', 'WH001', '2025-01-18', '2025-01-18 13:30:00', '알루미늄포일/파우치필름 입고 (일진소재/상보)',      'W015'),
(6, 'P002', 'IB-20250115-0001', 'COMPLETED',  'V009', 'WH009', '2025-01-15', '2025-01-15 09:00:00', 'BMS기판 EV/ESS + 버스바 입고 (디에이테크놀로지)',   'W019'),

-- 검수 중 (전해액 2차분 - 순도 검사)
(7, 'P001', 'IB-20250125-0001', 'INSPECTING', 'V004', 'WH003', '2025-01-25', '2025-01-25 08:30:00', '전해액 LiPF6 2000L 추가분 (순도 검수 중)',        'W015'),

-- 도착 (NCM811 추가분)
(8, 'P001', 'IB-20250128-0001', 'ARRIVED',    'V001', 'WH001', '2025-01-28', '2025-01-28 14:00:00', 'NCM811 양극활물질 3톤 추가분 도착',               'W015'),

-- 예정
( 9, 'P002', 'IB-20250130-0001', 'PLANNED', 'V009', 'WH009', '2025-01-30', NULL, 'BMS기판 EV 200EA 보충 발주',      'W019'),
(10, 'P001', 'IB-20250202-0001', 'PLANNED', 'V003', 'WH003', '2025-02-02', NULL, '분리막 50롤 보충 발주',            'W015'),

-- 취소 (재고 충분)
(11, 'P001', 'IB-20250120-0001', 'CANCELLED', 'V006', 'WH001', '2025-01-20', NULL, 'PVDF 바인더 재고 충분하여 취소', 'W015');

-- 입고 품목
INSERT INTO MCS_INBOUND_ITEM (INBOUND_ID, ITEM_CD, LOT_NO, LOCATION_ID, EXPECTED_QTY, ACTUAL_QTY, ITEM_STATUS, ITEM_RMK, REG_USER_ID) VALUES
-- IB-1: NCM811 양극활물질 → NCM-01-01
(1, 'RM-NCM-001', 'RM-NCM-001-20250118-001',  1, 5000.000, 5000.000, 'STOCKED', '에코프로 LOT EP-NCM811-250110-A', 'W016'),
-- IB-2: 음극활물질/구리포일 → GRP-01-01, CUF-01-01
(2, 'RM-GRP-001', 'RM-GRP-001-20250118-001',  6, 5000.000, 5000.000, 'STOCKED', NULL, 'W016'),
(2, 'RM-CUF-001', 'RM-CUF-001-20250118-001',  7,  200.000,  200.000, 'STOCKED', NULL, 'W016'),
-- IB-3: 전해액 → ELY-01-01
(3, 'RM-ELY-001', 'RM-ELY-001-20250118-001',  8, 3000.000, 3000.000, 'STOCKED', '순도 99.95% 합격', 'W016'),
-- IB-4: 분리막 → SEP-01-01
(4, 'RM-SEP-001', 'RM-SEP-001-20250118-001',  9,  100.000,  100.000, 'STOCKED', NULL, 'W016'),
-- IB-5: 알루미늄포일/파우치필름 → ALF-01-01, BND-01-01
(5, 'RM-ALF-001', 'RM-ALF-001-20250118-001',  3,  200.000,  200.000, 'STOCKED', NULL, 'W016'),
(5, 'RM-PCH-001', 'RM-PCH-001-20250118-001',  4, 10000.000, 10000.000, 'STOCKED', NULL, 'W016'),
-- IB-6: BMS기판/버스바 → BMS-01-01, HSG-01-01
(6, 'RM-BMS-001', 'RM-BMS-001-20250115-001', 25,  200.000,  200.000, 'STOCKED', NULL, 'W019'),
(6, 'RM-BMS-002', 'RM-BMS-002-20250115-001', 25,  100.000,  100.000, 'STOCKED', NULL, 'W019'),
(6, 'RM-BUS-001', 'RM-BUS-001-20250115-001', 26, 5000.000, 5000.000, 'STOCKED', NULL, 'W019'),
(6, 'RM-CAS-001', 'RM-CAS-001-20250115-001', 26,  100.000,  100.000, 'STOCKED', NULL, 'W019'),

-- IB-7: 검수 중 (아직 적치 안 됨)
(7, 'RM-ELY-001', NULL, NULL, 2000.000, 0.000, 'PENDING', '전해액 순도/수분 검사 진행중', 'W016'),

-- IB-8: 도착만 한 상태
(8, 'RM-NCM-001', NULL, NULL, 3000.000, 0.000, 'PENDING', '입도분포 검사 대기', 'W016'),

-- IB-9: 예정 (BMS 보충)
(9, 'RM-BMS-001', NULL, NULL, 200.000, 0.000, 'PENDING', NULL, 'W019'),

-- IB-10: 예정 (분리막 보충)
(10, 'RM-SEP-001', NULL, NULL, 50.000, 0.000, 'PENDING', NULL, 'W016');

-- ============================================================
-- 3-5. MCS_OUTBOUND_ORDER / ITEM - 출고
-- ============================================================
INSERT INTO MCS_OUTBOUND_ORDER (OUTBOUND_ID, PLANT_CD, OUTBOUND_NO, OUTBOUND_STATUS, CUSTOMER_CD, WAREHOUSE_CD, REQUEST_DT, SHIPPED_DT, DESTINATION, WO_ID, OUTBOUND_RMK, REG_USER_ID) VALUES
-- 출하 완료: 양극믹싱 생산투입
(1, 'P001', 'OB-20250120-0001', 'SHIPPED', NULL, 'WH001', '2025-01-20 07:30:00', '2025-01-20 08:00:00',
   '양극믹싱작업장(WC001)', 1, 'WO202501200001 양극슬러리 자재투입 (NCM811+바인더+도전재)', 'W015'),

-- 출하 완료: 완성팩 현대차 납품
(2, 'P002', 'OB-20250122-0001', 'SHIPPED', 'V010', 'WH011', '2025-01-22 09:00:00', '2025-01-22 15:00:00',
   '현대자동차(주) / 서울시 서초구', NULL, 'EV용 배터리팩 납품 (SO-2025-B006)', 'W021'),

-- 피킹 완료: 전해액 생산투입 대기
(3, 'P001', 'OB-20250122-0002', 'PICKED', NULL, 'WH003', '2025-01-22 12:30:00', NULL,
   '전해액주입작업장(WC009)', 34, 'WO202501220007 전해액주입 자재투입', 'W015'),

-- 할당 완료: 완성셀 대전공장 출하
(4, 'P001', 'OB-20250122-0003', 'ALLOCATED', NULL, 'WH006', '2025-01-22 10:00:00', NULL,
   '대전2공장(P002) 셀입고창고(WH008)', NULL, '모듈조립용 완성셀 공장간 이동', 'W015'),

-- 출고 요청: 기아 EV팩 납품
(5, 'P002', 'OB-20250125-0001', 'REQUESTED', 'V011', 'WH011', '2025-01-25 11:00:00', NULL,
   '기아(주) / 서울시 서초구', NULL, 'EV용 배터리팩 납품 (SO-2025-B002)', 'W021'),

-- 취소: SK온 ESS팩 납품 연기
(6, 'P002', 'OB-20250120-0001', 'CANCELLED', 'V012', 'WH011', '2025-01-20 14:00:00', NULL,
   NULL, NULL, 'SK온 ESS팩 납기 연기로 취소', 'W021');

-- 출고 품목
INSERT INTO MCS_OUTBOUND_ITEM (OUTBOUND_ID, ITEM_CD, LOT_NO, LOCATION_ID, REQUESTED_QTY, ALLOCATED_QTY, PICKED_QTY, SHIPPED_QTY, ITEM_STATUS, REG_USER_ID) VALUES
-- OB-1: 양극믹싱 생산투입 완료
(1, 'RM-NCM-001', 'RM-NCM-001-20250118-001',  1, 500.000, 500.000, 500.000, 500.000, 'SHIPPED', 'W016'),
(1, 'RM-PVD-001', 'RM-PVD-001-20250118-001',  4,  30.000,  30.000,  30.000,  30.000, 'SHIPPED', 'W016'),
(1, 'RM-CNT-001', 'RM-CNT-001-20250118-001',  4,  10.000,  10.000,  10.000,  10.000, 'SHIPPED', 'W016'),

-- OB-2: 현대차 EV팩 납품 완료
(2, 'FG-PCK-001', 'FG-PCK-001-20250120-001', 28, 3.000, 3.000, 3.000, 3.000, 'SHIPPED', 'W021'),

-- OB-3: 전해액 피킹 완료 (투입 대기)
(3, 'RM-ELY-001', 'RM-ELY-001-20250118-001',  8, 200.000, 200.000, 200.000, 0.000, 'PICKED', 'W016'),

-- OB-4: 완성셀 대전 출하 할당
(4, 'FG-CEL-001', 'FG-CEL-001-20250120-001', 20, 320.000, 320.000, 0.000, 0.000, 'ALLOCATED', 'W015'),
(4, 'FG-CEL-001', 'FG-CEL-001-20250121-001', 21, 180.000, 180.000, 0.000, 0.000, 'ALLOCATED', 'W015'),

-- OB-5: 기아 EV팩 요청
(5, 'FG-PCK-001', 'FG-PCK-001-20250121-001', 28, 4.000, 0.000, 0.000, 0.000, 'PENDING', 'W021'),

-- OB-6: 취소
(6, 'FG-PCK-002', 'FG-PCK-002-20250121-001', 29, 3.000, 0.000, 0.000, 0.000, 'CANCELLED', 'W021');

-- ============================================================
-- 3-6. MCS_TRANSFER_ORDER / ITEM - 이동
-- ============================================================
INSERT INTO MCS_TRANSFER_ORDER (TRANSFER_ID, PLANT_CD, TRANSFER_NO, TRANSFER_STATUS, FROM_LOCATION_ID, TO_LOCATION_ID, TRANSFER_REASON, REG_USER_ID) VALUES
-- 완료: 입고대기 → NCM 정위치
(1, 'P001', 'TF-20250118-0001', 'COMPLETED',    5,  1, '입고대기구역 → NCM활물질 파렛트1 적치', 'W016'),
-- 완료: 입고대기 → 전해액 탱크
(2, 'P001', 'TF-20250118-0002', 'COMPLETED',   11,  8, '검수완료 전해액 → ELY탱크1 이송',       'W016'),

-- 진행 중: 완성셀 → 출하대기 (대전행)
(3, 'P001', 'TF-20250122-0001', 'IN_PROGRESS', 20, 22, '완성셀 1/20분 출하대기 이동 (대전공장 출하 준비)', 'W015'),

-- 요청: 완성팩 → 출하대기 (현대차행)
(4, 'P002', 'TF-20250125-0001', 'REQUESTED',   28, 30, '현대차 납품 준비 - EV팩 출하대기 이동', 'W021'),

-- 취소: 셀재공 내 이동 취소
(5, 'P001', 'TF-20250121-0001', 'CANCELLED',   17, 19, '웻셀 임시보관 이동 → 바로 화성공정 투입으로 취소', 'W016');

INSERT INTO MCS_TRANSFER_ITEM (TRANSFER_ID, ITEM_CD, LOT_NO, TRANSFER_QTY, ITEM_STATUS, REG_USER_ID) VALUES
(1, 'RM-NCM-001', 'RM-NCM-001-20250118-001', 5000.000, 'MOVED',     'W016'),
(2, 'RM-ELY-001', 'RM-ELY-001-20250118-001', 3000.000, 'MOVED',     'W016'),
(3, 'FG-CEL-001', 'FG-CEL-001-20250120-001',  500.000, 'PENDING',   'W015'),
(4, 'FG-PCK-001', 'FG-PCK-001-20250121-001',    4.000, 'PENDING',   'W021'),
(5, 'SF-WET-001', 'SF-WET-001-20250121-001',  800.000, 'CANCELLED', 'W016');

-- ============================================================
-- 3-7. MCS_LOC_TRANS_HIS - 주요 이력
-- ============================================================
INSERT INTO MCS_LOC_TRANS_HIS (PLANT_CD, LOC_STOCK_ID, TRANS_TYPE, TRANS_QTY, BEFORE_QTY, AFTER_QTY, REF_TYPE, REF_NO, REF_ID, TRANS_RMK, REG_USER_ID) VALUES
-- 입고 적치 이력
('P001',  1, 'IB_IN', 5000.000,    0.000, 5000.000, 'IB', 'IB-20250118-0001', 1, 'NCM811 양극활물질 5톤 입고 적치',    'W016'),
('P001',  6, 'IB_IN', 5000.000,    0.000, 5000.000, 'IB', 'IB-20250118-0002', 2, '인조흑연 음극활물질 5톤 입고',       'W016'),
('P001',  7, 'IB_IN',  200.000,    0.000,  200.000, 'IB', 'IB-20250118-0002', 2, '구리포일 200롤 입고',                'W016'),
('P001',  8, 'IB_IN', 3000.000,    0.000, 3000.000, 'IB', 'IB-20250118-0003', 3, '전해액 LiPF6 3000L 입고',           'W016'),
('P001',  9, 'IB_IN',  100.000,    0.000,  100.000, 'IB', 'IB-20250118-0004', 4, '세라믹코팅 분리막 100롤 입고',       'W016'),
('P002', 16, 'IB_IN',  200.000,    0.000,  200.000, 'IB', 'IB-20250115-0001', 6, 'BMS기판 EV 200EA 입고',             'W019'),
('P002', 17, 'IB_IN',  100.000,    0.000,  100.000, 'IB', 'IB-20250115-0001', 6, 'BMS기판 ESS 100EA 입고',            'W019'),
('P002', 18, 'IB_IN', 5000.000,    0.000, 5000.000, 'IB', 'IB-20250115-0001', 6, '버스바 5000EA 입고',                'W019'),

-- 생산투입 출고 이력
('P001',  1, 'OB_OUT',  500.000, 5000.000, 4500.000, 'OB', 'OB-20250120-0001', 1, 'WO202501200001 양극믹싱 NCM811 투입', 'W016'),
('P001',  3, 'OB_OUT',   30.000,  480.000,  450.000, 'OB', 'OB-20250120-0001', 1, 'WO202501200001 PVDF 바인더 투입',     'W016'),
('P001',  4, 'OB_OUT',   10.000,  195.000,  185.000, 'OB', 'OB-20250120-0001', 1, 'WO202501200001 CNT 도전재 투입',      'W016'),

-- 고객 납품 출고 이력
('P002', 21, 'OB_OUT',    3.000,    3.000,    0.000, 'OB', 'OB-20250122-0001', 2, '현대차 EV팩 3SET 납품',              'W021'),

-- 이동 이력
('P001',  1, 'TF_IN',  5000.000,    0.000, 5000.000, 'TF', 'TF-20250118-0001', 1, '입고대기 → NCM파렛트1 적치',        'W016'),
('P001',  8, 'TF_IN',  3000.000,    0.000, 3000.000, 'TF', 'TF-20250118-0002', 2, '검수완료 → 전해액탱크1 이송',        'W016'),

-- 재고 조정 (실사)
('P001',  5, 'ADJ_MINUS', 500.000, 9000.000, 8500.000, NULL, NULL, NULL, '파우치필름 실사 결과 조정 (-500EA, 인쇄불량)', 'W015'),

-- NMP 용매 입고/투입 이력
('P001', 24, 'IB_IN',      2000.000,    0.000, 2000.000, 'IB', 'IB-20250118-0003', 3, 'NMP 용매 2000L 전해액 입고시 동반입고',      'W016'),
('P001', 24, 'PRD_ISSUE',   400.000, 2000.000, 1600.000, NULL, NULL, NULL, '양극/음극 코팅공정 NMP 용매 투입 (1/20~1/22)', 'W016'),

-- WH004 전극재공 생산입고 이력 (1/20~1/22 생산분)
('P001', 25, 'PRD_RECEIPT',  515.000,    0.000,  515.000, NULL, 'WO202501220001', 28, 'NCM811 양극슬러리 515KG 양극믹싱 생산입고 → SLR-01-01', 'W016'),
('P001', 26, 'PRD_RECEIPT',  416.000,    0.000,  416.000, NULL, 'WO202501220002', 29, '인조흑연 음극슬러리 416KG 음극믹싱 생산입고 → SLR-01-02', 'W016'),
('P001', 27, 'PRD_RECEIPT', 2940.000,    0.000, 2940.000, NULL, 'WO202501210006', 19, 'NCM811 양극(슬리팅) 2940EA 슬리팅/노칭 생산입고 → PRS-01-01', 'W016'),
('P001', 28, 'PRD_RECEIPT', 3038.000,    0.000, 3038.000, NULL, 'WO202501210007', 20, '흑연 음극(슬리팅) 3038EA 슬리팅/노칭 생산입고 → PRS-01-02', 'W016'),
('P001', 29, 'PRD_RECEIPT',  650.000,    0.000,  650.000, NULL, 'WO202501220003', 30, 'NCM811 양극전극시트 650M 양극코팅 생산입고 → PRS-01-02', 'W016');


SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 검증 쿼리 (설치 후 확인용)
-- 기대 카운트: ZONE=22, LOCATION=30, LOC_STOCK=29, LOC_TRANS=24,
--              IB_ORDER=11, IB_ITEM=15, OB_ORDER=6, OB_ITEM=9,
--              TF_ORDER=5, TF_ITEM=5
-- ============================================================
-- SELECT 'MCS_ZONE' AS TBL, COUNT(*) AS CNT FROM MCS_ZONE
-- UNION ALL SELECT 'MCS_LOCATION', COUNT(*) FROM MCS_LOCATION
-- UNION ALL SELECT 'MCS_LOCATION_STOCK', COUNT(*) FROM MCS_LOCATION_STOCK
-- UNION ALL SELECT 'MCS_LOC_TRANS_HIS', COUNT(*) FROM MCS_LOC_TRANS_HIS
-- UNION ALL SELECT 'MCS_INBOUND_ORDER', COUNT(*) FROM MCS_INBOUND_ORDER
-- UNION ALL SELECT 'MCS_INBOUND_ITEM', COUNT(*) FROM MCS_INBOUND_ITEM
-- UNION ALL SELECT 'MCS_OUTBOUND_ORDER', COUNT(*) FROM MCS_OUTBOUND_ORDER
-- UNION ALL SELECT 'MCS_OUTBOUND_ITEM', COUNT(*) FROM MCS_OUTBOUND_ITEM
-- UNION ALL SELECT 'MCS_TRANSFER_ORDER', COUNT(*) FROM MCS_TRANSFER_ORDER
-- UNION ALL SELECT 'MCS_TRANSFER_ITEM', COUNT(*) FROM MCS_TRANSFER_ITEM;
