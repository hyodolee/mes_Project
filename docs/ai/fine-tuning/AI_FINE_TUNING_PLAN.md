# MES/MCS AI Fine-Tuning Plan

## 목표

MES/MCS 프로젝트에 맞는 AI 작업오더 분석 품질을 높이기 위해 fine-tuning을 적용한다.

1차 목표는 사용자가 특정 작업오더를 선택했을 때, AI가 연결된 MES 작업오더, MCS 이동오더, 경로, LOT, PLC 이벤트 상태를 근거로 문제 원인과 조치 방법을 일관된 형식으로 설명하는 것이다.

## 적용 범위

| 구분 | 결정 |
|---|---|
| 1차 기능 | AI 작업오더 분석 |
| 모델 전략 | MES/MCS 통합 fine-tuned model 1개 |
| 입력 데이터 | 작업오더, 생산품목, LOT, MCS 이동오더, 출발/도착 Location, 경로 기준, PLC 이벤트 |
| 출력 형식 | JSON 기반 분석 결과 |
| 우선 제외 | AI 자동 조치, 재고 자동 보정, 작업오더 자동 상태 변경 |

MES와 MCS는 시스템은 분리되어 있지만, 시연과 운영 분석에서는 하나의 업무 흐름으로 연결된다. 따라서 fine-tuning도 MES 모델과 MCS 모델을 따로 만들기보다, 우선 "MES 작업오더와 MCS 자재 이동을 함께 해석하는 통합 모델"로 시작한다.

## Fine-Tuning이 맡는 역할

Fine-tuning은 최신 DB 값을 외우게 하는 작업이 아니다.

Fine-tuning이 맡는 일:

- MES/MCS 업무 용어를 일관되게 이해한다.
- 작업오더와 자재 이동 상태의 관계를 설명한다.
- 실패, 취소, 미요청, 인터락, PLC 오류 같은 시나리오를 운영 관점으로 해석한다.
- 답변 형식을 항상 비슷하게 유지한다.
- 포트폴리오 시연에서 "이 시스템을 이해하는 AI"처럼 보이게 한다.

Fine-tuning이 맡지 않는 일:

- 현재 재고 수량을 기억한다.
- DB 최신 상태를 자체적으로 조회한다.
- 실제 작업오더를 시작/취소/완료한다.
- MCS 이동오더를 자동 생성하거나 상태 변경한다.

현재 상태 조회는 기존 MES/MCS 서비스와 API가 맡고, AI는 백엔드가 넘겨준 근거 데이터를 해석한다.

## 1차 분석 입력 구조

AI 작업오더 분석은 아래 형태의 운영 컨텍스트를 입력으로 받는다.

```json
{
  "workOrder": {
    "workOrderNo": "WO202606010012",
    "status": "WAITING",
    "plantName": "오창1공장",
    "itemCode": "FG-CEL-001",
    "itemName": "파우치셀 60Ah (NCM811)",
    "lotNo": "LOT-FGCEL001-20260601-0012",
    "orderQty": 3,
    "goodQty": 0,
    "defectQty": 0
  },
  "materialTransfer": {
    "transferNo": "MES-53-780300251997",
    "status": "COMPLETED",
    "fromLocation": "CEL-01-02",
    "toLocation": "CEL-01-01",
    "routePolicy": "AVOID_CONGESTION"
  },
  "plcEvents": [
    {
      "eventType": "TRANSFER_COMPLETED",
      "message": "자재 이동 완료"
    }
  ]
}
```

## 1차 분석 출력 구조

Fine-tuned model은 아래 JSON 형식을 우선 목표로 한다.

```json
{
  "summary": "MCS 자재 이동이 완료되어 작업 시작이 가능한 상태입니다.",
  "facts": [
    "작업오더 상태는 대기입니다.",
    "연결된 MCS 이동오더 상태는 완료입니다."
  ],
  "inference": "필요 자재가 도착 Location으로 이동 완료되었으므로 MES 작업 시작 조건을 만족합니다.",
  "impact": "작업 시작 지연 요인은 현재 확인되지 않습니다.",
  "recommendedActions": [
    "MES 작업오더 화면에서 시작 버튼을 눌러 작업을 시작합니다."
  ]
}
```

## 학습 시나리오

| 시나리오 | 설명 |
|---|---|
| 정상 완료 | MCS 이동 완료 후 MES 작업 시작 가능 |
| 자재 요청 전 | MES 작업오더는 있으나 MCS 이동오더가 없음 |
| 이동 요청 중 | MCS 이동오더가 REQUESTED 또는 IN_PROGRESS 상태 |
| 이동 실패 | PLC 오류 또는 설비 오류로 MCS 이동 실패 |
| 인터락 차단 | PLC 인터락으로 이동이 차단됨 |
| 취소 불일치 | MES 작업오더는 취소됐지만 MCS 이동오더가 남아 있음 |
| 재고 부족 | 출발 Location 가용 재고 부족 |
| 경로 막힘 | MCS 경로가 BLOCKED 상태 |
| 혼잡 우회 | 혼잡 회피 기준으로 대체 경로 사용 |
| 위치 불일치 | LOT 현재 위치와 요청 출발 위치가 다름 |

## 개발 순서

1. Fine-tuning 계획 문서화
2. 작업오더 분석용 샘플 케이스 파일 작성
3. 샘플 케이스를 OpenAI fine-tuning JSONL 형식으로 변환하는 스크립트 작성
4. 실제 DB/시연 데이터 기반 케이스를 점진적으로 추가
5. 기본 모델과 fine-tuned model 비교 테스트
6. 검증된 fine-tuned model id를 `OPENAI_MODEL` 환경변수에 적용
7. React AI 작업오더 분석 화면에서 모델명과 fallback 여부 확인

## 데이터셋 파일 구조

```text
docs/ai/fine-tuning/
  AI_FINE_TUNING_PLAN.md
  work-order-analysis-cases.json
  work-order-analysis-extra-cases.json
  work-order-analysis-extra-cases-02.json
  generated/
    work-order-analysis-train.jsonl
    work-order-analysis-validation.jsonl

scripts/ai/
  create-finetune-dataset.mjs
  upload-finetune-files.mjs
  create-finetune-job.mjs
  get-finetune-job.mjs
```

`work-order-analysis-cases.json`은 사람이 읽고 수정하기 쉬운 원본 사례 파일이다.

`work-order-analysis-extra-cases.json`, `work-order-analysis-extra-cases-02.json`은 추가 학습 사례 파일이다. 앞으로도 파일명에 `cases`가 포함된 JSON 파일을 추가하면 생성 스크립트가 자동으로 합쳐서 JSONL을 만든다.

`generated/*.jsonl`은 OpenAI fine-tuning 업로드에 사용할 변환 결과다.

현재 샘플 데이터셋 현황:

| 구분 | 건수 |
|---|---:|
| 전체 사례 | 50 |
| 학습용 train | 38 |
| 검증용 validation | 12 |

생성 명령:

```powershell
node scripts\ai\create-finetune-dataset.mjs
```

생성 후 확인할 파일:

```text
docs/ai/fine-tuning/generated/work-order-analysis-train.jsonl
docs/ai/fine-tuning/generated/work-order-analysis-validation.jsonl
```

## Fine-Tuning 실행 스크립트

실행 위치는 항상 프로젝트 루트다.

```powershell
cd C:\dev\mes_project
```

1. 데이터셋 생성

```powershell
node scripts\ai\create-finetune-dataset.mjs
```

2. OpenAI API Key 설정

```powershell
$env:OPENAI_API_KEY="sk-..."
```

API Key는 코드나 문서에 저장하지 않는다.

3. 업로드 전 dry-run 확인

```powershell
node scripts\ai\upload-finetune-files.mjs --dry-run
```

4. 학습/검증 JSONL 파일 업로드

```powershell
node scripts\ai\upload-finetune-files.mjs
```

성공하면 아래 파일에 OpenAI file id가 저장된다.

```text
docs/ai/fine-tuning/generated/openai-file-ids.json
```

5. Fine-tuning job 생성 전 dry-run 확인

```powershell
node scripts\ai\create-finetune-job.mjs --dry-run
```

기본 base model은 `gpt-4.1-mini-2025-04-14`다. 다른 fine-tuning 지원 모델을 쓰려면 아래처럼 지정한다.

```powershell
node scripts\ai\create-finetune-job.mjs --model gpt-4.1-mini-2025-04-14 --suffix mes-mcs-work-order-analysis
```

6. Fine-tuning job 생성

```powershell
node scripts\ai\create-finetune-job.mjs
```

성공하면 아래 파일에 job 결과가 저장된다.

```text
docs/ai/fine-tuning/generated/openai-finetune-job.json
```

7. Fine-tuning job 상태 조회

```powershell
node scripts\ai\get-finetune-job.mjs
```

완료되면 `fine_tuned_model` 값이 생긴다. 예시는 아래와 같은 형태다.

```text
ft:gpt-4.1-mini:...
```

8. 프로젝트 적용

완료된 model id를 `OPENAI_MODEL`에 넣는다.

```powershell
$env:OPENAI_MODEL="ft:gpt-4.1-mini:..."
```

또는 Antigravity/VS Code 실행 설정의 환경변수에 같은 값을 넣는다.

## 비교 테스트 방법

같은 입력 컨텍스트를 두 모델에 각각 보낸다.

| 비교 대상 | 설명 |
|---|---|
| 기본 모델 | 예: `gpt-4.1-mini-2025-04-14`, `gpt-5-mini` |
| Fine-tuned model | 예: `ft:gpt-4.1-mini:...` |

평가 항목:

| 항목 | 기준 |
|---|---|
| 업무 용어 정확도 | MES 작업오더, MCS 이동오더, LOT, PLC 이벤트를 혼동하지 않는가 |
| 원인 추론 | 상태값을 근거로 원인을 설명하는가 |
| 조치 적합성 | 실제 화면/업무 흐름에서 가능한 조치를 제안하는가 |
| 답변 일관성 | 항상 summary/facts/inference/impact/recommendedActions 형식을 지키는가 |
| 과장 방지 | 근거 데이터에 없는 내용을 꾸며내지 않는가 |

## 운영 주의사항

- 학습 데이터에는 API Key, DB 비밀번호, 개인 토큰을 절대 넣지 않는다.
- 실제 회사 데이터가 들어간 경우 공개 저장소에 커밋하지 않는다.
- Fine-tuning 후에도 최신 운영 상태는 항상 MES/MCS API에서 조회해서 프롬프트에 넣는다.
- AI 응답은 조언이며, 상태 변경은 기존 백엔드 API와 사용자의 명시적 버튼 클릭으로만 수행한다.
