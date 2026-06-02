-- MCS transfer failure status used by PLC error/interlock scenarios.
-- Safe to run multiple times.

INSERT INTO COM_CODE (
    GRP_CD, COM_CD, COM_NM, COM_DESC, SORT_SEQ, ATTR1, ATTR2, ATTR3, USE_YN, REG_USER_ID, REG_DTM
)
SELECT 'MCS_TF_STATUS', 'FAILED', '실패', 'PLC 실패 또는 인터락으로 이동 실패', 5, NULL, NULL, NULL, 'Y', 'SYSTEM', NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM COM_CODE
    WHERE GRP_CD = 'MCS_TF_STATUS'
      AND COM_CD = 'FAILED'
);
