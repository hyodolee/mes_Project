# MCS 개발 진행 현황

MCS는 MES와 같은 `MES_DB`를 사용합니다. MCS 전용 테이블은 `MCS_` 접두어로 구분하고, 공장/창고/품목/거래처 같은 마스터 정보는 MES 마스터 테이블을 참조합니다.

## 현재 상태 요약

| 구분 | 상태 | 비고 |
|---|---|---|
| MCS 프로젝트 기본 구조 | 완료 | Spring Boot, MyBatis, 공통 응답/예외/페이징 구성 |
| MES 마스터 조회 | 완료 | Plant, Warehouse, Item, Vendor, ComCode 조회 |
| Zone/Location | 완료 | CRUD, 화면, API, Mapper/XML |
| 로케이션 재고 | 완료 | 재고 조회, 재고 조정, 재고 이력 |
| 입고 워크플로우 | 완료 | 입고오더/품목, 상태 전이, 재고 반영 |
| 출고 워크플로우 | 완료 | 출고오더/품목, 상태 전이, 재고 차감 |
| 이동 관리 | 완료 | 이동오더/품목, 시작/완료/취소/실패 처리 |
| 경로 관리 | 완료 | 노드/구간/상태 관리 |
| 경로 최적화 | 완료 | 최단 시간, 혼잡 회피, 막힌 구간 제외 |
| PLC 이벤트 | 완료 | PowerShell 시뮬레이터, 이벤트 수신 API |
| MES 자재 요청 연동 | 완료 | MES 작업오더에서 MCS 이동오더 생성 |
| React 화면 | 완료 | MCS 주요 화면 React 구현 |
| AI 운영 브리핑 | 완료 | Spring AI + 규칙 기반 fallback |
| AI 운영 챗봇 | 완료 | Tool Calling, 스트리밍, 대화 메모리 |
| AI 이벤트 알림 | 완료 | PLC 오류 감지, 알림 저장, SSE push |
| Vector RAG | 완료 | Chroma + OpenAI 임베딩, 문서 업로드/재색인 관리, SCL 코드 분석 |

## 완료된 Phase

| Phase | 내용 | 상태 |
|---:|---|---|
| 0 | MCS 프로젝트 부트스트랩 | 완료 |
| 1 | MES 마스터 조회, Zone/Location CRUD | 완료 |
| 2 | 로케이션 재고, 재고 조정, 재고 이력 | 완료 |
| 3 | 입고 워크플로우 | 완료 |
| 4 | 출고 워크플로우 | 완료 |
| 5 | 이동 관리 | 완료 |
| 6 | 경로 관리, 경로 최적화 | 완료 |
| 7 | PLC 이벤트 시뮬레이션 | 완료 |
| 8 | MES 작업오더 자재 요청 연동 | 완료 |
| 9 | AI 실시간 운영 브리핑 | 완료 |
| 10 | AI 자연어 챗봇 | 완료 |
| 11 | AI 이벤트 알림 | 완료 |

## AI 기능 현황

### 운영 브리핑

주요 파일:

- `OperationAiAnalysisService`
- `OperationAiAnalysisApiController`
- `mes_frontend/src/pages/ai/operations.jsx`

현재 방식:

- 사용자가 `분석 시작` 또는 `다시 분석`을 누를 때 AI 분석을 요청합니다.
- 프론트는 마지막 브리핑 결과를 `localStorage`에 저장해서 반복 호출을 막습니다.
- AI 호출 실패 시 규칙 기반 요약을 보여줍니다.

### 운영 챗봇

주요 파일:

- `OperationQueryService`
- `OperationQueryPrompt`
- `OperationTools`
- `ChatDrawer.jsx`
- `chatStore.js`

현재 방식:

- 프론트는 `/api/v1/ai/query/stream`으로 스트리밍 요청을 보냅니다.
- 백엔드는 Spring AI `ChatClient.stream()`으로 답변 토큰을 SSE로 내려줍니다.
- `MessageChatMemoryAdvisor`가 `conversationId` 기준으로 최근 대화를 기억합니다.
- 현재 메모리는 서버 메모리 기반이며 DB 저장은 보류 상태입니다.

### 운영 문서 검색 Tool (Vector RAG)

주요 파일:

- `OperationDocumentSearchService` — Chroma 벡터 검색 전용. 결과 없으면 빈 결과 반환(로컬 폴백 없음)
- `RagDocumentService` — 업로드 파일을 추출·청크·임베딩 적재, 재색인/삭제 관리
- `RagDocumentApiController` — `/api/v1/ai/rag/documents` 업로드·목록·재색인·삭제 API
- `mes_frontend/src/pages/ai/rag-documents.jsx` — RAG 문서 관리 화면

현재 방식:

- `searchOperationDocuments()` Tool이 Chroma Vector DB에서 의미 유사도 기반으로 문서를 검색합니다.
- 임베딩 모델은 OpenAI `text-embedding-3-small`, Vector DB는 Docker Chroma(`mes-mcs-chroma`, 포트 8000)입니다.
- 검색 결과가 없으면 RAG 문서가 없거나 미색인 상태로 보고, 추측/로컬 폴백 없이 빈 결과를 반환합니다.
- 프롬프트는 검색 결과에 실제 등장한 함수명·신호·주소만 인용하고, 없으면 "문서 업로드 후 재색인" 안내만 하도록 제약합니다.
- 답변은 "쉬운 한 줄 요약 → 용어 풀이 포함 원인 → 권장 조치 1개" 형식으로 가독성을 강제합니다. 코드 원문/주소는 사용자가 "자세히/코드 보여줘"라고 요청할 때만 상세히 답합니다.

업로드 지원 형식: `md, txt, pdf, docx, xlsx, scl, st, l5x, awl`
(scl/st/l5x/awl은 PLC 프로그램 소스/익스포트 — 텍스트로 추출. pdf/docx/xlsx는 PDFBox/POI 리플렉션 추출)

데모용 샘플 문서 (`docs/rag/`):

- `plc-mcs-communication-spec.md` (통신 정의서, 이벤트별 필수 필드)
- `plc-tag-mapping.md` (JSON 필드 ↔ PLC 태그/주소 매핑)
- `plc-troubleshooting-sop.md` (장애 조치 SOP)
- `glossary.md` (현장 용어 ↔ 필드/태그 동의어)
- `plc-code/CV001_transfer_control.scl` (실제 Siemens SCL 소스 export — 진단 prose 없이 코드만)

### RAG 단계 시연 (업로드 기반)

별도 전환 기능 없이, RAG 문서 관리 화면에서 문서를 올리고 지우는 것으로 단계를 시연합니다.

| 단계 | 업로드 상태 | 답변 수준 |
|---|---|---|
| 1단계 | 문서 전부 삭제 | "RAG 문서가 없어 확인할 수 없습니다" |
| 2단계 | 스펙 문서 4종만 | 필드명/태그명(`TO_LOCATION_CD`), 점검 방향 |
| 3단계 | 스펙 + `CV001_transfer_control.scl` | AI가 코드의 `IF DEST_SENSOR_OK ... ELSE destLoc := ''` 분기를 직접 읽고 원인 추론 |

핵심: Stage 3 답변의 코드 레벨 진단은 문서나 프롬프트에 미리 적혀 있지 않으며, AI가 업로드된 SCL 코드를 분석해 도출합니다.

접근: 프론트 좌측 메뉴 **AI > RAG 문서 관리** (`/ai/rag-documents`). 업로드 후 자동 색인, 필요 시 "전체 재색인".

설정 (`application.yml`):

- `ai.rag.upload-dir` — 업로드 원본 저장 경로 (기본 `../tmp/rag-uploads`)

주의:

- Chroma 컨테이너는 항상 실행 중이어야 합니다. `ChromaVectorStoreAutoConfiguration`이 시작 시 VectorStore Bean을 생성하며 Chroma에 연결합니다.
- 챗봇 SSE 타임아웃: RAG 검색+툴콜+긴 답변이 30초를 넘겨 끊기던 문제를 `OperationQueryService`의 SSE wall-clock 제한을 180초로 늘려 해결했습니다(토큰 간 무응답 감지 45초는 유지).

### AI 알림

주요 파일:

- `AiNotificationService`
- `SseEmitterService`
- `AiNotificationApiController`
- `Notification.jsx`

현재 방식:

- 60초마다 PLC 이벤트를 확인합니다.
- 알림 대상 이벤트만 선별합니다.
- `sourceRef` 기준으로 중복 알림을 막습니다.
- 알림을 DB에 저장하고 SSE로 프론트에 전달합니다.

## 작업오더 단건 AI 분석 정리

작업오더 화면의 단건 `AI 분석` 버튼과 Drawer는 제거했습니다.

정리 이유:

- 챗봇으로 작업오더 상태를 직접 질문할 수 있습니다.
- 단건 분석 기능은 챗봇과 역할이 겹칩니다.
- AI 진입점이 많아지면 학습과 유지보수가 어려워집니다.

현재 상태:

- 프론트 단건 AI 분석 UI 제거 완료
- 프론트 API 함수 제거 완료
- 백엔드 단건 AI 분석 컨트롤러/서비스/응답 DTO 제거 완료

## DTO 리팩토링

사용자 요청에 따라 MES/MCS public DTO와 API 응답 record를 Lombok class로 전환했습니다.

기준:

- `@Getter`
- `@Setter`
- `@NoArgsConstructor`
- `@AllArgsConstructor`

이유:

- MyBatis 매핑과 setter 주입 방식이 더 단순합니다.
- 초심자가 record보다 class 구조를 따라가기 쉽습니다.

## 다음 작업

| 순서 | 작업 | 내용 |
|---:|---|---|
| 1 | 깨진 문서/화면 문구 정리 | AI 문서, 프롬프트, 챗봇 UI 문구를 UTF-8 기준으로 정리 |
| 2 | 챗봇 답변 가독성 개선 | 줄바꿈, 문장부호 뒤 공백, 현황/원인/조치 형식 개선 |
| 3 | ~~RAG 학습 단계 시작~~ | 완료 — 문서/chunk/embedding/Chroma/검색 API 구현 |
| 4 | ~~Chroma 연결~~ | 완료 — Docker Chroma + Spring AI VectorStore 연결 |
| 5 | ~~RAG 검색 결과 검증~~ | 완료 — 3단계 전환 데모로 답변 품질 차이 검증 |
| 6 | Chroma 볼륨 마운트 정리 | 볼륨이 `/chroma/chroma`(구버전 경로)에 마운트됨 → `/data`로 수정해 영속화 |
| 7 | Graph RAG 검토 | `docs/rag/GRAPH_RAG_DEMO_SCENARIO.md` 기반 차기 단계 |

## 참고 문서

| 문서 | 용도 |
|---|---|
| `mcs/MCS_설계문서_v2.md` | MCS DDL, ERD, REST API, MES 연동 로직 |
| `mcs/MCS_더미데이터_업무흐름.md` | 더미 데이터 기준 업무 흐름 |
| `docs/ai/AI_CODE_STRUCTURE.md` | AI 코드 구조 안내 |
| `docs/design/AI_OPERATION_PLAN.md` | AI 운영 기능 계획 |
| `docs/runbooks/DEMO_SCENARIOS.md` | 시연 시나리오 |
