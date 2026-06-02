# MES/MCS 시연 시나리오

이 문서는 AI 기능을 붙이기 전, MES/MCS/PLC 시뮬레이터가 하나의 운영 흐름으로 연결되어 있음을 반복 시연하기 위한 기준입니다.

## 시연 목표

```text
MES 작업오더
  -> MCS 자재 이동 요청
    -> MCS 경로 계산
      -> PLC 이벤트 시뮬레이션
        -> 성공: 재고 반영 및 MES 작업 시작 허용
        -> 실패: MES 작업 시작 차단
        -> 복구: 실패 이동 취소 후 재요청
```

## 권장 시연 데이터

| 항목 | 값 |
|---|---|
| 공장 | 오창1공장 |
| 완제품 | `FG-CEL-001` / 파우치셀 60Ah(NCM811) |
| 자재 | `RM-NCM-001` / NCM811 양극활물질 |
| 작업 수량 | `3` |
| 설비 | `EQ001` |
| 출발 Location | `NCM-01-01` |
| 도착 Location | `NCM-01-02` |
| 경로 기준 | 혼잡 회피 또는 최단 시간 |

`RM-NCM-001`의 사용 가능한 LOT가 없으면 MCS 로케이션 재고에서 먼저 재고를 생성하거나 조정합니다.

## 시나리오 1. 정상 이동

목적: MES가 MCS 자재 이동 완료 전까지 작업 시작을 기다리는 흐름을 보여줍니다.

1. React에서 MES 작업오더 화면을 엽니다.
2. 권장 시연 데이터로 작업오더를 생성합니다.
3. `자재 요청`을 클릭합니다.
4. MCS 이동오더가 생성되었는지 확인합니다.
5. MCS 이동 관리 화면에서 새 이동오더의 ID를 확인합니다.
6. PLC 성공 시뮬레이션을 실행합니다.

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId {TRANSFER_ID} -Scenario Success -Mode PlcApi
```

기대 결과:

- MCS 이동오더 상태가 `완료(COMPLETED)`가 됩니다.
- PLC 이벤트 이력에 `TRANSFER_STARTED`, `TRANSFER_COMPLETED`가 남습니다.
- MCS 로케이션 재고가 출발지에서 차감되고 도착지에 반영됩니다.
- MES 작업오더의 `시작` 버튼이 활성화됩니다.

## 시나리오 2. 실패 이동

목적: PLC 오류나 인터락 발생 시 MES 작업 시작이 차단되는 흐름을 보여줍니다.

1. 새 MES 작업오더를 생성합니다.
2. `자재 요청`을 클릭합니다.
3. MCS 이동 관리 화면에서 새 이동오더의 ID를 확인합니다.
4. PLC 실패 시뮬레이션을 실행합니다.

설비 오류:

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId {TRANSFER_ID} -Scenario EquipmentError -Mode PlcApi
```

인터락:

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId {TRANSFER_ID} -Scenario InterlockBlocked -Mode PlcApi
```

기대 결과:

- MCS 이동오더 상태가 `실패(FAILED)`가 됩니다.
- PLC 이벤트 이력에 오류 이벤트가 남습니다.
- MES 작업오더의 `시작` 버튼은 비활성 상태로 유지됩니다.
- MES에는 MCS 자재 이동이 완료되지 않았다는 안내가 표시됩니다.

## 시나리오 3. 실패 후 복구

목적: 실패 이동은 이력으로 남기고, 취소 후 새 이동 요청으로 복구하는 흐름을 보여줍니다.

1. MCS 이동 상태가 `FAILED`인 작업오더를 준비합니다.
2. MCS 이동 관리 화면에서 실패 이동오더를 `취소`합니다.
3. MES 작업오더 화면으로 돌아갑니다.
4. 같은 작업오더에서 `자재 요청`을 다시 클릭합니다.
5. 새 MCS 이동오더가 생성되었는지 확인합니다.
6. 새 이동오더 ID로 PLC 성공 시뮬레이션을 실행합니다.

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId {NEW_TRANSFER_ID} -Scenario Success -Mode PlcApi
```

기대 결과:

- 이전 실패 이동오더는 `취소(CANCELLED)`로 남습니다.
- 새 이동오더는 `완료(COMPLETED)`가 됩니다.
- MES 작업오더의 `시작` 버튼이 활성화됩니다.

## 시연 전 체크리스트

- MES backend가 `8080`에서 실행 중입니다.
- MCS backend가 `8081`에서 실행 중입니다.
- React frontend가 `3000`에서 실행 중입니다.
- backend 실행 전에 `MES_DB_PASSWORD` 환경변수가 설정되어 있습니다.
- 기존 DB에 `FAILED` 상태 코드가 없다면 `db/patches/mcs/MCS_add_failed_transfer_status.sql`을 한 번 실행합니다.
- PLC 명령은 `C:\dev\mes_project`에서 실행하거나 script path를 절대경로로 지정합니다.
