package com.mcs.application.service.transfer;

import com.mcs.application.service.location.LocationService;
import com.mcs.application.service.route.RouteService;
import com.mcs.domain.inventory.dto.LocStockDto;
import com.mcs.domain.inventory.dto.LocTransHisDto;
import com.mcs.domain.location.dto.LocationDto;
import com.mcs.domain.transfer.dto.TransferItemDto;
import com.mcs.domain.transfer.dto.TransferOrderDto;
import com.mcs.domain.transfer.dto.TransferSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import com.mcs.infra.persistence.mybatis.mapper.transfer.TransferMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

    private final TransferMapper transferMapper;
    private final InventoryMapper inventoryMapper;
    private final LocationService locationService;
    private final RouteService routeService;

    public PageResponse<TransferOrderDto> getTransferList(TransferSearchDto searchDto) {
        List<TransferOrderDto> list = transferMapper.selectTransferList(searchDto);
        long total = transferMapper.selectTransferCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    public TransferOrderDto getTransferOrder(Long transferId) {
        return transferMapper.selectTransferById(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
    }

    public List<TransferItemDto> getTransferItems(Long transferId) {
        return transferMapper.selectTransferItems(transferId);
    }

    @Transactional
    public Long createTransferOrder(TransferOrderDto orderDto) {
        if (orderDto.getFromLocationId().equals(orderDto.getToLocationId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        transferMapper.insertTransferOrder(orderDto);
        TransferOrderDto savedOrder = transferMapper.selectTransferByNo(orderDto.getTransferNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        routeService.createRouteForTransfer(savedOrder.getTransferId(), normalizeOptimizeRule(orderDto.getOptimizeRule()), orderDto.getRegUserId());
        return savedOrder.getTransferId();
    }

    @Transactional
    public void createTransferItem(Long transferId, TransferItemDto itemDto) {
        TransferOrderDto order = getTransferOrder(transferId);
        if (!"REQUESTED".equals(order.getTransferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (itemDto.getTransferQty() == null || itemDto.getTransferQty() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        validateAvailableStock(order, itemDto.getItemCd(), normalizeLotNo(itemDto.getLotNo()), itemDto.getTransferQty());

        TransferItemDto dto = new TransferItemDto(
                null,
                transferId,
                itemDto.getItemCd(),
                normalizeLotNo(itemDto.getLotNo()),
                itemDto.getTransferQty(),
                "REQUESTED",
                "SYSTEM",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        transferMapper.insertTransferItem(dto);
    }

    @Transactional
    public void updateTransferOrder(TransferOrderDto orderDto) {
        TransferOrderDto existing = getTransferOrder(orderDto.getTransferId());
        if (!"REQUESTED".equals(existing.getTransferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (orderDto.getFromLocationId().equals(orderDto.getToLocationId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        transferMapper.updateTransferOrder(orderDto);
        routeService.createRouteForTransfer(orderDto.getTransferId(), normalizeOptimizeRule(orderDto.getOptimizeRule()), orderDto.getUpdUserId());
    }

    @Transactional
    public void deleteTransferItem(Long transferId, Long transferItemId) {
        TransferOrderDto existing = getTransferOrder(transferId);
        if (!"REQUESTED".equals(existing.getTransferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        transferMapper.deleteTransferItem(transferItemId, transferId);
    }

    @Transactional
    public void deleteTransferOrder(Long transferId) {
        TransferOrderDto existing = getTransferOrder(transferId);
        if (!"REQUESTED".equals(existing.getTransferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        routeService.deleteTransferRoute(transferId);
        transferMapper.deleteTransferItems(transferId);
        transferMapper.deleteTransferOrder(transferId);
    }

    @Transactional
    public void changeOrderStatus(Long transferId, String newStatus, String userId) {
        TransferOrderDto order = getTransferOrder(transferId);
        String currentStatus = order.getTransferStatus();
        validateMesRequestedTransferStatusChange(order, newStatus, userId);

        if ("IN_PROGRESS".equals(newStatus)) {
            if (!"REQUESTED".equals(currentStatus)) {
                throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
            }
            validateTransferItems(order);
            routeService.ensureRouteForTransfer(transferId, "SHORTEST_TIME", userId);
            routeService.changeTransferRouteStatus(transferId, "ACTIVE", userId);
        } else if ("COMPLETED".equals(newStatus)) {
            if (!"IN_PROGRESS".equals(currentStatus)) {
                throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
            }
            completeTransfer(order, userId);
            routeService.changeTransferRouteStatus(transferId, "COMPLETED", userId);
        } else if ("CANCELLED".equals(newStatus)) {
            if (!"REQUESTED".equals(currentStatus) && !"IN_PROGRESS".equals(currentStatus) && !"FAILED".equals(currentStatus)) {
                throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
            }
            routeService.changeTransferRouteStatus(transferId, "FAILED", userId);
        } else {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        transferMapper.updateTransferStatus(transferId, newStatus, userId);
    }

    @Transactional
    public void markOrderFailed(Long transferId, String message, String userId) {
        TransferOrderDto order = getTransferOrder(transferId);
        String currentStatus = order.getTransferStatus();
        if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        routeService.changeTransferRouteStatus(transferId, "FAILED", userId);
        transferMapper.updateTransferStatus(transferId, "FAILED", userId);
    }

    private void validateMesRequestedTransferStatusChange(TransferOrderDto order, String newStatus, String userId) {
        if (!isMesRequestedTransfer(order) || "PLC".equals(userId)) {
            return;
        }
        if ("IN_PROGRESS".equals(newStatus) || "COMPLETED".equals(newStatus)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "MES 자재요청 이동오더는 PLC 이벤트로 시작/완료 처리해야 합니다."
            );
        }
    }

    private boolean isMesRequestedTransfer(TransferOrderDto order) {
        return order.getTransferNo() != null && order.getTransferNo().startsWith("MES-");
    }

    private void completeTransfer(TransferOrderDto order, String userId) {
        List<TransferItemDto> items = getTransferItems(order.getTransferId());
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이동 품목을 먼저 추가해야 합니다.");
        }

        LocationDto fromLocation = locationService.getLocation(order.getFromLocationId());
        LocationDto toLocation = locationService.getLocation(order.getToLocationId());

        for (TransferItemDto item : items) {
            moveItem(order, item, fromLocation, toLocation, userId);
            transferMapper.updateTransferItemStatus(item.getTransferItemId(), "MOVED", userId);
        }
    }

    private void validateTransferItems(TransferOrderDto order) {
        List<TransferItemDto> items = getTransferItems(order.getTransferId());
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이동 품목을 먼저 추가해야 합니다.");
        }

        for (TransferItemDto item : items) {
            validateAvailableStock(order, item.getItemCd(), normalizeLotNo(item.getLotNo()), item.getTransferQty());
        }
    }

    private void validateAvailableStock(TransferOrderDto order, String itemCd, String lotNo, Double transferQty) {
        LocStockDto stock = inventoryMapper
                .selectLocStockForUpdate(order.getPlantCd(), order.getFromLocationId(), itemCd, lotNo)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "출발 로케이션에 해당 품목/LOT 재고가 없습니다. 품목과 LOT 번호를 확인하세요."
                ));

        if (stock.getAvailableQty() < transferQty) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "출발 로케이션의 가용 재고가 부족합니다. 현재 가용수량: " + stock.getAvailableQty()
            );
        }
    }

    private void moveItem(
            TransferOrderDto order,
            TransferItemDto item,
            LocationDto fromLocation,
            LocationDto toLocation,
            String userId
    ) {
        String lotNo = normalizeLotNo(item.getLotNo());
        double qty = item.getTransferQty();

        LocStockDto fromStock = inventoryMapper
                .selectLocStockForUpdate(order.getPlantCd(), order.getFromLocationId(), item.getItemCd(), lotNo)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "출발 로케이션에 해당 품목/LOT 재고가 없습니다. 품목과 LOT 번호를 확인하세요."
                ));

        if (fromStock.getAvailableQty() < qty) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "출발 로케이션의 가용 재고가 부족합니다. 현재 가용수량: " + fromStock.getAvailableQty()
            );
        }

        double fromBeforeQty = fromStock.getStockQty();
        double fromAfterQty = fromBeforeQty - qty;
        inventoryMapper.updateLocStockQty(fromStock.getLocStockId(), -qty, userId);
        insertTransferHistory(order, fromStock.getLocStockId(), "TF_OUT", qty, fromBeforeQty, fromAfterQty, userId);

        LocStockDto toStock = inventoryMapper
                .selectLocStockForUpdate(order.getPlantCd(), order.getToLocationId(), item.getItemCd(), lotNo)
                .orElseGet(() -> createEmptyDestinationStock(order, item, lotNo, userId));

        double toBeforeQty = toStock.getStockQty();
        double toAfterQty = toBeforeQty + qty;
        inventoryMapper.updateLocStockQty(toStock.getLocStockId(), qty, userId);
        insertTransferHistory(order, toStock.getLocStockId(), "TF_IN", qty, toBeforeQty, toAfterQty, userId);
        inventoryMapper.syncLocationUsage(order.getFromLocationId(), userId);
        inventoryMapper.syncLocationUsage(order.getToLocationId(), userId);

        syncMesWarehouseStockIfNeeded(order, item, fromLocation, toLocation, lotNo, qty, userId);
        insertMesTransferHistory(order, item, fromLocation, toLocation, lotNo, qty, userId);
    }

    private LocStockDto createEmptyDestinationStock(
            TransferOrderDto order,
            TransferItemDto item,
            String lotNo,
            String userId
    ) {
        inventoryMapper.insertLocStock(new LocStockDto(
                null,
                order.getPlantCd(),
                order.getToLocationId(),
                item.getItemCd(),
                lotNo,
                0.0,
                0.0,
                null,
                userId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
        return inventoryMapper
                .selectLocStockForUpdate(order.getPlantCd(), order.getToLocationId(), item.getItemCd(), lotNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
    }

    private void insertTransferHistory(
            TransferOrderDto order,
            Long locStockId,
            String transType,
            double qty,
            double beforeQty,
            double afterQty,
            String userId
    ) {
        inventoryMapper.insertLocTransHis(new LocTransHisDto(
                null,
                order.getPlantCd(),
                locStockId,
                transType,
                qty,
                beforeQty,
                afterQty,
                "TF",
                order.getTransferNo(),
                order.getTransferId(),
                order.getTransferReason(),
                userId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    private void syncMesWarehouseStockIfNeeded(
            TransferOrderDto order,
            TransferItemDto item,
            LocationDto fromLocation,
            LocationDto toLocation,
            String lotNo,
            double qty,
            String userId
    ) {
        if (fromLocation.getWarehouseCd().equals(toLocation.getWarehouseCd())) {
            return;
        }

        int updated = inventoryMapper.updateMesWarehouseStockQty(
                order.getPlantCd(),
                fromLocation.getWarehouseCd(),
                item.getItemCd(),
                lotNo,
                -qty,
                userId
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        inventoryMapper.upsertMesWarehouseStockQty(
                order.getPlantCd(),
                toLocation.getWarehouseCd(),
                item.getItemCd(),
                lotNo,
                qty,
                userId
        );
    }

    private void insertMesTransferHistory(
            TransferOrderDto order,
            TransferItemDto item,
            LocationDto fromLocation,
            LocationDto toLocation,
            String lotNo,
            double qty,
            String userId
    ) {
        inventoryMapper.insertMesTransferHistory(
                order.getPlantCd(),
                createMesTransNo(order.getTransferId(), item.getTransferItemId()),
                item.getItemCd(),
                lotNo,
                qty,
                fromLocation.getWarehouseCd(),
                toLocation.getWarehouseCd(),
                fromLocation.getLocationCd(),
                toLocation.getLocationCd(),
                order.getTransferNo(),
                order.getTransferId(),
                userId,
                userId
        );
    }

    private String createMesTransNo(Long transferId, Long transferItemId) {
        return String.format("MT%010d%010d", transferId, transferItemId);
    }

    private String normalizeLotNo(String lotNo) {
        return lotNo == null ? "" : lotNo.trim();
    }

    private String normalizeOptimizeRule(String optimizeRule) {
        return optimizeRule == null || optimizeRule.isBlank() ? "SHORTEST_TIME" : optimizeRule;
    }
}

