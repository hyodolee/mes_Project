# Graph RAG Demo Scenario

이 문서는 포트폴리오 발표에서 Graph RAG가 왜 필요한지 보여주기 위한 시연 예제입니다.

핵심 메시지는 단순합니다.

> Vector RAG는 비슷한 문서를 찾지만, 문서 안의 개념 관계를 놓치면 그럴듯한 오답을 만들 수 있습니다.
> Graph RAG는 이벤트, 필수 정보, PLC 태그, 조치 절차의 관계를 따라가서 오답 가능성을 줄입니다.

## 1. 시연 질문

발표에서는 기술 필드명 대신 현장 용어로 질문합니다.

```text
자재 이동이 실패했는데 목적지 정보가 없다고 나옵니다.
현장에서 어디를 확인해야 하나요?
```

이 질문은 비전공자도 이해할 수 있습니다.

- 자재 이동이 실패했다.
- 목적지 정보가 빠졌다.
- 그래서 어디를 확인해야 하는지 묻는다.

## 2. Vector RAG만 썼을 때 생기는 문제

Vector RAG는 질문과 의미가 비슷한 문서 조각을 찾습니다.

질문에는 다음 단어가 들어 있습니다.

- 자재 이동 실패
- 목적지
- 위치
- 확인

그래서 Vector RAG는 `목적지 정보 누락` 문서가 아니라, 단어가 비슷한 `위치 불일치` 장애 문서를 먼저 가져올 수 있습니다.

### 잘못된 답변 예시

```text
자재가 목적지가 아닌 다른 위치에서 감지된 것으로 보입니다.
위치 센서와 현재 위치 값을 확인하세요.
```

이 답변은 그럴듯하지만 방향이 틀렸습니다.

실제 문제는 `자재가 다른 위치에 도착한 문제`가 아닙니다.

실제 문제는 `자재를 어디로 보내야 하는지 목적지 정보가 빠진 문제`입니다.

따라서 확인해야 하는 것은 현재 위치 센서가 아니라, PLC가 MCS로 보내는 목적지 값입니다.

## 3. Graph RAG가 해결하는 방식

Graph RAG는 문서를 단순 조각으로만 보지 않고, 개념 사이의 관계를 따라갑니다.

```text
자재 이동 시작 신호
  -> 반드시 필요한 정보
  -> 목적지 정보
  -> PLC 목적지 태그
  -> 장애 조치 절차
```

발표용으로는 이렇게 표현합니다.

```text
이동 신호
  -> 목적지 정보가 필요함
  -> 목적지 정보는 PLC 목적지 값에서 옴
  -> 누락되면 PLC 송신 데이터와 목적지 값을 확인해야 함
```

개발자가 보는 실제 필드 관계는 아래와 같습니다.

```text
TRANSFER_STARTED
  -> requires
  -> toLocationCd

toLocationCd
  -> mappedTo
  -> TO_LOCATION_CD

toLocationCd missing
  -> handledBy
  -> SOP-001

SOP-001
  -> check
  -> PLC payload builder
```

## 4. Graph RAG 적용 후 답변 예시

```text
이 문제는 자재가 다른 위치에 도착한 문제가 아니라,
이동 시작 시 목적지 정보가 빠진 문제입니다.

먼저 PLC에서 목적지 위치 값이 MCS로 전달되는지 확인해야 합니다.

확인 순서는 다음과 같습니다.
1. PLC 송신 데이터에 목적지 값이 포함되어 있는지 확인
2. PLC의 목적지 태그 값이 비어 있지 않은지 확인
3. 수정 후 자재 이동 시작 신호를 다시 전송
```

발표 화면에서는 아래 기술 근거를 작게 보여주면 됩니다.

```text
기술 근거
- 이벤트: TRANSFER_STARTED
- 필수 정보: toLocationCd
- PLC 태그: TO_LOCATION_CD
- 조치 문서: SOP-001
```

## 5. 발표용 비교 문장

```text
Vector RAG는 "위치", "목적지", "이동 실패"처럼 비슷한 단어를 기준으로 문서를 찾기 때문에
위치 불일치 문제와 목적지 정보 누락 문제를 혼동할 수 있습니다.

Graph RAG는 이동 신호, 필수 정보, PLC 태그, 조치 절차의 관계를 따라가기 때문에
비슷해 보이는 장애 중에서도 정확한 확인 위치를 안내할 수 있습니다.
```

## 6. 구현용 그래프 데이터 예시

처음부터 그래프 DB가 없어도 됩니다.
아래 관계를 Java 객체나 JSON으로 만들어 시연할 수 있습니다.

```json
{
  "nodes": [
    { "id": "EVENT_TRANSFER_STARTED", "type": "Event", "label": "자재 이동 시작 신호" },
    { "id": "FIELD_TO_LOCATION", "type": "RequiredField", "label": "목적지 정보" },
    { "id": "TAG_TO_LOCATION", "type": "PlcTag", "label": "PLC 목적지 태그" },
    { "id": "SOP_MISSING_DESTINATION", "type": "Sop", "label": "목적지 정보 누락 조치 절차" },
    { "id": "CHECK_PLC_PAYLOAD_BUILDER", "type": "Checkpoint", "label": "PLC payload 송신부" }
  ],
  "edges": [
    { "from": "EVENT_TRANSFER_STARTED", "to": "FIELD_TO_LOCATION", "type": "requires", "label": "반드시 필요" },
    { "from": "FIELD_TO_LOCATION", "to": "TAG_TO_LOCATION", "type": "mappedTo", "label": "PLC 값으로 전달" },
    { "from": "FIELD_TO_LOCATION", "to": "SOP_MISSING_DESTINATION", "type": "handledBy", "label": "누락 시 조치" },
    { "from": "SOP_MISSING_DESTINATION", "to": "CHECK_PLC_PAYLOAD_BUILDER", "type": "check", "label": "확인 위치" }
  ]
}
```

## 7. 구현 단계

1. Vector RAG만 사용한 답변 예시를 만든다.
   - 질문과 비슷한 `위치 불일치` 문서를 일부러 같이 검색되게 둔다.
   - AI가 현재 위치 센서 확인처럼 잘못된 방향으로 답할 수 있음을 보여준다.

2. Graph RAG 관계 데이터를 만든다.
   - 이벤트 -> 필수 정보 -> PLC 태그 -> 조치 절차 관계를 명시한다.

3. 같은 질문에 Graph RAG 경로를 함께 제공한다.
   - AI 프롬프트에 검색 문서뿐 아니라 관계 경로를 넣는다.

4. 답변을 비교한다.
   - Vector RAG: 비슷한 문서 기반의 그럴듯한 오답
   - Graph RAG: 관계 경로 기반의 정확한 확인 위치 안내

## 8. 최종 발표 메시지

```text
이 프로젝트에서 Graph RAG는 단순히 Tool Calling을 대체하기 위한 기능이 아닙니다.

Vector RAG가 비슷한 문서를 찾는 과정에서 장애 유형을 혼동할 수 있기 때문에,
운영 문서 안의 이벤트, 필수 정보, PLC 태그, 조치 절차 관계를 그래프로 연결했습니다.

그 결과 AI는 비슷한 장애 문서에 끌려가지 않고,
문제 원인에 맞는 확인 위치와 조치 절차를 안내할 수 있습니다.
```
