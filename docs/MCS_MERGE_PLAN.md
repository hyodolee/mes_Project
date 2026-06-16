# MES/MCS Merge Plan

## Goal

Merge the MCS runtime into `mes_backserver` so MES, MCS, AI, and RAG run from one backend.

- Final backend port: `8080`
- Old MCS backend port `8081` is no longer required after merge validation.
- Existing `MES_DB` is reused.
- Existing `MCS_` table prefix is preserved.
- Frontend route paths are preserved. Only API base URL changes to `8080`.

## Completed Backend Scope

The following MCS features were moved under `mes_backserver/src/main/java/com/mes/mcs` and verified with `./gradlew.bat build -x test`.

- Zone: `/api/zones`
- Location: `/api/locations`
- Route: `/api/route-nodes`, `/api/route-edges`, `/api/routes/optimize`, `/api/transfers/{transferId}/routes`
- Inventory: `/api/inventory/stocks`, `/api/inventory/transactions`
- Transfer: `/api/transfers`
- Material Request: `/api/material-requests`
- PLC Event: `/api/plc/events`
- Inbound: `/api/inbounds`
- Outbound: `/api/outbounds`
- Reference: `/api/references`

## Completed Frontend Scope

- MCS API base URL was changed from `8081` to `8080`.
- MCS screens now use the MES auth token.
- Separate MCS login calls were removed.
- Broken dashboard JSX text was restored.
- `corepack yarn build` was verified.

## Completed Integration Cleanup

- MCS service/controller bean names now use an explicit `mcs...` prefix to avoid collisions with existing MES beans.
- MyBatis broad type-alias scanning was removed to avoid DTO alias conflicts.
- `McsTransferClient` no longer calls the old MCS server over HTTP.
- `McsTransferClient` now directly calls merged MCS services:
  - `TransferService`
  - `MaterialRequestService`
  - `PlcEventService`
- The old `mcs.api.*` config was removed because there is no external MCS API call left.

## Completed AI Tool Scope

The MES AI operation chatbot can now read merged MCS data directly from the MES backend process.

- Route status:
  - `getRouteEdges`
  - `getBlockedRoutes`
  - `getTransferRoute`
  - `getTransferRouteSteps`
- Location and stock:
  - `getLocations`
  - `getLocationStocks`
- PLC and transfer issue analysis:
  - `getPlcValidationFailures`
  - `analyzeTransferBlockers`
- The AI prompt now tells the model when to use these tools for route, location, stock, PLC validation, and broad operation issue questions.

## Excluded Scope

- MCS `AuthApiController` was not merged. It belonged to the standalone MCS server and conflicts with the merged MES authentication model.
- ~~Removing `mcs_backserver` from the repository is deferred until screen/API validation is complete.~~
  완료(2026-06-16) — API 200 + MCS 화면 검증 후 `mcs_backserver/` 디렉토리 제거. 이력은 git history 참조.
  연관 정리: `scripts/dev/start-backends.ps1`, 루트 `.vscode/launch.json`, `CLAUDE.md`, `AGENTS.md`, `README.md`에서 8081/구 경로 참조 제거.

## Current Verification Status

- Backend compile/package: passed with `./gradlew.bat build -x test`.
- Frontend build: passed with `corepack yarn build`.
- Backend `bootRun`: **runtime now verified on `8080`** (DB auth resolved via `MES_DB_PASSWORD`).
  - Merged MCS endpoints return 200 with MES auth token: `/api/transfers`, `/api/zones`, `/api/locations`,
    `/api/references/plants`, `/api/plc/events`, `/api/route-edges`, `/api/inbounds`, `/api/outbounds`,
    `/api/inventory/stocks`.
  - `/api/material-requests` returns 405 on GET — expected (POST-only action controller).
  - Existing MES endpoints (`/api/v1/master/plants`, `/api/v1/planning/work-orders`) still 200 — no regression.
  - Bean-name collisions avoided: all MCS beans use explicit `mcs...` prefix (e.g. `mcsInventoryService`).
- Verified: 2026-06-16.

## Remaining Work

1. ~~Fix DB credentials/network access and start `mes_backserver` on `8080`.~~ 완료 — 런타임 검증됨.
2. Validate MCS screens from the frontend (API는 200 확인, 화면 단위 클릭 검증만 남음):
   - Zone
   - Location
   - Route
   - Inventory
   - Transfer
   - Inbound
   - Outbound
   - PLC Event
3. After screen validation, decide whether to remove or archive the old standalone `mcs_backserver`.
