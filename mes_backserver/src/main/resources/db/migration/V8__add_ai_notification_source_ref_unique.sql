DELETE n1
FROM MES_AI_NOTIFICATION n1
JOIN MES_AI_NOTIFICATION n2
  ON n1.source_ref = n2.source_ref
 AND n1.id > n2.id
WHERE n1.source_ref IS NOT NULL
  AND n1.source_ref <> '';

CREATE UNIQUE INDEX uq_ai_notification_source_ref
    ON MES_AI_NOTIFICATION (source_ref);
