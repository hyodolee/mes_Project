# MES/MCS Material Flow Test Plan

This document is the current reference for testing the MES work order and MCS material transfer flow.

## Goal

MES owns the production work order. MCS owns material location, route selection, and physical movement status. PLC simulation drives MCS movement progress or failure.

## Normal Flow

1. In MES, create a work order.
2. In MES, click material request.
3. MES sends only the production need to MCS.
4. MCS selects source stock/LOT, destination location, and route rule, then creates a transfer order.
5. MCS transfer remains `REQUESTED` until PLC starts it.
6. Run PLC simulator with `-Scenario Success -Mode PlcApi`.
7. MCS changes transfer to `IN_PROGRESS`, then `COMPLETED`.
8. MCS updates location stock and transfer route status.
9. MES allows work order start only after the linked MCS transfer is `COMPLETED`.

## Failure Flow

1. Create a MES work order.
2. Request material so that MCS creates a transfer order.
3. Run PLC simulator with a failure scenario, for example `EquipmentError`, `SensorMismatch`, `InterlockBlocked`, or `Timeout`.
4. MCS records the PLC event and changes the transfer order to `FAILED`.
5. MES must not allow work order start while the linked transfer is `FAILED`.
6. Operator cancels the failed transfer in MCS.
7. MES material request can be retried after the failed transfer is cancelled.

## Cancel Flow

1. Create a MES work order.
2. Request material from MES.
3. Before PLC completion, cancel the MES work order.
4. MES calls MCS cancellation for the active linked transfer.
5. MCS changes the transfer to `CANCELLED`.
6. Cancelled or completed transfers must not be physically executed again.

## Duplicate Request Rule

MES must block duplicate material requests while an active linked MCS transfer exists.

Active statuses:

- `REQUESTED`
- `IN_PROGRESS`
- `COMPLETED`
- `FAILED`

Retry is allowed only when the previous transfer is `CANCELLED`.

## SQL Required

Run this once if the existing database does not have the failed transfer status code:

```sql
source mcs/MCS_add_failed_transfer_status.sql;
```

For a fresh MCS installation, `mcs/MCS_install.sql` already includes `MCS_TF_STATUS = FAILED`.

## Demo Script Examples

Success:

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId 12 -Scenario Success -Mode PlcApi
```

Equipment failure:

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId 12 -Scenario EquipmentError -Mode PlcApi
```

Interlock:

```powershell
powershell.exe -ExecutionPolicy Bypass -File "C:\dev\mes_project\scripts\plc\simulate-transfer.ps1" -TransferId 12 -Scenario InterlockBlocked -Mode PlcApi
```
