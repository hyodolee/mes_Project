# MES/MCS RAG 문서 세트

이 폴더는 AI 챗봇과 운영 분석 기능이 참고할 현장 문서 원본입니다.

RAG에서는 문서 형식이 중요합니다. 문서는 사람이 읽기에도 자연스러워야 하지만, AI가 검색하기 좋은 단위로도 나뉘어 있어야 합니다. 이 프로젝트는 PDF를 원본으로 쓰지 않고 Markdown을 원본으로 관리합니다.

## 문서 구성

| 문서 | 목적 |
|---|---|
| [plc-mcs-communication-spec.md](plc-mcs-communication-spec.md) | PLC가 MCS로 보내는 이벤트 payload, 필수 필드 정의 |
| [plc-tag-mapping.md](plc-tag-mapping.md) | MCS 필드와 PLC 태그/주소 매핑 |
| [plc-troubleshooting-sop.md](plc-troubleshooting-sop.md) | PLC 이벤트 오류, 데이터 누락, 인터록 발생 시 조치 절차 |
| [glossary.md](glossary.md) | 현장 자연어 표현 ↔ 이벤트/필드/태그 동의어 매핑 (검색어 보강용) |
| [GRAPH_RAG_DEMO_SCENARIO.md](GRAPH_RAG_DEMO_SCENARIO.md) | Vector RAG 오답 가능성과 Graph RAG 개선 효과를 설명하는 발표/구현용 시연 예제 |

## Markdown을 원본으로 쓰는 이유

- Chroma에 적재할 때 제목, 표, 문단 단위로 쪼개기 쉽습니다.
- Git에서 변경 이력을 명확하게 확인할 수 있습니다.
- 사람이 문서를 수정하기 쉽습니다.
- 필요하면 나중에 PDF, HTML, 발표 자료로 변환할 수 있습니다.

## 구현 단계 (현재 → 목표)

RAG는 단계적으로 도입합니다. 현재 코드 상태와 목표를 구분합니다.

| 단계 | 방식 | 상태 |
|---|---|---|
| 1차 | 로컬 Markdown 키워드 검색 (`OperationDocumentSearchService`) | 구현됨 — 매칭 라인 주변 스니펫 반환 |
| 2차 | Vector RAG (Chroma 등 임베딩 검색) | 예정 — 아래 "RAG 적재 기준"의 청크 단위 적용 |
| 3차 | Graph RAG (이벤트·필드·태그·조치 관계 탐색) | 예정 — [GRAPH_RAG_DEMO_SCENARIO.md](GRAPH_RAG_DEMO_SCENARIO.md) 참고 |

## RAG 적재 기준

> 아래 청크 기준은 2차 Vector RAG 적용 시 기준입니다. 1차 키워드 검색은 라인 단위 스니펫이라 표의 헤더 맥락이 빠질 수 있습니다.

권장 chunk 단위:

- 큰 제목 하나 전체가 아니라 `###` 이하의 작은 절 단위
- 표는 행 단위 또는 표 전체 단위
- 코드 샘플은 코드 블록 하나 단위
- 장애 조치 문서는 "증상 + 원인 + 확인 위치 + 조치"를 한 묶음으로 유지

AI 답변은 이 문서 내용을 근거로 하되, 실제 설비 제어 또는 데이터 수정은 직접 수행하지 않습니다.
