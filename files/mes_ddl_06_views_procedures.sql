-- ============================================================
-- MES 시스템 데이터베이스 DDL 스크립트
-- DBMS: MariaDB 10.x
-- 파일: mes_ddl_06_views_procedures.sql (뷰, 저장 프로시저, 초기 데이터)
-- 작성일: 2025-01-19
-- ============================================================

USE MES_DB;

-- ============================================================
-- PART 1: 뷰(VIEW) 생성
-- ============================================================

-- ------------------------------------------------------------
-- 1. 생산현황 관련 뷰
-- ------------------------------------------------------------

-- 1.1 일별 생산실적 현황 뷰
CREATE OR REPLACE VIEW V_DAILY_PROD_SUMMARY AS
SELECT 
    r.PLANT_CD,
    r.RESULT_DT,
    r.ITEM_CD,
    i.ITEM_NM,
    i.ITEM_TYPE,
    r.WORKCENTER_CD,
    wc.WORKCENTER_NM,
    COUNT(DISTINCT r.WO_ID) AS WO_CNT,
    SUM(r.PROD_QTY) AS TOTAL_PROD_QTY,
    SUM(r.GOOD_QTY) AS TOTAL_GOOD_QTY,
    SUM(r.DEFECT_QTY) AS TOTAL_DEFECT_QTY,
    ROUND(SUM(r.GOOD_QTY) / NULLIF(SUM(r.PROD_QTY), 0) * 100, 2) AS YIELD_RATE,
    SUM(r.WORK_TIME) AS TOTAL_WORK_TIME
FROM PRD_WORK_RESULT r
JOIN MST_ITEM i ON r.ITEM_CD = i.ITEM_CD
LEFT JOIN MST_WORKCENTER wc ON r.WORKCENTER_CD = wc.WORKCENTER_CD
GROUP BY r.PLANT_CD, r.RESULT_DT, r.ITEM_CD, i.ITEM_NM, i.ITEM_TYPE, 
         r.WORKCENTER_CD, wc.WORKCENTER_NM;

-- 1.2 작업지시 진행현황 뷰
CREATE OR REPLACE VIEW V_WORK_ORDER_STATUS AS
SELECT 
    wo.WO_ID,
    wo.PLANT_CD,
    wo.WO_NO,
    wo.WO_DT,
    wo.ITEM_CD,
    i.ITEM_NM,
    wo.WO_QTY,
    wo.GOOD_QTY,
    wo.DEFECT_QTY,
    ROUND(wo.GOOD_QTY / NULLIF(wo.WO_QTY, 0) * 100, 2) AS PROGRESS_RATE,
    wo.WO_STATUS,
    wo.WORKCENTER_CD,
    wc.WORKCENTER_NM,
    wo.EQUIPMENT_CD,
    eq.EQUIPMENT_NM,
    wo.WORKER_ID,
    w.WORKER_NM,
    wo.PLAN_START_DTM,
    wo.PLAN_END_DTM,
    wo.ACTUAL_START_DTM,
    wo.ACTUAL_END_DTM,
    wo.LOT_NO,
    wo.PRIORITY
FROM PLN_WORK_ORDER wo
JOIN MST_ITEM i ON wo.ITEM_CD = i.ITEM_CD
LEFT JOIN MST_WORKCENTER wc ON wo.WORKCENTER_CD = wc.WORKCENTER_CD
LEFT JOIN MST_EQUIPMENT eq ON wo.EQUIPMENT_CD = eq.EQUIPMENT_CD
LEFT JOIN MST_WORKER w ON wo.WORKER_ID = w.WORKER_ID;

-- 1.3 불량현황 분석 뷰
CREATE OR REPLACE VIEW V_DEFECT_ANALYSIS AS
SELECT 
    d.PLANT_CD,
    d.DEFECT_DT,
    d.ITEM_CD,
    i.ITEM_NM,
    d.DEFECT_CD,
    dc.DEFECT_NM,
    d.DEFECT_TYPE,
    d.PROCESS_CD,
    d.EQUIPMENT_CD,
    eq.EQUIPMENT_NM,
    SUM(d.DEFECT_QTY) AS TOTAL_DEFECT_QTY,
    COUNT(*) AS DEFECT_CNT
FROM PRD_DEFECT_HIS d
JOIN MST_ITEM i ON d.ITEM_CD = i.ITEM_CD
LEFT JOIN QC_DEFECT_CODE dc ON d.DEFECT_CD = dc.DEFECT_CD
LEFT JOIN MST_EQUIPMENT eq ON d.EQUIPMENT_CD = eq.EQUIPMENT_CD
GROUP BY d.PLANT_CD, d.DEFECT_DT, d.ITEM_CD, i.ITEM_NM, 
         d.DEFECT_CD, dc.DEFECT_NM, d.DEFECT_TYPE, 
         d.PROCESS_CD, d.EQUIPMENT_CD, eq.EQUIPMENT_NM;

-- ------------------------------------------------------------
-- 2. 재고현황 관련 뷰
-- ------------------------------------------------------------

-- 2.1 품목별 재고 현황 뷰
CREATE OR REPLACE VIEW V_STOCK_SUMMARY AS
SELECT 
    s.PLANT_CD,
    s.ITEM_CD,
    i.ITEM_NM,
    i.ITEM_TYPE,
    i.ITEM_GRP,
    i.UNIT,
    SUM(s.STOCK_QTY) AS TOTAL_STOCK_QTY,
    SUM(s.RESERVED_QTY) AS TOTAL_RESERVED_QTY,
    SUM(s.AVAILABLE_QTY) AS TOTAL_AVAILABLE_QTY,
    i.SAFETY_STOCK_QTY,
    CASE WHEN SUM(s.AVAILABLE_QTY) < COALESCE(i.SAFETY_STOCK_QTY, 0) 
         THEN 'Y' ELSE 'N' END AS LOW_STOCK_YN,
    SUM(s.STOCK_AMT) AS TOTAL_STOCK_AMT
FROM INV_STOCK s
JOIN MST_ITEM i ON s.ITEM_CD = i.ITEM_CD
WHERE s.STOCK_STATUS = '정상'
GROUP BY s.PLANT_CD, s.ITEM_CD, i.ITEM_NM, i.ITEM_TYPE, 
         i.ITEM_GRP, i.UNIT, i.SAFETY_STOCK_QTY;

-- 2.2 창고별 재고현황 뷰
CREATE OR REPLACE VIEW V_WAREHOUSE_STOCK AS
SELECT 
    s.PLANT_CD,
    s.WAREHOUSE_CD,
    wh.WAREHOUSE_NM,
    wh.WAREHOUSE_TYPE,
    s.ITEM_CD,
    i.ITEM_NM,
    i.ITEM_TYPE,
    s.LOT_NO,
    s.STOCK_QTY,
    s.RESERVED_QTY,
    s.AVAILABLE_QTY,
    s.STOCK_STATUS,
    s.EXPIRE_DT,
    CASE WHEN s.EXPIRE_DT IS NOT NULL AND s.EXPIRE_DT <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
         THEN 'Y' ELSE 'N' END AS NEAR_EXPIRE_YN
FROM INV_STOCK s
JOIN MST_ITEM i ON s.ITEM_CD = i.ITEM_CD
JOIN MST_WAREHOUSE wh ON s.WAREHOUSE_CD = wh.WAREHOUSE_CD
WHERE s.STOCK_QTY > 0;

-- 2.3 LOT 추적 뷰
CREATE OR REPLACE VIEW V_LOT_TRACKING AS
SELECT 
    l.LOT_NO,
    l.PLANT_CD,
    l.ITEM_CD,
    i.ITEM_NM,
    l.LOT_QTY,
    l.CURRENT_QTY,
    l.CREATE_DT,
    l.EXPIRE_DT,
    l.LOT_STATUS,
    l.VENDOR_CD,
    v.VENDOR_NM,
    l.VENDOR_LOT_NO,
    l.WO_ID,
    wo.WO_NO,
    l.PARENT_LOT_NO
FROM INV_LOT l
JOIN MST_ITEM i ON l.ITEM_CD = i.ITEM_CD
LEFT JOIN MST_VENDOR v ON l.VENDOR_CD = v.VENDOR_CD
LEFT JOIN PLN_WORK_ORDER wo ON l.WO_ID = wo.WO_ID;

-- ------------------------------------------------------------
-- 3. 설비현황 관련 뷰
-- ------------------------------------------------------------

-- 3.1 설비 가동률 뷰
CREATE OR REPLACE VIEW V_EQUIPMENT_UTILIZATION AS
SELECT 
    o.PLANT_CD,
    o.EQUIPMENT_CD,
    e.EQUIPMENT_NM,
    e.EQUIPMENT_TYPE,
    o.OPER_DT,
    o.SHIFT,
    SUM(CASE WHEN o.OPER_STATUS = '가동' THEN o.OPER_TIME ELSE 0 END) AS RUN_TIME,
    SUM(CASE WHEN o.OPER_STATUS = '비가동' THEN o.OPER_TIME ELSE 0 END) AS DOWN_TIME,
    SUM(CASE WHEN o.OPER_STATUS = '대기' THEN o.OPER_TIME ELSE 0 END) AS IDLE_TIME,
    SUM(o.OPER_TIME) AS TOTAL_TIME,
    ROUND(SUM(CASE WHEN o.OPER_STATUS = '가동' THEN o.OPER_TIME ELSE 0 END) 
          / NULLIF(SUM(o.OPER_TIME), 0) * 100, 2) AS UTILIZATION_RATE,
    SUM(o.PROD_QTY) AS TOTAL_PROD_QTY
FROM EQP_OPER_STATUS o
JOIN MST_EQUIPMENT e ON o.EQUIPMENT_CD = e.EQUIPMENT_CD
GROUP BY o.PLANT_CD, o.EQUIPMENT_CD, e.EQUIPMENT_NM, 
         e.EQUIPMENT_TYPE, o.OPER_DT, o.SHIFT;

-- 3.2 설비 비가동 분석 뷰
CREATE OR REPLACE VIEW V_DOWNTIME_ANALYSIS AS
SELECT 
    d.PLANT_CD,
    d.EQUIPMENT_CD,
    e.EQUIPMENT_NM,
    d.DOWNTIME_DT,
    d.DOWNTIME_TYPE,
    d.DOWNTIME_CD,
    dc.DOWNTIME_NM,
    SUM(d.DOWNTIME_MIN) AS TOTAL_DOWNTIME_MIN,
    COUNT(*) AS DOWNTIME_CNT,
    dc.PLANNED_YN
FROM EQP_DOWNTIME d
JOIN MST_EQUIPMENT e ON d.EQUIPMENT_CD = e.EQUIPMENT_CD
JOIN EQP_DOWNTIME_CODE dc ON d.DOWNTIME_CD = dc.DOWNTIME_CD
GROUP BY d.PLANT_CD, d.EQUIPMENT_CD, e.EQUIPMENT_NM, 
         d.DOWNTIME_DT, d.DOWNTIME_TYPE, d.DOWNTIME_CD, 
         dc.DOWNTIME_NM, dc.PLANNED_YN;

-- 3.3 정비예정 알림 뷰
CREATE OR REPLACE VIEW V_MAINT_SCHEDULE_ALERT AS
SELECT 
    mp.MAINT_PLAN_ID,
    mp.PLANT_CD,
    mp.EQUIPMENT_CD,
    e.EQUIPMENT_NM,
    e.WORKCENTER_CD,
    wc.WORKCENTER_NM,
    mp.MAINT_PLAN_NM,
    mp.MAINT_TYPE,
    mp.NEXT_MAINT_DT,
    mp.LAST_MAINT_DT,
    DATEDIFF(mp.NEXT_MAINT_DT, CURDATE()) AS DAYS_UNTIL_MAINT,
    CASE 
        WHEN mp.NEXT_MAINT_DT <= CURDATE() THEN '지연'
        WHEN mp.NEXT_MAINT_DT <= DATE_ADD(CURDATE(), INTERVAL mp.LEAD_TIME DAY) THEN '임박'
        ELSE '정상'
    END AS MAINT_ALERT_STATUS,
    mp.MANAGER_ID,
    w.WORKER_NM AS MANAGER_NM
FROM EQP_MAINT_PLAN mp
JOIN MST_EQUIPMENT e ON mp.EQUIPMENT_CD = e.EQUIPMENT_CD
JOIN MST_WORKCENTER wc ON e.WORKCENTER_CD = wc.WORKCENTER_CD
LEFT JOIN MST_WORKER w ON mp.MANAGER_ID = w.WORKER_ID
WHERE mp.USE_YN = 'Y'
  AND mp.NEXT_MAINT_DT IS NOT NULL;

-- ------------------------------------------------------------
-- 4. 품질현황 관련 뷰
-- ------------------------------------------------------------

-- 4.1 검사현황 요약 뷰
CREATE OR REPLACE VIEW V_INSPECT_SUMMARY AS
SELECT 
    ir.PLANT_CD,
    ir.INSPECT_DT,
    ir.INSPECT_TYPE,
    ir.ITEM_CD,
    i.ITEM_NM,
    COUNT(*) AS INSPECT_CNT,
    SUM(ir.INSPECT_QTY) AS TOTAL_INSPECT_QTY,
    SUM(ir.PASS_QTY) AS TOTAL_PASS_QTY,
    SUM(ir.FAIL_QTY) AS TOTAL_FAIL_QTY,
    ROUND(SUM(ir.PASS_QTY) / NULLIF(SUM(ir.INSPECT_QTY), 0) * 100, 2) AS PASS_RATE,
    SUM(CASE WHEN ir.JUDGE_RESULT = '합격' THEN 1 ELSE 0 END) AS PASS_CNT,
    SUM(CASE WHEN ir.JUDGE_RESULT = '불합격' THEN 1 ELSE 0 END) AS FAIL_CNT,
    SUM(CASE WHEN ir.JUDGE_RESULT = '조건부합격' THEN 1 ELSE 0 END) AS COND_PASS_CNT
FROM QC_INSPECT_RESULT ir
JOIN MST_ITEM i ON ir.ITEM_CD = i.ITEM_CD
GROUP BY ir.PLANT_CD, ir.INSPECT_DT, ir.INSPECT_TYPE, ir.ITEM_CD, i.ITEM_NM;

-- 4.2 공급업체별 수입검사 현황 뷰
CREATE OR REPLACE VIEW V_VENDOR_INSPECT_STATUS AS
SELECT 
    ir.PLANT_CD,
    ir.VENDOR_CD,
    v.VENDOR_NM,
    ir.ITEM_CD,
    i.ITEM_NM,
    COUNT(*) AS INSPECT_CNT,
    SUM(ir.INSPECT_QTY) AS TOTAL_INSPECT_QTY,
    SUM(ir.PASS_QTY) AS TOTAL_PASS_QTY,
    SUM(ir.FAIL_QTY) AS TOTAL_FAIL_QTY,
    ROUND(SUM(ir.PASS_QTY) / NULLIF(SUM(ir.INSPECT_QTY), 0) * 100, 2) AS PASS_RATE
FROM QC_INSPECT_RESULT ir
JOIN MST_ITEM i ON ir.ITEM_CD = i.ITEM_CD
JOIN MST_VENDOR v ON ir.VENDOR_CD = v.VENDOR_CD
WHERE ir.INSPECT_TYPE = '수입검사'
GROUP BY ir.PLANT_CD, ir.VENDOR_CD, v.VENDOR_NM, ir.ITEM_CD, i.ITEM_NM;

-- ============================================================
-- PART 2: 저장 프로시저(Stored Procedure) 생성
-- ============================================================

DELIMITER //

-- ------------------------------------------------------------
-- 1. 채번 프로시저
-- ------------------------------------------------------------

-- 1.1 범용 채번 프로시저
CREATE PROCEDURE SP_GET_SEQ_NO (
    IN p_seq_type VARCHAR(20),
    IN p_prefix VARCHAR(10),
    IN p_seq_length INT,
    OUT p_seq_no VARCHAR(50)
)
BEGIN
    DECLARE v_current_seq INT DEFAULT 0;
    DECLARE v_seq_dt DATE DEFAULT CURDATE();
    
    -- 채번 테이블 잠금
    SELECT CURRENT_SEQ INTO v_current_seq
    FROM COM_SEQ_NO
    WHERE SEQ_TYPE = p_seq_type AND SEQ_DT = v_seq_dt
    FOR UPDATE;
    
    IF v_current_seq IS NULL THEN
        -- 신규 일자 채번 시작
        INSERT INTO COM_SEQ_NO (SEQ_TYPE, PREFIX, SEQ_DT, CURRENT_SEQ, SEQ_LENGTH)
        VALUES (p_seq_type, p_prefix, v_seq_dt, 1, p_seq_length);
        SET v_current_seq = 1;
    ELSE
        -- 기존 채번 증가
        UPDATE COM_SEQ_NO 
        SET CURRENT_SEQ = CURRENT_SEQ + 1
        WHERE SEQ_TYPE = p_seq_type AND SEQ_DT = v_seq_dt;
        SET v_current_seq = v_current_seq + 1;
    END IF;
    
    -- 채번 결과 생성
    SET p_seq_no = CONCAT(p_prefix, DATE_FORMAT(v_seq_dt, '%Y%m%d'), 
                          LPAD(v_current_seq, p_seq_length, '0'));
END //

-- 1.2 작업지시번호 채번 프로시저
CREATE PROCEDURE SP_GET_WO_NO (
    OUT p_wo_no VARCHAR(30)
)
BEGIN
    CALL SP_GET_SEQ_NO('WO', 'WO', 4, p_wo_no);
END //

-- 1.3 LOT번호 채번 프로시저
CREATE PROCEDURE SP_GET_LOT_NO (
    IN p_item_cd VARCHAR(50),
    OUT p_lot_no VARCHAR(50)
)
BEGIN
    DECLARE v_seq INT DEFAULT 0;
    DECLARE v_seq_dt DATE DEFAULT CURDATE();
    
    SELECT COALESCE(MAX(CAST(SUBSTRING(LOT_NO, -3) AS UNSIGNED)), 0) + 1 INTO v_seq
    FROM INV_LOT
    WHERE ITEM_CD = p_item_cd
      AND DATE(CREATE_DT) = v_seq_dt;
    
    SET p_lot_no = CONCAT(p_item_cd, '-', DATE_FORMAT(v_seq_dt, '%Y%m%d'), '-', LPAD(v_seq, 3, '0'));
END //

-- ------------------------------------------------------------
-- 2. 생산관리 프로시저
-- ------------------------------------------------------------

-- 2.1 작업지시 생성 프로시저
CREATE PROCEDURE SP_CREATE_WORK_ORDER (
    IN p_plant_cd VARCHAR(20),
    IN p_item_cd VARCHAR(50),
    IN p_wo_qty DECIMAL(15,3),
    IN p_plan_start_dtm DATETIME,
    IN p_plan_end_dtm DATETIME,
    IN p_workcenter_cd VARCHAR(20),
    IN p_user_id VARCHAR(50),
    OUT p_wo_id BIGINT,
    OUT p_wo_no VARCHAR(30)
)
BEGIN
    DECLARE v_wo_no VARCHAR(30);
    
    -- 작업지시번호 채번
    CALL SP_GET_WO_NO(v_wo_no);
    
    -- 작업지시 등록
    INSERT INTO PLN_WORK_ORDER (
        PLANT_CD, WO_NO, WO_DT, ITEM_CD, WO_QTY,
        WORKCENTER_CD, PLAN_START_DTM, PLAN_END_DTM,
        WO_STATUS, REG_USER_ID
    ) VALUES (
        p_plant_cd, v_wo_no, CURDATE(), p_item_cd, p_wo_qty,
        p_workcenter_cd, p_plan_start_dtm, p_plan_end_dtm,
        '대기', p_user_id
    );
    
    SET p_wo_id = LAST_INSERT_ID();
    SET p_wo_no = v_wo_no;
    
    -- 자재 소요량 자동 생성 (BOM 기반)
    INSERT INTO PLN_WO_MATERIAL (WO_ID, ITEM_CD, PLAN_QTY, REG_USER_ID)
    SELECT p_wo_id, 
           b.CHILD_ITEM_CD, 
           ROUND(p_wo_qty * b.BOM_QTY * (1 + COALESCE(b.LOSS_RATE, 0) / 100), 5),
           p_user_id
    FROM MST_BOM b
    WHERE b.PARENT_ITEM_CD = p_item_cd
      AND b.USE_YN = 'Y'
      AND CURDATE() BETWEEN b.START_DT AND COALESCE(b.END_DT, '9999-12-31');
    
END //

-- 2.2 작업시작 프로시저
CREATE PROCEDURE SP_START_WORK_ORDER (
    IN p_wo_id BIGINT,
    IN p_worker_id VARCHAR(20),
    IN p_equipment_cd VARCHAR(20),
    IN p_user_id VARCHAR(50)
)
BEGIN
    -- 작업지시 상태 변경
    UPDATE PLN_WORK_ORDER
    SET WO_STATUS = '진행',
        ACTUAL_START_DTM = NOW(),
        WORKER_ID = p_worker_id,
        EQUIPMENT_CD = p_equipment_cd,
        UPD_USER_ID = p_user_id,
        UPD_DTM = NOW()
    WHERE WO_ID = p_wo_id;
    
    -- 설비 가동 시작 기록
    IF p_equipment_cd IS NOT NULL THEN
        INSERT INTO EQP_OPER_STATUS (
            PLANT_CD, EQUIPMENT_CD, OPER_DT, SHIFT, OPER_STATUS,
            START_DTM, WO_ID, WORKER_ID, REG_USER_ID
        )
        SELECT PLANT_CD, p_equipment_cd, CURDATE(), 
               CASE WHEN HOUR(NOW()) < 18 THEN '주간' ELSE '야간' END,
               '가동', NOW(), p_wo_id, p_worker_id, p_user_id
        FROM PLN_WORK_ORDER
        WHERE WO_ID = p_wo_id;
    END IF;
END //

-- 2.3 생산실적 등록 프로시저
CREATE PROCEDURE SP_REG_WORK_RESULT (
    IN p_wo_id BIGINT,
    IN p_good_qty DECIMAL(15,3),
    IN p_defect_qty DECIMAL(15,3),
    IN p_worker_id VARCHAR(20),
    IN p_user_id VARCHAR(50),
    OUT p_result_id BIGINT
)
BEGIN
    DECLARE v_plant_cd VARCHAR(20);
    DECLARE v_item_cd VARCHAR(50);
    DECLARE v_workcenter_cd VARCHAR(20);
    DECLARE v_equipment_cd VARCHAR(20);
    DECLARE v_lot_no VARCHAR(50);
    DECLARE v_result_no VARCHAR(30);
    DECLARE v_prod_qty DECIMAL(15,3);
    
    SET v_prod_qty = p_good_qty + COALESCE(p_defect_qty, 0);
    
    -- 작업지시 정보 조회
    SELECT PLANT_CD, ITEM_CD, WORKCENTER_CD, EQUIPMENT_CD, LOT_NO
    INTO v_plant_cd, v_item_cd, v_workcenter_cd, v_equipment_cd, v_lot_no
    FROM PLN_WORK_ORDER
    WHERE WO_ID = p_wo_id;
    
    -- 실적번호 채번
    CALL SP_GET_SEQ_NO('PR', 'PR', 4, v_result_no);
    
    -- LOT번호 생성 (없는 경우)
    IF v_lot_no IS NULL THEN
        CALL SP_GET_LOT_NO(v_item_cd, v_lot_no);
        UPDATE PLN_WORK_ORDER SET LOT_NO = v_lot_no WHERE WO_ID = p_wo_id;
    END IF;
    
    -- 생산실적 등록
    INSERT INTO PRD_WORK_RESULT (
        PLANT_CD, RESULT_NO, WO_ID, RESULT_DT, SHIFT,
        WORKER_ID, WORKCENTER_CD, EQUIPMENT_CD, ITEM_CD,
        PROD_QTY, GOOD_QTY, DEFECT_QTY,
        START_DTM, LOT_NO, RESULT_STATUS, REG_USER_ID
    ) VALUES (
        v_plant_cd, v_result_no, p_wo_id, CURDATE(),
        CASE WHEN HOUR(NOW()) < 18 THEN '주간' ELSE '야간' END,
        p_worker_id, v_workcenter_cd, v_equipment_cd, v_item_cd,
        v_prod_qty, p_good_qty, p_defect_qty,
        NOW(), v_lot_no, '완료', p_user_id
    );
    
    SET p_result_id = LAST_INSERT_ID();
    
    -- 작업지시 수량 업데이트
    UPDATE PLN_WORK_ORDER
    SET GOOD_QTY = GOOD_QTY + p_good_qty,
        DEFECT_QTY = DEFECT_QTY + COALESCE(p_defect_qty, 0),
        UPD_USER_ID = p_user_id,
        UPD_DTM = NOW()
    WHERE WO_ID = p_wo_id;
    
    -- LOT 정보 생성/업데이트
    INSERT INTO INV_LOT (PLANT_CD, LOT_NO, ITEM_CD, LOT_QTY, CURRENT_QTY, 
                         CREATE_DT, WO_ID, LOT_TYPE, LOT_STATUS, REG_USER_ID)
    VALUES (v_plant_cd, v_lot_no, v_item_cd, p_good_qty, p_good_qty,
            CURDATE(), p_wo_id, '생산', '정상', p_user_id)
    ON DUPLICATE KEY UPDATE 
        LOT_QTY = LOT_QTY + p_good_qty,
        CURRENT_QTY = CURRENT_QTY + p_good_qty,
        UPD_USER_ID = p_user_id,
        UPD_DTM = NOW();
    
END //

-- ------------------------------------------------------------
-- 3. 재고관리 프로시저
-- ------------------------------------------------------------

-- 3.1 재고 입고 처리 프로시저
CREATE PROCEDURE SP_STOCK_IN (
    IN p_plant_cd VARCHAR(20),
    IN p_warehouse_cd VARCHAR(20),
    IN p_item_cd VARCHAR(50),
    IN p_lot_no VARCHAR(50),
    IN p_qty DECIMAL(15,3),
    IN p_trans_reason VARCHAR(50),
    IN p_ref_type VARCHAR(20),
    IN p_ref_no VARCHAR(50),
    IN p_unit_price DECIMAL(18,4),
    IN p_user_id VARCHAR(50)
)
BEGIN
    DECLARE v_trans_no VARCHAR(30);
    DECLARE v_unit VARCHAR(20);
    DECLARE v_before_qty DECIMAL(15,3) DEFAULT 0;
    
    -- 품목 단위 조회
    SELECT UNIT INTO v_unit FROM MST_ITEM WHERE ITEM_CD = p_item_cd;
    
    -- 현재 재고 조회
    SELECT COALESCE(STOCK_QTY, 0) INTO v_before_qty
    FROM INV_STOCK
    WHERE PLANT_CD = p_plant_cd AND WAREHOUSE_CD = p_warehouse_cd 
      AND ITEM_CD = p_item_cd AND COALESCE(LOT_NO, '') = COALESCE(p_lot_no, '');
    
    -- 재고 증가
    INSERT INTO INV_STOCK (
        PLANT_CD, WAREHOUSE_CD, ITEM_CD, LOT_NO, STOCK_QTY, UNIT,
        STOCK_STATUS, LAST_IN_DT, UNIT_COST, STOCK_AMT, REG_USER_ID
    ) VALUES (
        p_plant_cd, p_warehouse_cd, p_item_cd, p_lot_no, p_qty, v_unit,
        '정상', CURDATE(), p_unit_price, p_qty * COALESCE(p_unit_price, 0), p_user_id
    )
    ON DUPLICATE KEY UPDATE
        STOCK_QTY = STOCK_QTY + p_qty,
        LAST_IN_DT = CURDATE(),
        STOCK_AMT = (STOCK_QTY + p_qty) * COALESCE(p_unit_price, UNIT_COST),
        UPD_USER_ID = p_user_id,
        UPD_DTM = NOW();
    
    -- 입출고 이력 등록
    CALL SP_GET_SEQ_NO('TR', 'TR', 4, v_trans_no);
    
    INSERT INTO INV_TRANS_HIS (
        PLANT_CD, TRANS_NO, TRANS_DT, TRANS_TYPE, TRANS_REASON,
        ITEM_CD, LOT_NO, TRANS_QTY, UNIT, TO_WAREHOUSE_CD,
        BEFORE_QTY, AFTER_QTY, REF_TYPE, REF_NO,
        UNIT_PRICE, TRANS_AMT, TRANS_USER_ID, REG_USER_ID
    ) VALUES (
        p_plant_cd, v_trans_no, CURDATE(), '입고', p_trans_reason,
        p_item_cd, p_lot_no, p_qty, v_unit, p_warehouse_cd,
        v_before_qty, v_before_qty + p_qty, p_ref_type, p_ref_no,
        p_unit_price, p_qty * COALESCE(p_unit_price, 0), p_user_id, p_user_id
    );
    
END //

-- 3.2 재고 출고 처리 프로시저
CREATE PROCEDURE SP_STOCK_OUT (
    IN p_plant_cd VARCHAR(20),
    IN p_warehouse_cd VARCHAR(20),
    IN p_item_cd VARCHAR(50),
    IN p_lot_no VARCHAR(50),
    IN p_qty DECIMAL(15,3),
    IN p_trans_reason VARCHAR(50),
    IN p_ref_type VARCHAR(20),
    IN p_ref_no VARCHAR(50),
    IN p_user_id VARCHAR(50),
    OUT p_result INT
)
BEGIN
    DECLARE v_trans_no VARCHAR(30);
    DECLARE v_unit VARCHAR(20);
    DECLARE v_stock_qty DECIMAL(15,3) DEFAULT 0;
    DECLARE v_unit_cost DECIMAL(18,4) DEFAULT 0;
    
    SET p_result = 0;
    
    -- 현재 재고 조회
    SELECT STOCK_QTY, UNIT_COST INTO v_stock_qty, v_unit_cost
    FROM INV_STOCK
    WHERE PLANT_CD = p_plant_cd AND WAREHOUSE_CD = p_warehouse_cd 
      AND ITEM_CD = p_item_cd AND COALESCE(LOT_NO, '') = COALESCE(p_lot_no, '')
      AND STOCK_STATUS = '정상';
    
    -- 재고 부족 체크
    IF v_stock_qty < p_qty THEN
        SET p_result = -1; -- 재고 부족
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '재고가 부족합니다.';
    END IF;
    
    -- 품목 단위 조회
    SELECT UNIT INTO v_unit FROM MST_ITEM WHERE ITEM_CD = p_item_cd;
    
    -- 재고 차감
    UPDATE INV_STOCK
    SET STOCK_QTY = STOCK_QTY - p_qty,
        STOCK_AMT = (STOCK_QTY - p_qty) * COALESCE(UNIT_COST, 0),
        LAST_OUT_DT = CURDATE(),
        UPD_USER_ID = p_user_id,
        UPD_DTM = NOW()
    WHERE PLANT_CD = p_plant_cd AND WAREHOUSE_CD = p_warehouse_cd 
      AND ITEM_CD = p_item_cd AND COALESCE(LOT_NO, '') = COALESCE(p_lot_no, '');
    
    -- 입출고 이력 등록
    CALL SP_GET_SEQ_NO('TR', 'TR', 4, v_trans_no);
    
    INSERT INTO INV_TRANS_HIS (
        PLANT_CD, TRANS_NO, TRANS_DT, TRANS_TYPE, TRANS_REASON,
        ITEM_CD, LOT_NO, TRANS_QTY, UNIT, FROM_WAREHOUSE_CD,
        BEFORE_QTY, AFTER_QTY, REF_TYPE, REF_NO,
        UNIT_PRICE, TRANS_AMT, TRANS_USER_ID, REG_USER_ID
    ) VALUES (
        p_plant_cd, v_trans_no, CURDATE(), '출고', p_trans_reason,
        p_item_cd, p_lot_no, p_qty, v_unit, p_warehouse_cd,
        v_stock_qty, v_stock_qty - p_qty, p_ref_type, p_ref_no,
        v_unit_cost, p_qty * COALESCE(v_unit_cost, 0), p_user_id, p_user_id
    );
    
    -- LOT 현재수량 업데이트
    IF p_lot_no IS NOT NULL THEN
        UPDATE INV_LOT
        SET CURRENT_QTY = CURRENT_QTY - p_qty,
            UPD_USER_ID = p_user_id,
            UPD_DTM = NOW()
        WHERE LOT_NO = p_lot_no;
    END IF;
    
    SET p_result = 1; -- 성공
    
END //

DELIMITER ;

-- ============================================================
-- PART 3: 초기 데이터 입력
-- ============================================================

-- ------------------------------------------------------------
-- 1. 공통코드 초기 데이터
-- ------------------------------------------------------------

-- 코드그룹 등록
INSERT INTO COM_CODE_GRP (GRP_CD, GRP_NM, GRP_DESC, USE_YN, REG_USER_ID) VALUES
('ITEM_TYPE', '품목유형', '품목의 유형을 구분하는 코드', 'Y', 'SYSTEM'),
('WO_STATUS', '작업상태', '작업지시의 상태를 구분하는 코드', 'Y', 'SYSTEM'),
('PLAN_STATUS', '계획상태', '생산계획의 상태를 구분하는 코드', 'Y', 'SYSTEM'),
('INSPECT_TYPE', '검사유형', '품질검사의 유형을 구분하는 코드', 'Y', 'SYSTEM'),
('JUDGE_RESULT', '판정결과', '검사 판정결과를 구분하는 코드', 'Y', 'SYSTEM'),
('TRANS_TYPE', '거래유형', '재고 거래유형을 구분하는 코드', 'Y', 'SYSTEM'),
('TRANS_REASON', '거래사유', '재고 거래사유를 구분하는 코드', 'Y', 'SYSTEM'),
('STOCK_STATUS', '재고상태', '재고의 상태를 구분하는 코드', 'Y', 'SYSTEM'),
('LOT_STATUS', 'LOT상태', 'LOT의 상태를 구분하는 코드', 'Y', 'SYSTEM'),
('OPER_STATUS', '가동상태', '설비 가동상태를 구분하는 코드', 'Y', 'SYSTEM'),
('DOWNTIME_TYPE', '비가동유형', '설비 비가동유형을 구분하는 코드', 'Y', 'SYSTEM'),
('MAINT_TYPE', '정비유형', '설비 정비유형을 구분하는 코드', 'Y', 'SYSTEM'),
('MAINT_RESULT', '정비결과', '정비 결과를 구분하는 코드', 'Y', 'SYSTEM'),
('DISPOSITION', '처리방법', '불량품 처리방법을 구분하는 코드', 'Y', 'SYSTEM'),
('DEFECT_TYPE', '불량유형', '불량의 유형을 구분하는 코드', 'Y', 'SYSTEM'),
('SHIFT', '근무조', '근무조를 구분하는 코드', 'Y', 'SYSTEM'),
('SKILL_LEVEL', '숙련도', '작업자 숙련도를 구분하는 코드', 'Y', 'SYSTEM'),
('WORKER_TYPE', '작업자유형', '작업자 고용유형을 구분하는 코드', 'Y', 'SYSTEM'),
('VENDOR_TYPE', '거래처유형', '거래처 유형을 구분하는 코드', 'Y', 'SYSTEM'),
('WH_TYPE', '창고유형', '창고 유형을 구분하는 코드', 'Y', 'SYSTEM'),
('UNIT', '단위', '수량 단위를 구분하는 코드', 'Y', 'SYSTEM'),
('CYCLE_TYPE', '주기유형', '정비주기 유형을 구분하는 코드', 'Y', 'SYSTEM'),
('EQP_STATUS', '설비상태', '설비의 상태를 구분하는 코드', 'Y', 'SYSTEM'),
('DATA_TYPE', '데이터유형', '검사항목의 데이터 유형', 'Y', 'SYSTEM');

-- 공통코드 등록
-- 품목유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('ITEM_TYPE', 'RAW', '원자재', 1, 'Y', 'SYSTEM'),
('ITEM_TYPE', 'HALF', '반제품', 2, 'Y', 'SYSTEM'),
('ITEM_TYPE', 'PRODUCT', '완제품', 3, 'Y', 'SYSTEM'),
('ITEM_TYPE', 'PART', '부품', 4, 'Y', 'SYSTEM'),
('ITEM_TYPE', 'CONSUMABLE', '소모품', 5, 'Y', 'SYSTEM');

-- 작업상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('WO_STATUS', 'WAIT', '대기', 1, 'Y', 'SYSTEM'),
('WO_STATUS', 'PROGRESS', '진행', 2, 'Y', 'SYSTEM'),
('WO_STATUS', 'COMPLETE', '완료', 3, 'Y', 'SYSTEM'),
('WO_STATUS', 'CANCEL', '취소', 4, 'Y', 'SYSTEM'),
('WO_STATUS', 'HOLD', '보류', 5, 'Y', 'SYSTEM');

-- 계획상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('PLAN_STATUS', 'PLAN', '계획', 1, 'Y', 'SYSTEM'),
('PLAN_STATUS', 'CONFIRM', '확정', 2, 'Y', 'SYSTEM'),
('PLAN_STATUS', 'PROGRESS', '진행', 3, 'Y', 'SYSTEM'),
('PLAN_STATUS', 'COMPLETE', '완료', 4, 'Y', 'SYSTEM'),
('PLAN_STATUS', 'CANCEL', '취소', 5, 'Y', 'SYSTEM');

-- 검사유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('INSPECT_TYPE', 'IQC', '수입검사', 1, 'Y', 'SYSTEM'),
('INSPECT_TYPE', 'PQC', '공정검사', 2, 'Y', 'SYSTEM'),
('INSPECT_TYPE', 'FQC', '최종검사', 3, 'Y', 'SYSTEM'),
('INSPECT_TYPE', 'OQC', '출하검사', 4, 'Y', 'SYSTEM');

-- 판정결과
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('JUDGE_RESULT', 'WAIT', '대기', 1, 'Y', 'SYSTEM'),
('JUDGE_RESULT', 'PASS', '합격', 2, 'Y', 'SYSTEM'),
('JUDGE_RESULT', 'FAIL', '불합격', 3, 'Y', 'SYSTEM'),
('JUDGE_RESULT', 'COND_PASS', '조건부합격', 4, 'Y', 'SYSTEM');

-- 거래유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('TRANS_TYPE', 'IN', '입고', 1, 'Y', 'SYSTEM'),
('TRANS_TYPE', 'OUT', '출고', 2, 'Y', 'SYSTEM'),
('TRANS_TYPE', 'MOVE', '이동', 3, 'Y', 'SYSTEM'),
('TRANS_TYPE', 'ADJ', '조정', 4, 'Y', 'SYSTEM');

-- 거래사유
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, ATTR1, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('TRANS_REASON', 'PUR_IN', '구매입고', 'IN', 1, 'Y', 'SYSTEM'),
('TRANS_REASON', 'PRD_IN', '생산입고', 'IN', 2, 'Y', 'SYSTEM'),
('TRANS_REASON', 'RTN_IN', '반품입고', 'IN', 3, 'Y', 'SYSTEM'),
('TRANS_REASON', 'ADJ_IN', '조정입고', 'IN', 4, 'Y', 'SYSTEM'),
('TRANS_REASON', 'SAL_OUT', '판매출고', 'OUT', 5, 'Y', 'SYSTEM'),
('TRANS_REASON', 'PRD_OUT', '생산출고', 'OUT', 6, 'Y', 'SYSTEM'),
('TRANS_REASON', 'SCRAP', '폐기', 'OUT', 7, 'Y', 'SYSTEM'),
('TRANS_REASON', 'ADJ_OUT', '조정출고', 'OUT', 8, 'Y', 'SYSTEM'),
('TRANS_REASON', 'WH_MOVE', '창고이동', 'MOVE', 9, 'Y', 'SYSTEM');

-- 재고상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('STOCK_STATUS', 'NORMAL', '정상', 1, 'Y', 'SYSTEM'),
('STOCK_STATUS', 'HOLD', '보류', 2, 'Y', 'SYSTEM'),
('STOCK_STATUS', 'DEFECT', '불량', 3, 'Y', 'SYSTEM'),
('STOCK_STATUS', 'SCRAP', '폐기', 4, 'Y', 'SYSTEM');

-- LOT상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('LOT_STATUS', 'NORMAL', '정상', 1, 'Y', 'SYSTEM'),
('LOT_STATUS', 'HOLD', '보류', 2, 'Y', 'SYSTEM'),
('LOT_STATUS', 'SCRAP', '폐기', 3, 'Y', 'SYSTEM');

-- 가동상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('OPER_STATUS', 'RUN', '가동', 1, 'Y', 'SYSTEM'),
('OPER_STATUS', 'DOWN', '비가동', 2, 'Y', 'SYSTEM'),
('OPER_STATUS', 'IDLE', '대기', 3, 'Y', 'SYSTEM');

-- 비가동유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('DOWNTIME_TYPE', 'BREAKDOWN', '고장', 1, 'Y', 'SYSTEM'),
('DOWNTIME_TYPE', 'MAINT', '정비', 2, 'Y', 'SYSTEM'),
('DOWNTIME_TYPE', 'SETUP', '작업준비', 3, 'Y', 'SYSTEM'),
('DOWNTIME_TYPE', 'MATERIAL', '자재대기', 4, 'Y', 'SYSTEM'),
('DOWNTIME_TYPE', 'QUALITY', '품질문제', 5, 'Y', 'SYSTEM'),
('DOWNTIME_TYPE', 'OTHER', '기타', 6, 'Y', 'SYSTEM');

-- 정비유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('MAINT_TYPE', 'PM', '예방정비', 1, 'Y', 'SYSTEM'),
('MAINT_TYPE', 'CM', '사후정비', 2, 'Y', 'SYSTEM'),
('MAINT_TYPE', 'BM', '개량정비', 3, 'Y', 'SYSTEM'),
('MAINT_TYPE', 'DM', '일상점검', 4, 'Y', 'SYSTEM');

-- 정비결과
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('MAINT_RESULT', 'PROGRESS', '진행중', 1, 'Y', 'SYSTEM'),
('MAINT_RESULT', 'COMPLETE', '완료', 2, 'Y', 'SYSTEM'),
('MAINT_RESULT', 'HOLD', '보류', 3, 'Y', 'SYSTEM');

-- 처리방법
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('DISPOSITION', 'SCRAP', '폐기', 1, 'Y', 'SYSTEM'),
('DISPOSITION', 'REWORK', '재작업', 2, 'Y', 'SYSTEM'),
('DISPOSITION', 'REINSPECT', '재검사', 3, 'Y', 'SYSTEM'),
('DISPOSITION', 'SPECIAL', '특채', 4, 'Y', 'SYSTEM'),
('DISPOSITION', 'RETURN', '반품', 5, 'Y', 'SYSTEM');

-- 불량유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('DEFECT_TYPE', 'DIMENSION', '치수불량', 1, 'Y', 'SYSTEM'),
('DEFECT_TYPE', 'APPEARANCE', '외관불량', 2, 'Y', 'SYSTEM'),
('DEFECT_TYPE', 'FUNCTION', '기능불량', 3, 'Y', 'SYSTEM'),
('DEFECT_TYPE', 'MATERIAL', '자재불량', 4, 'Y', 'SYSTEM'),
('DEFECT_TYPE', 'OTHER', '기타불량', 5, 'Y', 'SYSTEM');

-- 근무조
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('SHIFT', 'DAY', '주간', 1, 'Y', 'SYSTEM'),
('SHIFT', 'NIGHT', '야간', 2, 'Y', 'SYSTEM'),
('SHIFT', 'MID', '중간', 3, 'Y', 'SYSTEM');

-- 숙련도
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('SKILL_LEVEL', 'BEGINNER', '초급', 1, 'Y', 'SYSTEM'),
('SKILL_LEVEL', 'INTER', '중급', 2, 'Y', 'SYSTEM'),
('SKILL_LEVEL', 'ADVANCED', '고급', 3, 'Y', 'SYSTEM'),
('SKILL_LEVEL', 'EXPERT', '전문가', 4, 'Y', 'SYSTEM');

-- 작업자유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('WORKER_TYPE', 'REGULAR', '정규직', 1, 'Y', 'SYSTEM'),
('WORKER_TYPE', 'CONTRACT', '계약직', 2, 'Y', 'SYSTEM'),
('WORKER_TYPE', 'TEMP', '파견직', 3, 'Y', 'SYSTEM');

-- 거래처유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('VENDOR_TYPE', 'SUPPLIER', '매입처', 1, 'Y', 'SYSTEM'),
('VENDOR_TYPE', 'CUSTOMER', '매출처', 2, 'Y', 'SYSTEM'),
('VENDOR_TYPE', 'BOTH', '매입/매출', 3, 'Y', 'SYSTEM');

-- 창고유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('WH_TYPE', 'RAW', '원자재창고', 1, 'Y', 'SYSTEM'),
('WH_TYPE', 'WIP', '재공창고', 2, 'Y', 'SYSTEM'),
('WH_TYPE', 'PRODUCT', '완제품창고', 3, 'Y', 'SYSTEM'),
('WH_TYPE', 'DEFECT', '불량품창고', 4, 'Y', 'SYSTEM'),
('WH_TYPE', 'RETURN', '반품창고', 5, 'Y', 'SYSTEM');

-- 단위
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('UNIT', 'EA', '개', 1, 'Y', 'SYSTEM'),
('UNIT', 'KG', 'Kg', 2, 'Y', 'SYSTEM'),
('UNIT', 'G', 'g', 3, 'Y', 'SYSTEM'),
('UNIT', 'L', 'L', 4, 'Y', 'SYSTEM'),
('UNIT', 'ML', 'mL', 5, 'Y', 'SYSTEM'),
('UNIT', 'M', 'm', 6, 'Y', 'SYSTEM'),
('UNIT', 'MM', 'mm', 7, 'Y', 'SYSTEM'),
('UNIT', 'SET', 'Set', 8, 'Y', 'SYSTEM'),
('UNIT', 'BOX', 'Box', 9, 'Y', 'SYSTEM'),
('UNIT', 'ROLL', 'Roll', 10, 'Y', 'SYSTEM');

-- 주기유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('CYCLE_TYPE', 'DAY', '일', 1, 'Y', 'SYSTEM'),
('CYCLE_TYPE', 'WEEK', '주', 2, 'Y', 'SYSTEM'),
('CYCLE_TYPE', 'MONTH', '월', 3, 'Y', 'SYSTEM'),
('CYCLE_TYPE', 'QUARTER', '분기', 4, 'Y', 'SYSTEM'),
('CYCLE_TYPE', 'YEAR', '년', 5, 'Y', 'SYSTEM'),
('CYCLE_TYPE', 'HOUR', '시간', 6, 'Y', 'SYSTEM');

-- 설비상태
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('EQP_STATUS', 'RUN', '가동', 1, 'Y', 'SYSTEM'),
('EQP_STATUS', 'STOP', '비가동', 2, 'Y', 'SYSTEM'),
('EQP_STATUS', 'MAINT', '정비중', 3, 'Y', 'SYSTEM'),
('EQP_STATUS', 'IDLE', '대기', 4, 'Y', 'SYSTEM');

-- 데이터유형
INSERT INTO COM_CODE (GRP_CD, COM_CD, COM_NM, SORT_SEQ, USE_YN, REG_USER_ID) VALUES
('DATA_TYPE', 'NUMBER', '숫자', 1, 'Y', 'SYSTEM'),
('DATA_TYPE', 'TEXT', '텍스트', 2, 'Y', 'SYSTEM'),
('DATA_TYPE', 'YN', '예/아니오', 3, 'Y', 'SYSTEM'),
('DATA_TYPE', 'SELECT', '선택', 4, 'Y', 'SYSTEM');

-- ------------------------------------------------------------
-- 2. 단위정보 초기 데이터
-- ------------------------------------------------------------
INSERT INTO MST_UNIT (UNIT_CD, UNIT_NM, UNIT_TYPE, USE_YN, REG_USER_ID) VALUES
('EA', '개', '수량', 'Y', 'SYSTEM'),
('KG', 'Kg', '중량', 'Y', 'SYSTEM'),
('G', 'g', '중량', 'Y', 'SYSTEM'),
('L', 'L', '용량', 'Y', 'SYSTEM'),
('ML', 'mL', '용량', 'Y', 'SYSTEM'),
('M', 'm', '길이', 'Y', 'SYSTEM'),
('MM', 'mm', '길이', 'Y', 'SYSTEM'),
('CM', 'cm', '길이', 'Y', 'SYSTEM'),
('SET', 'Set', '수량', 'Y', 'SYSTEM'),
('BOX', 'Box', '수량', 'Y', 'SYSTEM'),
('ROLL', 'Roll', '수량', 'Y', 'SYSTEM'),
('SHEET', 'Sheet', '수량', 'Y', 'SYSTEM'),
('PCS', 'Pcs', '수량', 'Y', 'SYSTEM');

-- ============================================================
-- 스크립트 완료
-- ============================================================
