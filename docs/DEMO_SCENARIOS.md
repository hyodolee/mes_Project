# MES/MCS Demo Scenarios

This document fixes the repeatable demo flow before AI features are added.

## Demo Goal

Show that MES, MCS, and PLC simulation are connected as one operational flow.

```text
MES work order
  -> MCS material transfer request
    -> MCS route calculation
      -> PLC event simulation
        -> success: stock reflected and MES start allowed
        -> failure: MES start blocked
        -> recovery: failed transfer cancelled and retried
```

## Fixed Demo Data

| Item | Value |
|---|---|
| Plant | 오창1공장 |
| Finished item | `FG-CEL-001` / 파우치셀 60Ah(NCM811) |
| Material item | `RM-NCM-001` / NCM811 양극활물질 |
| Work quantity | `3` |
| Equipment | `EQ001` |
| Source location | `NCM-01-01` |
| Destination location | `NCM-01-02` |
| Route rule | 혼잡 회피 |

If `RM-NCM-001` has no available LOT in MCS location stock, create or adjust stock before the demo.

## Scenario 1. Normal Transfer

Purpose: show that MES waits for MCS material movement.

Steps:

1. Open MES 작업 오더.
2. Create a work order with the fixed demo data.
3. Click `자재 요청`.
4. Confirm that an MCS transfer order is created.
5. Open MCS 이동 관리 and select the new transfer.
6. Copy the transfer ID.
7. Run PLC success simulation.

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId {TRANSFER_ID} -Scenario Success -Mode PlcApi
```

Expected result:

- MCS transfer status becomes `완료(COMPLETED)`.
- MCS movement detail shows PLC `TRANSFER_STARTED` and `TRANSFER_COMPLETED`.
- MES work order shows `MCS 이동 완료 - 작업 시작 가능`.
- MES `시작` button becomes available.

## Scenario 2. Failure Transfer

Purpose: show that PLC errors block MES production start.

Steps:

1. Create a new MES work order.
2. Click `자재 요청`.
3. Open MCS 이동 관리 and select the new transfer.
4. Copy the transfer ID.
5. Run PLC failure simulation.

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId {TRANSFER_ID} -Scenario EquipmentError -Mode PlcApi
```

Alternative interlock simulation:

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId {TRANSFER_ID} -Scenario InterlockBlocked -Mode PlcApi
```

Expected result:

- MCS transfer status becomes `실패(FAILED)`.
- MCS movement detail shows the error PLC event.
- MES work order shows `MCS 이동 실패 - 취소 후 재요청 필요`.
- MES `시작` button remains disabled.

## Scenario 3. Recovery After Failure

Purpose: show that failed movement is kept as history and recovered by a new transfer request.

Steps:

1. Start from a work order whose MCS transfer is `FAILED`.
2. Open MCS 이동 관리.
3. Click `취소` on the failed transfer.
4. Return to MES 작업 오더.
5. Click `자재 요청` again on the same work order.
6. Confirm that a new MCS transfer order is created.
7. Run PLC success simulation for the new transfer ID.

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId {NEW_TRANSFER_ID} -Scenario Success -Mode PlcApi
```

Expected result:

- Old transfer remains `취소(CANCELLED)`.
- New transfer becomes `완료(COMPLETED)`.
- MES work order shows `MCS 이동 완료 - 작업 시작 가능`.
- MES `시작` button becomes available.

## Demo Checklist

- MES backend running on `8080`.
- MCS backend running on `8081`.
- React frontend running on `3000`.
- `MES_DB_PASSWORD` is set before backend startup.
- `mcs/MCS_add_failed_transfer_status.sql` has been run if the DB was created before the `FAILED` code was added.
- PLC command is run from `C:\dev\mes_project` or with the absolute script path.
