package com.mes.application.service.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationGraphSeeder {

    private final Neo4jClient neo4jClient;

    @Value("${ai.graph-rag.enabled:true}")
    private boolean graphRagEnabled;

    @Value("${ai.graph-rag.seed-enabled:true}")
    private boolean seedEnabled;

    @Value("${ai.graph-rag.seed-retry-attempts:6}")
    private int seedRetryAttempts;

    @Value("${ai.graph-rag.seed-retry-delay-ms:3000}")
    private long seedRetryDelayMs;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (!graphRagEnabled) {
            log.info("[GRAPH-RAG] Neo4j graph seed disabled");
            return;
        }
        if (!seedEnabled) {
            return;
        }

        CompletableFuture.runAsync(this::seedWithRetry);
    }

    private void seedWithRetry() {
        int attempts = Math.max(seedRetryAttempts, 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                seedOnce();
                log.info("[GRAPH-RAG] Neo4j operation graph seed completed");
                return;
            } catch (Exception e) {
                if (attempt >= attempts) {
                    log.warn("[GRAPH-RAG] Neo4j operation graph seed skipped after {} attempts: {}", attempts, e.getMessage());
                    return;
                }
                log.info("[GRAPH-RAG] Neo4j is not ready yet. Retrying seed ({}/{}): {}", attempt, attempts, e.getMessage());
                sleepBeforeRetry();
            }
        }
    }

    private void seedOnce() {
        try {
            neo4jClient.query("""
                    CREATE CONSTRAINT operation_concept_code IF NOT EXISTS
                    FOR (n:OperationConcept)
                    REQUIRE n.code IS UNIQUE
                    """).run();

            seedTransferStarted();
            seedLotMissing();
            seedDestinationBlocked();
            seedLocationMismatch();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(Math.max(seedRetryDelayMs, 0));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void seedTransferStarted() {
        neo4jClient.query("""
                MERGE (e:OperationConcept {code: 'TRANSFER_STARTED'})
                SET e.type = 'Event',
                    e.name = '이동 시작 이벤트',
                    e.keywords = ['TRANSFER_STARTED', '이동 시작', '이동 실패', '목적지 정보', '목적지 누락']

                MERGE (f:OperationConcept {code: 'toLocationCd'})
                SET f.type = 'RequiredField',
                    f.name = '목적지 정보',
                    f.keywords = ['toLocationCd', '목적지', '도착지', '목적지 정보']

                MERGE (t:OperationConcept {code: 'TO_LOCATION_CD'})
                SET t.type = 'PlcTag',
                    t.name = 'PLC 목적지 태그',
                    t.keywords = ['TO_LOCATION_CD', '목적지 태그', 'PLC 태그']

                MERGE (s:OperationConcept {code: 'SOP_MISSING_DESTINATION'})
                SET s.type = 'Sop',
                    s.name = '목적지 정보 누락 조치',
                    s.keywords = ['목적지 누락', '필수값 누락', 'VALIDATION_FAILED']

                MERGE (c:OperationConcept {code: 'PLC_PAYLOAD_BUILDER'})
                SET c.type = 'Checkpoint',
                    c.name = 'PLC payload 송신부',
                    c.keywords = ['payload', '페이로드', '송신부', 'PLC payload']

                MERGE (e)-[:REQUIRES {label: '이동 시작에는 목적지 정보가 필요'}]->(f)
                MERGE (f)-[:MAPPED_TO {label: '목적지 정보는 PLC 목적지 태그와 연결'}]->(t)
                MERGE (f)-[:HANDLED_BY {label: '목적지 누락 시 조치 절차'}]->(s)
                MERGE (s)-[:CHECKS {label: '먼저 확인할 위치'}]->(c)
                """).run();
    }

    private void seedLotMissing() {
        neo4jClient.query("""
                MERGE (e:OperationConcept {code: 'LOT_SCANNED'})
                SET e.type = 'Event',
                    e.name = 'LOT 스캔 이벤트',
                    e.keywords = ['LOT_SCANNED', 'LOT', 'lotNo', '로트', '바코드']

                MERGE (f:OperationConcept {code: 'lotNo'})
                SET f.type = 'RequiredField',
                    f.name = 'LOT 번호',
                    f.keywords = ['lotNo', 'LOT 번호', '로트 번호', 'LOT 누락']

                MERGE (t:OperationConcept {code: 'LOT_NO'})
                SET t.type = 'PlcTag',
                    t.name = 'PLC LOT 태그',
                    t.keywords = ['LOT_NO', 'LOT 태그', '바코드 태그']

                MERGE (s:OperationConcept {code: 'SOP_MISSING_LOT'})
                SET s.type = 'Sop',
                    s.name = 'LOT 번호 누락 조치',
                    s.keywords = ['LOT 누락', '로트 누락', '바코드 누락', 'VALIDATION_FAILED']

                MERGE (c:OperationConcept {code: 'BARCODE_READER'})
                SET c.type = 'Checkpoint',
                    c.name = '바코드 리더',
                    c.keywords = ['바코드 리더', '스캐너', 'LOT 스캔']

                MERGE (e)-[:REQUIRES {label: 'LOT 스캔에는 LOT 번호가 필요'}]->(f)
                MERGE (f)-[:MAPPED_TO {label: 'LOT 번호는 PLC LOT 태그와 연결'}]->(t)
                MERGE (f)-[:HANDLED_BY {label: 'LOT 누락 시 조치 절차'}]->(s)
                MERGE (s)-[:CHECKS {label: '먼저 확인할 위치'}]->(c)
                """).run();
    }

    private void seedDestinationBlocked() {
        neo4jClient.query("""
                MERGE (e:OperationConcept {code: 'DESTINATION_BLOCKED'})
                SET e.type = 'Interlock',
                    e.name = '목적지 점유 인터록',
                    e.keywords = ['DESTINATION_BLOCKED', '목적지 점유', '이동 차단', '인터록', '도착지 점유']

                MERGE (f:OperationConcept {code: 'destinationOccupancy'})
                SET f.type = 'State',
                    f.name = '목적지 점유 상태',
                    f.keywords = ['점유 상태', '목적지 상태', '도착지 상태']

                MERGE (t:OperationConcept {code: 'DEST_OCCUPIED'})
                SET t.type = 'PlcTag',
                    t.name = 'PLC 목적지 점유 태그',
                    t.keywords = ['DEST_OCCUPIED', '점유 태그', '인터록 태그']

                MERGE (s:OperationConcept {code: 'SOP_DESTINATION_BLOCKED'})
                SET s.type = 'Sop',
                    s.name = '목적지 점유 해소 조치',
                    s.keywords = ['목적지 점유 조치', '이동 차단 조치', '인터록 조치']

                MERGE (c:OperationConcept {code: 'DESTINATION_LOCATION_STATUS'})
                SET c.type = 'Checkpoint',
                    c.name = 'MCS 로케이션 상태',
                    c.keywords = ['로케이션 상태', '도착 로케이션', '목적지 로케이션']

                MERGE (e)-[:INDICATES {label: '이동 차단 원인'}]->(f)
                MERGE (f)-[:MAPPED_TO {label: '점유 상태는 PLC 태그와 연결'}]->(t)
                MERGE (f)-[:HANDLED_BY {label: '점유 상태 해소 절차'}]->(s)
                MERGE (s)-[:CHECKS {label: '먼저 확인할 화면'}]->(c)
                """).run();
    }

    private void seedLocationMismatch() {
        neo4jClient.query("""
                MERGE (e:OperationConcept {code: 'LOCATION_MISMATCH'})
                SET e.type = 'Event',
                    e.name = '다른 위치 감지 이벤트',
                    e.keywords = ['LOCATION_MISMATCH', '오도착', '다른 위치', '위치 불일치', '현재 위치']

                MERGE (f:OperationConcept {code: 'currentLocationCd'})
                SET f.type = 'Field',
                    f.name = '현재 감지 위치',
                    f.keywords = ['currentLocationCd', '현재 위치', '감지 위치']

                MERGE (t:OperationConcept {code: 'CURRENT_LOCATION'})
                SET t.type = 'PlcTag',
                    t.name = 'PLC 현재 위치 태그',
                    t.keywords = ['CURRENT_LOCATION', '현재 위치 태그', '위치 센서']

                MERGE (s:OperationConcept {code: 'SOP_LOCATION_MISMATCH'})
                SET s.type = 'Sop',
                    s.name = '위치 불일치 조치',
                    s.keywords = ['위치 불일치 조치', '오도착 조치', '다른 위치 감지 조치']

                MERGE (c:OperationConcept {code: 'LOCATION_SENSOR'})
                SET c.type = 'Checkpoint',
                    c.name = '위치 센서와 로케이션 매핑',
                    c.keywords = ['위치 센서', '센서 매핑', '로케이션 매핑']

                MERGE (e)-[:INDICATES {label: '감지 위치 확인 필요'}]->(f)
                MERGE (f)-[:MAPPED_TO {label: '현재 위치는 PLC 위치 태그와 연결'}]->(t)
                MERGE (f)-[:HANDLED_BY {label: '위치 불일치 조치 절차'}]->(s)
                MERGE (s)-[:CHECKS {label: '먼저 확인할 위치'}]->(c)
                """).run();
    }
}
