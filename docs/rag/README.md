# MES/MCS RAG 문서 세트

이 폴더는 AI 챗봇과 운영 분석 기능이 참고할 현장 문서 원본입니다.

RAG에서는 문서의 형식이 중요합니다. 문서는 사람이 읽기에도 자연스러워야 하지만, AI가 검색하기 좋은 단위로도 나뉘어 있어야 합니다. 그래서 이 프로젝트는 PDF를 원본으로 쓰지 않고 Markdown을 원본으로 관리합니다.

## 문서 구성

| 문서 | 목적 |
|---|---|
| [plc-mcs-communication-spec.md](plc-mcs-communication-spec.md) | PLC가 MCS로 보내는 이벤트, payload, 필수 필드 정의 |
| [plc-tag-mapping.md](plc-tag-mapping.md) | MCS 필드와 PLC 태그/주소 매핑표 |
| [plc-troubleshooting-sop.md](plc-troubleshooting-sop.md) | PLC 이벤트 오류, 데이터 누락, 인터락 발생 시 장애 조치 절차 |

## 왜 Markdown을 원본으로 쓰는가

- Chroma에 적재할 때 제목, 표, 문단 단위로 쪼개기 쉽습니다.
- Git에서 변경 이력이 명확하게 남습니다.
- 사람이 문서를 수정하기 쉽습니다.
- 필요하면 나중에 PDF, HTML, 발표 자료로 변환할 수 있습니다.

## RAG 적재 기준

권장 chunk 단위:

- 큰 제목 하나 전체가 아니라, `###` 이하의 작은 절 단위
- 표는 행 단위 또는 표 전체 단위
- 코드 샘플은 코드 블록 하나 단위
- 장애 조치 문서는 "증상 + 원인 + 확인 위치 + 조치"를 한 묶음으로 유지

AI 답변은 이 문서 내용을 근거로 하되, 실제 설비 제어 또는 데이터 수정은 직접 수행하지 않습니다.
