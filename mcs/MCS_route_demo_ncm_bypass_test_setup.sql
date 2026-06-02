-- ============================================================
-- MCS Route Demo NCM Bypass Test Setup
-- Purpose: Force the demo route data into a known-good state for
--          NCM-01-01 -> NCM-01-02 bypass testing.
-- ============================================================

SET NAMES utf8mb4;

-- 1. Ensure the bypass edge exists.
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

-- 2. Force the three edges needed for the bypass path to be available.
UPDATE MCS_ROUTE_EDGE
SET EDGE_STATUS = 'AVAILABLE',
    USE_YN = 'Y',
    UPD_USER_ID = 'SYSTEM',
    UPD_DTM = NOW()
WHERE PLANT_CD = 'P001'
  AND EDGE_CD IN ('E-NCM01-CV01', 'E-CV01-BUF01', 'E-BUF01-NCM02');

-- 3. Block only the original direct destination edge.
-- This is the failure condition that should make MCS choose the bypass.
UPDATE MCS_ROUTE_EDGE
SET EDGE_STATUS = 'BLOCKED',
    USE_YN = 'Y',
    UPD_USER_ID = 'SYSTEM',
    UPD_DTM = NOW()
WHERE PLANT_CD = 'P001'
  AND EDGE_CD = 'E-NCM02-CV01';

-- 4. Verification: all four rows must appear.
-- Expected:
-- E-NCM01-CV01   AVAILABLE
-- E-NCM02-CV01   BLOCKED
-- E-CV01-BUF01   AVAILABLE
-- E-BUF01-NCM02  AVAILABLE
SELECT
    e.EDGE_CD,
    fn.NODE_CD AS FROM_NODE,
    tn.NODE_CD AS TO_NODE,
    e.BIDIRECTIONAL_YN,
    e.EDGE_STATUS,
    e.USE_YN
FROM MCS_ROUTE_EDGE e
INNER JOIN MCS_ROUTE_NODE fn ON e.FROM_NODE_ID = fn.ROUTE_NODE_ID
INNER JOIN MCS_ROUTE_NODE tn ON e.TO_NODE_ID = tn.ROUTE_NODE_ID
WHERE e.PLANT_CD = 'P001'
  AND e.EDGE_CD IN ('E-NCM01-CV01', 'E-NCM02-CV01', 'E-CV01-BUF01', 'E-BUF01-NCM02')
ORDER BY e.EDGE_CD;

