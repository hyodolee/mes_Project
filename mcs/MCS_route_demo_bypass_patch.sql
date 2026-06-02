-- ============================================================
-- MCS Route Demo Bypass Patch
-- Purpose: Add a demo bypass edge for NCM-01-01 -> NCM-01-02 tests.
-- Run this once if MCS_route_optimization.sql was already executed.
-- ============================================================

SET NAMES utf8mb4;

-- Do not hard-code ROUTE_EDGE_ID here. Some local DBs may already have
-- route edge id 12 from test data, which would make INSERT IGNORE skip
-- the bypass edge silently.
INSERT INTO MCS_ROUTE_EDGE
    (PLANT_CD, EDGE_CD, EDGE_NM, FROM_NODE_ID, TO_NODE_ID, BIDIRECTIONAL_YN,
     DISTANCE_M, TRAVEL_TIME_SEC, BASE_COST, EDGE_STATUS, USE_YN, REG_USER_ID)
SELECT
    'P001',
    'E-BUF01-NCM02',
    'Bypass buffer to NCM-01-02',
    from_node.ROUTE_NODE_ID,
    to_node.ROUTE_NODE_ID,
    'Y',
    30.00,
    85,
    85.00,
    'AVAILABLE',
    'Y',
    'SYSTEM'
FROM MCS_ROUTE_NODE from_node
INNER JOIN MCS_ROUTE_NODE to_node
        ON to_node.PLANT_CD = 'P001'
       AND to_node.NODE_CD = 'N-NCM-01-02'
WHERE from_node.PLANT_CD = 'P001'
  AND from_node.NODE_CD = 'N-BUF-01'
  AND NOT EXISTS (
      SELECT 1
      FROM MCS_ROUTE_EDGE existing
      WHERE existing.PLANT_CD = 'P001'
        AND existing.EDGE_CD = 'E-BUF01-NCM02'
  );

-- Optional reset for this demo route.
-- If you already changed this edge while testing, this makes sure the
-- bypass edge is available.
UPDATE MCS_ROUTE_EDGE
SET EDGE_STATUS = 'AVAILABLE',
    USE_YN = 'Y',
    UPD_USER_ID = 'SYSTEM',
    UPD_DTM = NOW()
WHERE PLANT_CD = 'P001'
  AND EDGE_CD = 'E-BUF01-NCM02';

-- Verification 1: this must return one row.
SELECT
    e.ROUTE_EDGE_ID,
    e.EDGE_CD,
    fn.NODE_CD AS FROM_NODE,
    tn.NODE_CD AS TO_NODE,
    e.EDGE_STATUS
FROM MCS_ROUTE_EDGE e
INNER JOIN MCS_ROUTE_NODE fn ON e.FROM_NODE_ID = fn.ROUTE_NODE_ID
INNER JOIN MCS_ROUTE_NODE tn ON e.TO_NODE_ID = tn.ROUTE_NODE_ID
WHERE e.PLANT_CD = 'P001'
  AND e.EDGE_CD = 'E-BUF01-NCM02';

-- Test flow:
-- 1. Set E-NCM02-CV01 to BLOCKED.
-- 2. Calculate NCM-01-01 -> NCM-01-02 again.
-- 3. Expected route:
--    N-NCM-01-01 -> N-CV-01 -> N-BUF-01 -> N-NCM-01-02

