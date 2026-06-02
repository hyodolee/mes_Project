-- ============================================================
-- MCS Route Demo CV-01 Block Bypass Patch
-- Purpose: Add a bypass path when the NCM-01-01 -> CV-01 edge is blocked.
--
-- Scenario:
--   Blocked edge:
--     E-NCM01-CV01  N-NCM-01-01 -> N-CV-01
--
--   Expected bypass route:
--     N-NCM-01-01 -> N-BUF-01 -> N-NCM-01-02
-- ============================================================

SET NAMES utf8mb4;

-- Add NCM-01-01 -> bypass buffer.
INSERT INTO MCS_ROUTE_EDGE
    (PLANT_CD, EDGE_CD, EDGE_NM, FROM_NODE_ID, TO_NODE_ID, BIDIRECTIONAL_YN,
     DISTANCE_M, TRAVEL_TIME_SEC, BASE_COST, EDGE_STATUS, USE_YN, REG_USER_ID)
SELECT
    'P001',
    'E-NCM01-BUF01',
    'NCM-01-01 to bypass buffer',
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
       AND to_node.NODE_CD = 'N-BUF-01'
WHERE from_node.PLANT_CD = 'P001'
  AND from_node.NODE_CD = 'N-NCM-01-01'
  AND NOT EXISTS (
      SELECT 1
      FROM MCS_ROUTE_EDGE existing
      WHERE existing.PLANT_CD = 'P001'
        AND existing.EDGE_CD = 'E-NCM01-BUF01'
  );

-- Add bypass buffer -> NCM-01-02 if it does not exist yet.
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

-- Force the bypass path open.
UPDATE MCS_ROUTE_EDGE
SET EDGE_STATUS = 'AVAILABLE',
    USE_YN = 'Y',
    UPD_USER_ID = 'SYSTEM',
    UPD_DTM = NOW()
WHERE PLANT_CD = 'P001'
  AND EDGE_CD IN ('E-NCM01-BUF01', 'E-BUF01-NCM02');

-- For this specific demo, block only the original first edge.
UPDATE MCS_ROUTE_EDGE
SET EDGE_STATUS = 'BLOCKED',
    USE_YN = 'Y',
    UPD_USER_ID = 'SYSTEM',
    UPD_DTM = NOW()
WHERE PLANT_CD = 'P001'
  AND EDGE_CD = 'E-NCM01-CV01';

-- Keep the destination-side edge open. It is not used in the bypass,
-- but this makes the demo state easier to read in the route management screen.
UPDATE MCS_ROUTE_EDGE
SET EDGE_STATUS = 'AVAILABLE',
    USE_YN = 'Y',
    UPD_USER_ID = 'SYSTEM',
    UPD_DTM = NOW()
WHERE PLANT_CD = 'P001'
  AND EDGE_CD = 'E-NCM02-CV01';

-- Verification: all rows below must appear.
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
  AND e.EDGE_CD IN ('E-NCM01-CV01', 'E-NCM02-CV01', 'E-NCM01-BUF01', 'E-BUF01-NCM02')
ORDER BY e.EDGE_CD;

