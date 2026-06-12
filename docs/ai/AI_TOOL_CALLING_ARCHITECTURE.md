# Spring AI Tool Calling 도입 배경 및 구조

> 포트폴리오 / 발표 자료용 기술 정리  
> 작성일: 2026-06-10

---

## 1. 문제 정의

### 1-1. 챗봇이 "동문서답"을 하는 이유

MES/MCS 운영 AI 챗봇을 구현하면서 다음과 같은 문제가 반복적으로 발생했다.

| 상황 | 기대 동작 | 실제 동작 |
|---|---|---|
| 이동 관리 화면에서 "실패한 이동 왜 멈췄어?" | 자재 이동 + PLC 이벤트 조회 후 답변 | 이동 관리 화면이므로 이동 데이터만 조회 → 실패 원인 모름 |
| 작업 오더 화면에서 "실패한 자재 이동 있어?" | 자재 이동 데이터 조회 후 답변 | 작업 오더 화면이므로 작업지시 데이터만 조회 → 엉뚱한 답변 |
| "실패 1건은 무슨 이유야?" (후속 질문) | 앞 대화 맥락 이어서 PLC 이벤트 조회 | "실패", "이유" 키워드가 분류 기준에 없어 GENERAL로 오분류 |

### 1-2. 근본 원인: 규칙 기반 분류(Intent Classification)의 한계

초기 구현은 **백엔드가 질문을 먼저 분류**하고, 분류 결과에 따라 데이터를 조회하는 방식이었다.

```
[기존 흐름]
질문 → classifyQueryType()
        1순위: 현재 페이지 경로 (/mcs/transfers → TRANSFER)
        2순위: 직전 대화 맥락 키워드 매칭
        3순위: 질문 키워드 매칭 ("이동" → TRANSFER)
      → 해당 분류의 데이터만 조회
      → AI에게 "이 데이터 보고 답해"
      → 최종 답변
```

이 방식의 문제:
- **페이지 종속**: 어떤 화면에 있느냐에 따라 같은 질문의 답이 달라짐
- **키워드 한계**: "무슨 이유야?", "왜 그래?", "처음에 말한 그거" 등 변칙 표현은 오분류
- **단일 데이터 조회**: 복합 질문("이동 실패 원인이랑 관련 작업지시도 알려줘")을 처리 불가
- **두더지잡기**: 오분류 케이스가 발생할 때마다 키워드를 추가해야 하는 무한 패치 반복

---

## 2. LLM의 근본적 한계와 해결 아이디어

### 2-1. LLM은 코드를 실행할 수 없다

GPT, Claude 같은 LLM은 텍스트 생성 모델이다. DB 조회, HTTP 호출, 계산 등을 **직접 실행할 수 없다.**

```
LLM이 할 수 있는 것:  텍스트 읽기, 텍스트 생성
LLM이 못 하는 것:    DB 조회, API 호출, 파일 읽기, 실시간 데이터 접근
```

따라서 기존 방식은 "백엔드가 데이터를 가져와서 LLM에게 넘기는" 구조가 필수였고,
이 과정에서 **"어떤 데이터를 가져올지 선택하는 로직"** 이 복잡해지는 문제가 생겼다.

### 2-2. 아이디어: 선택을 AI에게 맡기면 어떨까?

데이터 선택(분류)을 백엔드 코드가 하는 게 아니라, **AI 자신이 어떤 데이터가 필요한지 판단하게** 하면 어떨까?

→ 이것이 **Tool Calling(함수 호출)** 이다.

---

## 3. Tool Calling 동작 원리

### 3-1. OpenAI API 레벨에서 일어나는 일

Tool Calling은 OpenAI의 공식 프로토콜이다. 일반 API 호출과 비교하면:

```
[일반 호출]
요청 → { messages, model }
응답 ← { content: "답변 텍스트" }

[Tool Calling 호출]
요청 → { messages, model, tools: [도구 명세 목록] }
응답 ← { finish_reason: "tool_calls",    ← 답변 대신
          tool_calls: [{ name: "getTransfers", arguments: {} }] }
          → 우리 서버에서 해당 함수 실행
          → 결과를 다시 OpenAI에 전송
응답 ← { finish_reason: "stop", content: "최종 답변" }
```

AI가 직접 텍스트 답변을 반환하는 대신,
**"이 함수를 실행해서 결과를 나에게 다시 줘"** 라고 요청하는 방식이다.
실제 함수 실행은 항상 우리 서버(Java)에서 이루어진다.

### 3-2. 연쇄 호출 (Multi-step Reasoning)

AI는 도구를 여러 번 연속으로 호출할 수 있다:

```
질문: "실패한 이동의 원인과 관련 작업지시를 알려줘"

1단계: AI → getTransfers() 호출 요청
       서버 실행 → [이동 목록, 실패 건: transferId=54] 반환
2단계: AI → getTransferEvents(transferId=54) 호출 요청
       서버 실행 → [CV-001 모터 과부하 오류] 반환
3단계: AI → getWorkOrders() 호출 요청
       서버 실행 → [WO-2026-0012 관련 작업지시] 반환
4단계: AI → 세 결과를 종합해 최종 답변 생성

최종 답변:
"CV-001 장비에서 모터 과부하 오류로 이동이 멈췄어요.
 이 자재는 WO-2026-0012 작업에 사용될 예정이라
 생산 일정에 영향이 있을 수 있어요.
 MCS의 이동 관리 화면에서 해당 건을 확인해보세요."
```

어떤 도구를 어떤 순서로 몇 번 호출할지 **AI가 스스로 결정**한다.

---

## 4. Spring AI에서의 구현

### 4-1. @Tool 어노테이션

Spring AI는 Java 메서드에 `@Tool` 어노테이션을 붙이는 것만으로
OpenAI에 전달할 함수 명세 JSON을 자동 생성해준다.

```java
@Component
public class OperationTools {

    @Tool(description = "자재 이동(반송) 목록을 조회한다. 이동 상태(이동중/완료/실패), 출발지, 도착지 정보 포함")
    public List<TransferInfo> getTransfers() {
        return mcsTransferClient.getAllTransfers(50);
    }

    @Tool(description = "특정 자재 이동의 PLC 설비 이벤트(오류 코드, 실패 원인)를 조회한다")
    public List<PlcEventInfo> getTransferEvents(
            @ToolParam(description = "조회할 이동의 ID") Long transferId) {
        return mcsTransferClient.getPlcEventsByTransfer(transferId, 10);
    }

    @Tool(description = "작업지시 목록을 조회한다. 대기/진행/완료 상태, 품목, 수량 포함")
    public List<WorkOrderInfo> getWorkOrders() {
        return workOrderService.getWorkOrders(null, null, null, null, null, null);
    }
}
```

**description이 핵심이다.** AI는 이 설명만 보고 어떤 도구를 호출할지 판단하므로,
구체적이고 명확하게 작성할수록 정확도가 높아진다.

### 4-2. ChatClient에 도구 등록

```java
String answer = chatClientBuilder.build()
        .prompt()
        .system(systemPrompt)
        .user(question)
        .tools(operationTools)   // 도구 등록 한 줄
        .call()
        .content();
```

tool_call 응답 수신 → Java 메서드 실행 → 결과 재전송의 루프를
Spring AI가 내부적으로 자동 처리한다.

### 4-3. 코드 변화: 삭제되는 것 vs 추가되는 것

```
[삭제]
- classifyQueryType() 전체 (약 30줄) — 페이지 경로/키워드 분류 로직
- collectContext()의 switch문 (약 80줄) — 분류별 데이터 조회 분기
- pageContext의 분류 용도 — 더 이상 데이터 선택에 사용 안 함

[추가]
- OperationTools.java (신규) — @Tool 메서드 4~5개
- ChatClient에 .tools() 한 줄
```

약 110줄의 복잡한 분류/조회 코드가 사라지고, 선언적인 도구 정의로 대체된다.

---

## 5. 기존 방식 vs Tool Calling 비교

| 항목 | 기존 (규칙 기반 분류) | Tool Calling |
|---|---|---|
| 데이터 선택 주체 | 백엔드 개발자가 작성한 if/switch | AI가 스스로 판단 |
| 페이지 종속성 | 있음 (현재 경로가 1순위) | 없음 (질문 내용만 봄) |
| 복합 질문 처리 | 불가 (단일 분류만 가능) | 가능 (도구 연쇄 호출) |
| 오분류 대응 | 키워드 추가 패치 반복 | description 수정으로 해결 |
| 기능 확장 | 새 분류 케이스 코드 추가 필요 | @Tool 메서드 하나 추가 |
| API 호출 횟수 | 1회 | 2~4회 (왕복) |
| 응답 속도 | 빠름 (~3초) | 다소 느림 (~10초) |

---

## 6. 설계 원칙: 읽기 전용 도구만 등록

Tool Calling에서 **절대 지켜야 할 원칙**이 있다.

> **조회(READ) 도구만 등록한다. 수정/삭제/생성 도구는 등록하지 않는다.**

AI는 도구를 자율적으로 호출하기 때문에,
만약 `cancelTransfer()`, `deleteWorkOrder()` 같은 도구를 등록하면
AI가 의도치 않게 데이터를 변경하거나 삭제할 수 있다.

현재 프로젝트에서 등록된 도구는 모두 조회 전용이다 (MES/MCS 전 영역):

| 영역 | 도구 |
|---|---|
| 자재 이동 | `getTransfers()`, `getTransferEvents(transferId)` |
| 설비 신호 | `getRecentPlcEvents()` |
| 작업/생산 | `getWorkOrders()`, `getProdPlans()` |
| 기준정보 | `getPlants()`, `getItems()` |
| 재고 | `getStocks()` |
| 설비 | `getEquipmentStatus()`, `getEquipmentDowntimes()` |
| 품질 | `getInspectResults()`, `getDefects()` |

답변 범위를 넓힐 때는 새 분류 코드를 짜는 게 아니라 `@Tool` 메서드를 하나 추가하면 된다.
실제로 초기 3개 영역(이동/PLC/작업지시)에서 전 영역으로 확장할 때, 분류 로직 수정 없이
도구 메서드만 추가하는 것으로 끝났다.

---

## 7. 다른 AI 프레임워크와의 비교

같은 개념이 다른 이름으로 존재한다. 모두 동일한 OpenAI function_calling 프로토콜 위에서 동작한다.

| 프레임워크 | 언어 | 용어 |
|---|---|---|
| **Spring AI** | Java | `@Tool`, `@ToolParam` |
| LangChain | Python | `Tool`, `Agent`, `ReAct` |
| LlamaIndex | Python | `FunctionTool`, `QueryEngineTool` |
| OpenAI SDK | 직접 | `function_calling`, `tools` |
| Semantic Kernel | C# / Python | `KernelFunction`, `[KernelFunction]` |

---

## 8. 확장 로드맵: Spring AI 생태계

Tool Calling은 더 큰 아키텍처의 첫 단계다.

```
ChatClient
 ├─ .tools(operationTools)
 │   └─ @Tool 메서드들 ← 현재 구현 단계
 │       (DB/API 조회를 AI가 자율 선택)
 │
 ├─ .advisors(...)
 │   ├─ MessageChatMemoryAdvisor  ← 구현: 서버 측 대화 이력 관리
 │   │   (프론트 6턴 수동 전달은 fallback으로 유지)
 │   │
 │   ├─ searchOperationDocuments  ← 구현: 로컬 운영 문서 검색 Tool
 │   └─ QuestionAnswerAdvisor     ← 이후: Chroma RAG 학습 후 구조 비교
 │       (설비 매뉴얼, 통신 정의서를 Vector DB에 저장
 │        "CV-001 E02 에러코드가 뭐야?" 같은 질문에 문서 기반 답변)
 │
 └─ .build()
```

세 가지 모두 `ChatClient` 빌더에 끼워 넣는 방식으로 구성되어
단계별로 추가가 가능하다.

---

## 9. 핵심 요약 (발표 1슬라이드 분량)

**문제**: LLM 챗봇이 동문서답을 반복 — 규칙 기반 분류가 복잡한 자연어 질문을 감당하지 못함

**원인**: 백엔드가 질문을 미리 분류해서 데이터를 조회하는 구조 → 분류 오류 = 잘못된 데이터 = 엉뚱한 답변

**해결**: Spring AI Tool Calling 도입
- `@Tool` 어노테이션으로 조회 함수들을 AI에게 도구로 노출
- AI가 질문을 이해하고 필요한 도구를 스스로 선택·연쇄 호출
- 분류 코드 110줄 삭제, 페이지 종속성 제거, 복합 질문 처리 가능

**효과**: "어떤 화면에 있든, 어떤 표현을 쓰든 질문 의도에 맞는 데이터를 AI가 스스로 찾아서 답변"
