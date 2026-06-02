# AI Operation Plan

AI is an assistant layer for MES/MCS operations. It must not directly change production, transfer, stock, or PLC state in the first implementation.

The backend remains responsible for business rules and state changes. AI receives curated operational context, explains the situation, and suggests the next action.

## Scope

| Feature | Purpose | First target |
|---|---|---|
| AI incident analysis | Explain why a work order or transfer is blocked | MES work order + MCS transfer + PLC event failure |
| AI operation query | Answer natural-language questions from operators | "Why can this work order not start?" |
| AI operation summary | Summarize current MES/MCS status on dashboard screens | Waiting work orders, failed transfers, recent PLC errors |
| AI alert message generation | Generate human-readable notification text | PLC error/interlock message for operator |

## Recommended Stack

Use Spring AI first.

Reasons:

- Current backend is Spring Boot.
- MES/MCS data and business rules already live in Java services.
- Tool calling can wrap existing service methods.
- It keeps the portfolio architecture simpler than adding a separate Python AI server.

LangChain or LangGraph can be added later only if the project needs complex multi-step autonomous agents.

## First Implementation

Start with AI incident analysis.

Input context:

- Work order summary
- Material transfer status
- MCS route summary
- Recent PLC events for the transfer
- Current blocking reason from backend rules

Output:

- Problem summary
- Likely cause
- Operational impact
- Recommended recovery step

Example question:

```text
WO202606010009 왜 시작이 안 돼?
```

Expected answer:

```text
WO202606010009는 연결된 MCS 이동오더가 FAILED 상태라서 시작할 수 없습니다.
최근 PLC 이벤트에서 EQUIPMENT_ERROR가 발생했고, MCS가 해당 이동오더를 실패 처리했습니다.
복구하려면 MCS에서 실패 이동오더를 취소한 뒤 MES에서 자재 요청을 다시 생성하세요.
```

## Backend Design

```text
React AI panel
  -> MES AI API
    -> AiIncidentAnalysisService
      -> WorkOrderService
      -> McsTransferClient
      -> PLC event lookup
      -> Spring AI ChatClient
```

## Tool Candidates

| Tool | Responsibility |
|---|---|
| `getWorkOrderContext(woNo)` | Work order, item, lot, status, current MES block reason |
| `getMcsTransferContext(woId)` | Linked MCS transfers, latest active transfer, route |
| `getPlcEventsByTransfer(transferId)` | Recent PLC events for transfer |
| `getProblemWorkOrders()` | Work orders blocked by MCS transfer state |
| `getRecentTransferFailures()` | Recent failed MCS transfers and PLC causes |

## Guardrails

- AI does not execute cancel, start, complete, stock correction, or route status changes.
- AI may recommend a recovery action, but the user must click the actual button.
- AI answers must include the source status values used for the conclusion.
- If required context is missing, AI should say what data is missing instead of guessing.

## Development Order

1. AI incident analysis API for one work order.
2. React analysis panel on MES work order screen.
3. Natural-language operation query screen.
4. Dashboard operation summary.
5. Alert message generation for PLC failure/interlock.
