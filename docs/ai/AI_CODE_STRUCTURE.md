# AI 코드 구조 문서

이 문서는 MES/MCS AI 기능 코드를 처음 보는 사람이 어디부터 읽으면 되는지 정리한 안내서입니다.

## 1. 패키지 구조

AI 관련 서버 코드는 `com.mes.application.service.ai` 아래에 역할별로 나뉩니다.

```text
application/service/ai/
├─ support/       공통 유틸과 Spring AI 연결
├─ query/         운영 챗봇과 Tool Calling
├─ analysis/      운영 브리핑
└─ notification/  PLC 이벤트 기반 알림
```

컨트롤러는 `interfaces/api/ai`, DTO는 `domain/ai/dto`, 알림 Mapper는 `infra/persistence/mybatis/mapper/ai`에 있습니다.

## 2. Spring AI 연결

먼저 볼 파일:

```text
application/service/ai/support/AiClientGateway.java
```

역할:

- `OPENAI_API_KEY`가 있는지 확인합니다.
- Spring AI `ChatClient.Builder`를 제공합니다.
- 현재 사용하는 모델명을 제공합니다.
- API Key가 없으면 AI 호출 대신 fallback 흐름을 탈 수 있게 합니다.

## 3. 대화 메모리 설정

먼저 볼 파일:

```text
global/config/AiMemoryConfig.java
```

역할:

- `ChatMemory`를 생성합니다.
- `MessageChatMemoryAdvisor`를 생성합니다.
- 현재는 `conversationId`별로 최근 12개 메시지만 기억합니다.

현재 메모리는 서버 메모리 기반입니다. 서버를 재시작하면 대화 기억은 사라집니다.

## 4. 운영 챗봇 흐름

먼저 볼 파일:

```text
interfaces/api/ai/OperationQueryApiController.java
application/service/ai/query/OperationQueryService.java
```

요청 흐름:

```text
프론트 질문
-> OperationQueryApiController
-> OperationQueryService
-> OperationToolsFactory
-> OperationTools
-> ChatClient call/stream
```

중요 메서드:

- `query()`: 일반 응답 방식입니다.
- `streamQuery()`: SSE 스트리밍 응답 방식입니다.
- `buildRequestSpec()`: system prompt, user prompt, tools, memory advisor를 한 번에 붙입니다.
- `clearMemory()`: 현재 conversationId의 서버 메모리를 지웁니다.

## 5. 챗봇 프롬프트

먼저 볼 파일:

```text
application/service/ai/query/OperationQueryPrompt.java
```

역할:

- 챗봇의 말투를 정합니다.
- 어떤 질문에 어떤 Tool을 써야 하는지 알려줍니다.
- MES/MCS 화면 안내 규칙을 정합니다.
- 답변 범위를 공장 운영 데이터로 제한합니다.

## 6. Tool Calling

먼저 볼 파일:

```text
application/service/ai/query/OperationToolsFactory.java
application/service/ai/query/OperationTools.java
```

`OperationToolsFactory`는 MES/MCS 서비스 의존성을 모아 `OperationTools`를 만듭니다.

`OperationTools`는 AI가 호출할 수 있는 조회 도구입니다.

현재 Tool:

- `getTransfers()`
- `getTransferEvents()`
- `getRecentPlcEvents()`
- `getWorkOrders()`
- `getProdPlans()`
- `getPlants()`
- `getItems()`
- `getStocks()`
- `getEquipmentStatus()`
- `getEquipmentDowntimes()`
- `getInspectResults()`
- `getDefects()`
- `searchOperationDocuments()`

Tool은 조회 전용입니다. 등록, 수정, 삭제 기능은 AI Tool로 열지 않습니다.

## 7. 운영 브리핑

먼저 볼 파일:

```text
application/service/ai/analysis/OperationAiAnalysisService.java
interfaces/api/ai/OperationAiAnalysisApiController.java
```

역할:

- 생산 지시, MCS 이동, PLC 이벤트, 재고, 불량, 검사, 설비 현황을 모아 운영 스냅샷을 만듭니다.
- AI가 운영 브리핑 JSON을 만들도록 요청합니다.
- AI 호출이 실패하면 규칙 기반 요약으로 대체합니다.

프론트에서는 `AI 운영 분석` 화면의 `AI 실시간 운영 브리핑` 카드에서 사용합니다.

## 8. 작업오더 단건 AI 분석

작업오더 화면에 있던 단건 `AI 분석` 버튼과 Drawer는 제거했습니다.

이유:

- 운영 챗봇에서 작업오더 상태를 질문할 수 있습니다.
- 단건 분석 기능과 챗봇 역할이 겹쳤습니다.
- 학습과 유지보수를 위해 AI 진입점을 줄이는 편이 낫습니다.

현재 상태:

- 프론트 작업오더 단건 AI 분석 UI 제거 완료
- 백엔드 작업오더 단건 AI 분석 API/서비스/응답 DTO 제거 완료
- 관련 fine-tuning 예제 문서는 아직 남아 있으며, 나중에 fine-tuning 방향을 다시 정할 때 정리합니다.

## 9. 알림

먼저 볼 파일:

```text
application/service/ai/notification/AiNotificationService.java
application/service/ai/notification/SseEmitterService.java
interfaces/api/ai/AiNotificationApiController.java
```

흐름:

```text
60초마다 PLC 이벤트 확인
-> 알림 대상 이벤트 선별
-> 중복 알림 여부 확인
-> 알림 문구 생성
-> DB 저장
-> SSE로 프론트에 새 알림 전달
```

프론트는 `EventSource`로 `/api/v1/notifications/subscribe`를 구독합니다.

## 10. 현재 유지할 AI 기능

유지:

- 운영 챗봇
- AI 실시간 운영 브리핑
- PLC 이벤트 기반 AI 알림
- 운영 문서 검색 Tool 1차 구현

보류:

- 대화 내역 DB 저장
- 탭 간 대화 동기화
- Chroma 기반 RAG

다음 순서:

1. 깨진 문서와 화면 문구 정리
2. 챗봇 답변 가독성 개선
3. RAG 학습 단계 진입
