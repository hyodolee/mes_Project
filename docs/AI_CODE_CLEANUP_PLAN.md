# AI 코드 정리 계획

## 목적

현재 AI 관련 코드는 기능이 계속 추가되면서 한 파일이 너무 많은 일을 맡고 있다.
이번 정리의 목적은 코드를 짧게 줄이는 것이 아니라, 처음 보는 사람이 다음 흐름을 쉽게 파악하게 만드는 것이다.

```text
Controller -> Query Service -> ChatClient -> Advisor -> Tools -> MES/MCS/RAG 조회
```

참고한 예제 프로젝트는 `C:\edu\SpringAi\work\step07_tool-calling`이다.
해당 프로젝트의 장점은 다음 구조가 명확하다는 점이다.

```text
config
- ChatClient 기본 설정

service
- 대화 요청 흐름

tools
- LLM이 호출할 기능별 도구
```

MES 프로젝트도 이 흐름을 참고하되, 프로젝트 규모가 더 크므로 MES/MCS/RAG 기준으로 나누어 정리한다.

## 정리 원칙

1. 어려운 문법으로 억지로 줄이지 않는다.
2. 한 파일은 하나의 큰 책임만 가진다.
3. Tool Calling 코드는 기능별로 나눈다.
4. 프롬프트는 가능하면 Java 코드 밖으로 뺀다.
5. 주석은 짧고 쉬운 한국어로 쓴다.
6. 기능 변경 없이 구조 정리를 먼저 한다.
7. 버전 업그레이드는 이번 정리 범위에서 제외한다.

## 현재 문제

### 1. 한 파일이 너무 많은 역할을 가짐

대표 파일:

```text
OperationTools.java
- MCS 이동
- PLC 이벤트
- 경로/로케이션
- MES 작업지시/생산계획
- 재고/설비/품질/불량
- Graph RAG
- 문서 RAG
```

```text
OperationQueryService.java
- 일반 AI 호출
- SSE 스트리밍
- 이미지 저장
- Advisor 연결
- Tool 생성
- AI 실패 시 fallback 답변
```

```text
OperationAiAnalysisService.java
- 운영 데이터 수집
- AI 분석 호출
- AI 응답 파싱
- AI 실패 시 규칙 기반 분석
- 분석 프롬프트
```

### 2. 주석과 메시지가 일부 깨져 있음

과거 인코딩 문제로 깨진 한글이 남아 있다.
깨진 주석은 코드 이해를 방해하고, 깨진 에러 메시지는 화면에 그대로 노출될 수 있다.

우선 확인 대상:

```text
OperationQueryService.java
RagDocumentService.java
OperationGraphSeeder.java
OperationTools.java
OperationToolsFactory.java
```

### 3. Tool 클래스의 분류가 애매함

현재는 `OperationTools` 하나에 모든 Tool이 들어 있다.
그래서 특정 질문이 어떤 도구를 호출하는지 코드에서 한눈에 보기 어렵다.

## 목표 구조

### 패키지 구조

```text
com.mes.application.service.ai
  config
    AiMemoryConfig.java

  advisor
    SensitiveDataMaskingAdvisor.java

  query
    OperationQueryService.java
    OperationQueryFallbackService.java
    ConversationImageStore.java

  query.tools
    OperationToolFactory.java
    OperationToolSet.java
    McsAiTools.java
    MesAiTools.java
    OperationStatusAiTools.java
    RagAiTools.java

  rag
    RagDocumentService.java
    OperationDocumentSearchService.java
    OperationGraphSearchService.java
    OperationGraphSeeder.java

  analysis
    OperationAiAnalysisService.java
    OperationEvidenceCollector.java
    OperationAnalysisFallbackService.java
    OperationAnalysisPrompt.java

  support
    AiClientGateway.java
    AiJsonSupport.java
    AiTextSupport.java
    SensitiveDataSanitizer.java
```

패키지 이동은 한 번에 전부 하지 않고, 리스크가 낮은 순서대로 진행한다.

## Tool 정리 방향

현재 `OperationTools`를 다음처럼 나눈다.

### McsAiTools

MCS와 PLC 관련 조회만 담당한다.

```text
getTransfers
getTransferEvents
getRecentPlcEvents
getRouteEdges
getBlockedRoutes
getTransferRoute
getTransferRouteSteps
getLocations
getLocationStocks
getPlcValidationFailures
analyzeTransferBlockers
```

### MesAiTools

MES 기준정보와 생산계획 조회를 담당한다.

```text
getWorkOrders
getProdPlans
getPlants
getItems
```

### OperationStatusAiTools

운영 현황성 데이터를 담당한다.

```text
getStocks
getEquipmentStatus
getEquipmentDowntimes
getInspectResults
getDefects
```

### RagAiTools

RAG 조회만 담당한다.

```text
searchOperationGraph
searchOperationDocuments
```

### OperationToolSet

질문 1건마다 생성된 Tool 객체 묶음이다.
각 Tool은 `dataPoints`를 공유해서 화면의 근거 데이터 표시를 유지한다.

```java
public record OperationToolSet(
        McsAiTools mcs,
        MesAiTools mes,
        OperationStatusAiTools status,
        RagAiTools rag
) {
    public Object[] asArray() {
        return new Object[] { mcs, mes, status, rag };
    }
}
```

복잡한 추상화는 쓰지 않는다.
단순히 기능별 클래스로 나누고, `ChatClient`에 여러 Tool 객체를 넘기는 방식으로 정리한다.

## OperationQueryService 정리 방향

`OperationQueryService`는 챗봇 요청 흐름만 담당하게 만든다.

남길 책임:

```text
1. 요청을 받는다.
2. ChatClient 요청을 만든다.
3. Advisor를 붙인다.
4. Tools를 붙인다.
5. 일반 호출 또는 SSE 호출을 실행한다.
```

분리할 책임:

```text
OperationQueryFallbackService
- AI 호출 실패 시 기본 조회 답변 생성

ConversationImageStore
- 이미지 보관 유지

OperationToolFactory
- Tool 객체 묶음 생성
```

## Advisor 정리 방향

현재 민감정보 마스킹은 `SensitiveDataMaskingAdvisor`로 들어갔다.
이 파일은 `support`보다 `advisor` 패키지로 이동하는 것이 더 자연스럽다.

목표:

```text
advisor/SensitiveDataMaskingAdvisor.java
support/SensitiveDataSanitizer.java
```

역할 설명:

```text
SensitiveDataSanitizer
- 문자열 마스킹 규칙 보관

SensitiveDataMaskingAdvisor
- AI 호출 직전 ChatClientRequest 메시지에 마스킹 적용
```

## RAG 정리 방향

RAG 관련 클래스는 `query` 아래에 섞여 있으므로 `rag` 패키지로 모은다.

대상:

```text
RagDocumentService
OperationDocumentSearchService
OperationGraphSearchService
OperationGraphSeeder
```

단, `RagDocumentService`는 한 번에 쪼개지 않는다.
먼저 패키지 이동과 깨진 메시지 복구만 한다.
이후 필요하면 다음처럼 나눈다.

```text
RagTextExtractor
- pdf/docx/xlsx/txt 텍스트 추출

RagChunker
- 텍스트 청크 생성

RagDocumentService
- 업로드, 재색인, 삭제 흐름 조립
```

## OperationAiAnalysisService 정리 방향

현재 분석 서비스는 너무 많은 일을 한다.
다음 3단계로 분리한다.

```text
OperationEvidenceCollector
- 작업지시, 이동, PLC, 품질, 재고, 설비 데이터를 수집

OperationAiAnalysisService
- 전체 분석 흐름 조립
- 캐시 처리
- AI 호출

OperationAnalysisFallbackService
- AI 실패 시 규칙 기반 분석 생성
```

프롬프트는 다음 중 하나로 이동한다.

```text
resources/prompts/operation-analysis-system.st
또는
OperationAnalysisPrompt.java
```

처음에는 Java 파일로 분리하고, 안정화 후 `.st` 파일로 옮기는 방식을 추천한다.

## 프롬프트 정리 방향

예제 프로젝트는 `resources/prompts/system.st`를 사용한다.
MES도 이 방식을 적용하면 발표 때 설명하기 쉽다.

후보:

```text
src/main/resources/prompts/operation-query-system.st
src/main/resources/prompts/operation-analysis-system.st
src/main/resources/prompts/ai-notification-alert.st
```

다만 프롬프트 이동은 문자열 깨짐 복구 이후에 진행한다.

## 단계별 작업 계획

### 1단계: 깨진 한글 복구

목표:

```text
기능 변경 없음
주석, 로그, 에러 메시지, fallback 문구만 정상화
```

대상:

```text
OperationQueryService.java
RagDocumentService.java
OperationGraphSeeder.java
OperationTools.java
OperationToolsFactory.java
```

검증:

```text
gradlew.bat build -x test
챗봇 기본 질문 1회
RAG 문서 목록 조회 1회
Graph RAG 질문 1회
```

### 2단계: OperationTools 분리

목표:

```text
큰 Tool 파일을 기능별 Tool 클래스로 나눈다.
Tool 메서드 이름과 반환 DTO는 최대한 유지한다.
```

새 파일:

```text
McsAiTools.java
MesAiTools.java
OperationStatusAiTools.java
RagAiTools.java
OperationToolSet.java
OperationToolFactory.java
```

검증:

```text
기존 챗봇 질문이 같은 도구를 호출하는지 확인
dataPoints가 그대로 화면에 표시되는지 확인
```

### 3단계: OperationQueryFallbackService 분리

목표:

```text
OperationQueryService에서 fallback 요약 생성 로직을 제거한다.
OperationQueryService는 AI 호출 흐름만 읽히게 만든다.
```

검증:

```text
OpenAI 설정이 없거나 호출 실패 상황에서 기본 답변이 유지되는지 확인
```

### 4단계: RAG 패키지 정리

목표:

```text
RAG 관련 파일을 query 패키지에서 rag 패키지로 이동한다.
```

검증:

```text
문서 업로드
재색인
문서 검색
Graph RAG 검색
```

### 5단계: OperationAiAnalysisService 분리

목표:

```text
데이터 수집, AI 호출, fallback 분석을 분리한다.
```

검증:

```text
운영 분석 API 정상 호출
AI 실패 시 규칙 기반 분석 정상 반환
```

### 6단계: 프롬프트 파일 이동

목표:

```text
긴 system prompt를 resources/prompts로 이동한다.
```

검증:

```text
프롬프트 파일 누락 시 앱 시작 오류가 명확한지 확인
AI 응답 형식이 기존과 동일한지 확인
```

## 이번 정리에서 하지 않을 일

```text
Spring Boot 3.5.x 업그레이드
Spring AI 1.1.x 업그레이드
DB 구조 변경
API 응답 DTO 대규모 변경
프론트 화면 구조 변경
추상 클래스나 복잡한 제네릭 기반 리팩토링
```

## 완료 기준

다음 조건을 만족하면 1차 코드 정리가 끝난 것으로 본다.

```text
1. 깨진 한글 주석과 메시지가 사라진다.
2. OperationTools가 기능별 Tool 클래스로 나뉜다.
3. OperationQueryService가 250줄 이하 수준으로 줄어든다.
4. AI 질의, SSE 질의, RAG 검색, Graph RAG 검색이 기존처럼 동작한다.
5. 새로 보는 사람이 패키지 이름만 보고 역할을 예상할 수 있다.
```

## 권장 진행 방식

한 번에 전체를 바꾸지 않는다.
각 단계마다 빌드하고, 화면에서 최소 동작을 확인한다.

진행 순서:

```text
1. 한글 복구
2. Tool 분리
3. Query fallback 분리
4. RAG 패키지 이동
5. 분석 서비스 분리
6. 프롬프트 리소스화
```

가장 먼저 진행할 작업은 `1단계: 깨진 한글 복구`이다.

## 진행 현황

### 2026-06-24

완료:

```text
1단계 점검
- rg 기준으로 실제 저장된 AI 관련 주요 파일에는 깨진 한글 패턴이 남아 있지 않음을 확인했다.
- PowerShell Get-Content 출력에서 깨져 보이는 현상은 터미널 출력 인코딩 영향으로 판단했다.

2단계 완료
- 기존 OperationTools.java 단일 대형 파일을 제거했다.
- Tool 기능을 다음 파일로 분리했다.
  - McsAiTools.java
  - MesAiTools.java
  - OperationStatusAiTools.java
  - RagAiTools.java
- Tool 반환용 View 클래스는 OperationToolViews.java로 모았다.
- Tool 공통 문자열/숫자 변환은 OperationToolSupport.java로 모았다.
- OperationToolSet.java를 추가해 질문 1건에서 사용할 Tool 묶음을 명확히 했다.
- OperationToolsFactory가 OperationToolSet을 생성하도록 변경했다.
- OperationQueryService가 tools.asArray()를 ChatClient에 전달하도록 변경했다.

3단계 완료
- OperationQueryService의 fallback 요약 생성 로직을 OperationQueryFallbackService로 분리했다.
- OperationQueryService는 AI 요청 생성, Advisor 연결, Tool 연결, 일반/SSE 호출 흐름 중심으로 정리했다.
- WorkOrderService, McsTransferClient 직접 주입은 제거하고 OperationToolsFactory를 통해서만 조회 도구를 만들도록 정리했다.

4단계 완료
- RAG 관련 클래스를 query 패키지에서 rag 패키지로 이동했다.
  - OperationDocumentSearchService.java
  - OperationGraphSearchService.java
  - OperationGraphSeeder.java
  - RagDocumentService.java
- RagAiTools, OperationToolsFactory, RagDocumentApiController의 import를 새 패키지 기준으로 수정했다.
- OperationDocumentSearchServiceTest도 rag 패키지로 이동했다.

5단계 완료
- OperationAiAnalysisService의 데이터 수집 로직을 OperationEvidenceCollector로 분리했다.
- AI 실패 시 규칙 기반 분석 로직을 OperationAnalysisFallbackService로 분리했다.
- OperationAiAnalysisService는 캐시, AI 호출, 응답 파싱 흐름 중심으로 정리했다.

6단계 일부 진행
- 전체 운영 분석 프롬프트를 OperationAnalysisPrompt.java로 분리했다.
- 리소스 파일(.st) 이동은 이후 별도 작업으로 남긴다.
- OperationQueryService의 이미지 data URL 해석 책임을 ConversationImageStore로 옮겨 서비스 길이를 248줄로 줄였다.

추가 정리
- OperationQueryPrompt.java의 긴 system prompt를 역할별 섹션 상수로 나누었다.
  - ROLE
  - ANSWER_FORMAT
  - TOOL_RULES
  - EXPRESSION_RULES
  - SOURCE_RULES
  - SCREEN_GUIDE
  - PLC_RULES
  - IMAGE_RULES
  - OUT_OF_SCOPE_RULES
- SensitiveDataMaskingAdvisor를 support 패키지에서 advisor 패키지로 이동했다.
- SensitiveDataSanitizer는 문자열 마스킹 규칙을 담당하므로 support 패키지에 유지했다.
- Spring AI의 MessageChatMemoryAdvisor Bean을 만드는 AiMemoryConfig를 global.config에서 ai.config 패키지로 이동했다.
- 직접 구현한 Advisor는 ai.advisor, Spring AI Advisor Bean 설정은 ai.config에 두는 구조로 정리했다.
- AiMemoryConfig, SensitiveDataMaskingAdvisor, OperationEvidenceCollector, OperationAnalysisFallbackService,
  OperationToolSet, RagAiTools에 역할과 호출 흐름을 설명하는 주석을 보강했다.
- 알림 흐름 관련 파일에도 주석을 보강했다.
  - AiNotificationService.java
  - SseEmitterService.java
  - PlcEventNotificationListener.java
  - PlcEventProcessedEvent.java
  - PlcEventService.java
```

검증:

```text
mes_backserver 기준 gradlew.bat build -x test 성공
OperationDocumentSearchServiceTest 단위 테스트 성공
```

다음 작업:

```text
1. 챗봇, RAG 문서 검색, Graph RAG, 운영 분석 화면에서 최소 동작 확인
2. 필요하면 OperationQueryPrompt, OperationAnalysisPrompt를 resources/prompts/*.st로 이동
3. AI 관련 클래스 주석을 발표 설명용 흐름에 맞춰 추가 보강
```
