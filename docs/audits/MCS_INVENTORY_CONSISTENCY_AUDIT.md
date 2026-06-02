# MCS Inventory Consistency Audit

## Purpose

This document records the consistency check for MCS inventory-affecting workflows.
The main rule is that `MCS_LOCATION_STOCK` is the source of truth for item/lot stock,
while `MCS_LOCATION.CURRENT_USAGE` and `MCS_LOCATION.LOCATION_STATUS` are summary
values that must be synchronized after stock changes.

## Checked Workflows

| Workflow | Expected DB effect | Check result |
|---|---|---|
| Stock adjustment | Update `MCS_LOCATION_STOCK`, insert `MCS_LOC_TRANS_HIS`, sync `MCS_LOCATION` summary | Fixed |
| Transfer completed | Decrease source stock, increase destination stock, insert `TF_OUT`/`TF_IN`, sync both locations | Fixed |
| Inbound completed | Increase destination stock, insert `IB_IN`, mark item `STOCKED`, sync location | Fixed |
| Outbound shipped | Decrease source stock, insert `OB_OUT`, mark item `SHIPPED`, sync location | Fixed |
| Location delete | Should block delete when stock exists | Still needs guard |
| Zone delete | Should block delete when child locations exist | Still needs guard |

## Fix Summary

### Location Summary Sync

Added `InventoryMapper.syncLocationUsage(locationId, updUserId)`.

It recalculates:

- `MCS_LOCATION.CURRENT_USAGE = SUM(MCS_LOCATION_STOCK.STOCK_QTY)`
- `MCS_LOCATION.LOCATION_STATUS`
  - `EMPTY` when usage is `0`
  - `FULL` when usage is greater than or equal to `MAX_CAPACITY`
  - `PARTIAL` otherwise

### Transfer

`TransferService` now synchronizes both source and destination locations after stock is moved.

### Stock Adjustment

`InventoryService.adjustStock` now synchronizes the changed location after adjustment.

### Inbound

`InboundService.changeOrderStatus(..., COMPLETED, ...)` now:

- Requires at least one inbound item.
- Requires a target location per item.
- Uses `ACTUAL_QTY`, or `EXPECTED_QTY` when actual quantity is empty.
- Inserts a missing `MCS_LOCATION_STOCK` row if needed.
- Increases stock.
- Inserts `IB_IN` transaction history.
- Marks the item as `STOCKED`.
- Synchronizes the target location summary.

### Outbound

`OutboundService.changeOrderStatus(..., SHIPPED, ...)` now:

- Requires at least one outbound item.
- Requires a source location per item.
- Uses quantity in this order: `SHIPPED_QTY`, `PICKED_QTY`, `ALLOCATED_QTY`, `REQUESTED_QTY`.
- Validates available stock before shipping.
- Decreases stock.
- Inserts `OB_OUT` transaction history.
- Marks the item as `SHIPPED` and stores the shipped quantity.
- Synchronizes the source location summary.

## Existing Data Correction

Existing inconsistent `MCS_LOCATION` summary values should be corrected once:

```sql
UPDATE MCS_LOCATION l
LEFT JOIN (
    SELECT LOCATION_ID, COALESCE(SUM(STOCK_QTY), 0) AS CURRENT_USAGE
    FROM MCS_LOCATION_STOCK
    GROUP BY LOCATION_ID
) s ON l.LOCATION_ID = s.LOCATION_ID
SET l.CURRENT_USAGE = COALESCE(s.CURRENT_USAGE, 0),
    l.LOCATION_STATUS = CASE
        WHEN COALESCE(s.CURRENT_USAGE, 0) <= 0 THEN 'EMPTY'
        WHEN COALESCE(s.CURRENT_USAGE, 0) >= l.MAX_CAPACITY THEN 'FULL'
        ELSE 'PARTIAL'
    END,
    l.UPD_USER_ID = 'SYSTEM',
    l.UPD_DTM = NOW()
WHERE l.LOCATION_ID IS NOT NULL;
```

## Remaining Risks

- MES `INV_STOCK` synchronization for inbound/outbound should be reviewed separately.
  Transfer already has MES warehouse stock sync logic, but inbound/outbound currently focus on MCS location stock.
- Delete guards are still needed for `MCS_LOCATION` and `MCS_ZONE`.
- Automated integration tests should be added for stock-affecting status transitions.
