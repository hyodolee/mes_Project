-- Add search/list indexes for master domain tables.
-- Idempotent pattern: create only when index does not exist.

-- MST_COMPANY: 회사명/사용여부 조회 최적화
SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX IDX_MST_COMPANY_01 ON MST_COMPANY (COMPANY_NM, USE_YN)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'MST_COMPANY'
      AND index_name = 'IDX_MST_COMPANY_01'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- MST_COMPANY: 사용여부 단독 필터 최적화
SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX IDX_MST_COMPANY_02 ON MST_COMPANY (USE_YN)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'MST_COMPANY'
      AND index_name = 'IDX_MST_COMPANY_02'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- MST_PLANT: 회사/사용여부 조건 조회 최적화
SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX IDX_MST_PLANT_01 ON MST_PLANT (COMPANY_CD, USE_YN)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'MST_PLANT'
      AND index_name = 'IDX_MST_PLANT_01'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- MST_PLANT: 공장명 검색 최적화
SET @sql := (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX IDX_MST_PLANT_02 ON MST_PLANT (PLANT_NM)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'MST_PLANT'
      AND index_name = 'IDX_MST_PLANT_02'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
