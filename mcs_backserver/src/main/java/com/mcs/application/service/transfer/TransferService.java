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
        if (orderDto.fromLocationId().equals(orderDto.toLocationId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        transferMapper.insertTransferOrder(orderDto);
        TransferOrderDto savedOrder = transferMapper.selectTransferByNo(orderDto.transferNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
        routeService.createRouteForTransfer(savedOrder.transferId(), normalizeOptimizeRule(orderDto.optimizeRule()), orderDto.regUserId());
        return savedOrder.transferId();
    }

    @Transactional
    public void createTransferItem(Long transferId, TransferItemDto itemDto) {
        TransferOrderDto order = getTransferOrder(transferId);
        if (!"REQUESTED".equals(order.transferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (itemDto.transferQty() == null || itemDto.transferQty() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        validateAvailableStock(order, itemDto.itemCd(), normalizeLotNo(itemDto.lotNo()), itemDto.transferQty());

        TransferItemDto dto = new TransferItemDto(
                null,
                transferId,
                itemDto.itemCd(),
                normalizeLotNo(itemDto.lotNo()),
                itemDto.transferQty(),
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
        TransferOrderDto existing = getTransferOrder(orderDto.transferId());
        if (!"REQUESTED".equals(existing.transferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (orderDto.fromLocationId().equals(orderDto.toLocationId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        transferMapper.updateTransferOrder(orderDto);
        routeService.createRouteForTransfer(orderDto.transferId(), normalizeOptimizeRule(orderDto.optimizeRule()), orderDto.updUserId());
    }

    @Transactional
    public void deleteTransferItem(Long transferId, Long transferItemId) {
        TransferOrderDto existing = getTransferOrder(transferId);
        if (!"REQUESTED".equals(existing.transferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        transferMapper.deleteTransferItem(transferItemId, transferId);
    }

    @Transactional
    public void deleteTransferOrder(Long transferId) {
        TransferOrderDto existing = getTransferOrder(transferId);
        if (!"REQUESTED".equals(existing.transferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        routeService.deleteTransferRoute(transferId);
        transferMapper.deleteTransferItems(transferId);
        transferMapper.deleteTransferOrder(transferId);
    }

    @Transactional
    public void changeOrderStatus(Long transferId, String newStatus, String userId) {
        TransferOrderDto order = getTransferOrder(transferId);
        String currentStatus = order.transferStatus();
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
        String currentStatus = order.transferStatus();
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
        return order.transferNo() != null && order.transferNo().startsWith("MES-");
    }

    private void completeTransfer(TransferOrderDto order, String userId) {
        List<TransferItemDto> items = getTransferItems(order.transferId());
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이동 품목을 먼저 추가해야 합니다.");
        }

        LocationDto fromLocation = locationService.getLocation(order.fromLocationId());
        LocationDto toLocation = locationService.getLocation(order.toLocationId());

        for (TransferItemDto item : items) {
            moveItem(order, item, fromLocation, toLocation, userId);
            transferMapper.updateTransferItemStatus(item.transferItemId(), "MOVED", userId);
        }
    }

    private void validateTransferItems(TransferOrderDto order) {
        List<TransferItemDto> items = getTransferItems(order.transferId());
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이동 품목을 먼저 추가해야 합니다.");
        }

        for (TransferItemDto item : items) {
            validateAvailableStock(order, item.itemCd(), normalizeLotNo(item.lotNo()), item.transferQty());
        }
    }

    private void validateAvailableStock(TransferOrderDto order, String itemCd, String lotNo, Double transferQty) {
        LocStockDto stock = inventoryMapper
                .selectLocStockForUpdate(order.plantCd(), order.fromLocationId(), itemCd, lotNo)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "출발 로케이션에 해당 품목/LOT 재고가 없습니다. 품목과 LOT 번호를 확인하세요."
                ));

        if (stock.availableQty() < transferQty) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "출발 로케이션의 가용 재고가 부족합니다. 현재 가용수량: " + stock.availableQty()
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
        String lotNo = normalizeLotNo(item.lotNo());
        double qty = item.transferQty();

        LocStockDto fromStock = inventoryMapper
                .selectLocStockForUpdate(order.plantCd(), order.fromLocationId(), item.itemCd(), lotNo)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "출발 로케이션에 해당 품목/LOT 재고가 없습니다. 품목과 LOT 번호를 확인하세요."
                ));

        if (fromStock.availableQty() < qty) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "출발 로케이션의 가용 재고가 부족합니다. 현재 가용수량: " + fromStock.availableQty()
            );
        }

        double fromBeforeQty = fromStock.stockQty();
        double fromAfterQty = fromBeforeQty - qty;
        inventoryMapper.updateLocStockQty(fromStock.locStockId(), -qty, userId);
        insertTransferHistory(order, fromStock.locStockId(), "TF_OUT", qty, fromBeforeQty, fromAfterQty, userId);

        LocStockDto toStock = inventoryMapper
                .selectLocStockForUpdate(order.plantCd(), order.toLocationId(), item.itemCd(), lotNo)
                .orElseGet(() -> createEmptyDestinationStock(order, item, lotNo, userId));

        double toBeforeQty = toStock.stockQty();
        double toAfterQty = toBeforeQty + qty;
        inventoryMapper.updateLocStockQty(toStock.locStockId(), qty, userId);
        insertTransferHistory(order, toStock.locStockId(), "TF_IN", qty, toBeforeQty, toAfterQty, userId);
        inventoryMapper.syncLocationUsage(order.fromLocationId(), userId);
        inventoryMapper.syncLocationUsage(order.toLocationId(), userId);

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
                order.plantCd(),
                order.toLocationId(),
                item.itemCd(),
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
                .selectLocStockForUpdate(order.plantCd(), order.toLocationId(), item.itemCd(), lotNo)
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
                order.plantCd(),
                locStockId,
                transType,
                qty,
                beforeQty,
                afterQty,
                "TF",
                order.transferNo(),
                order.transferId(),
                order.transferReason(),
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
        if (fromLocation.warehouseCd().equals(toLocation.warehouseCd())) {
            return;
        }

        int updated = inventoryMapper.updateMesWarehouseStockQty(
                order.plantCd(),
                fromLocation.warehouseCd(),
                item.itemCd(),
                lotNo,
                -qty,
                userId
        );
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        inventoryMapper.upsertMesWarehouseStockQty(
                order.plantCd(),
                toLocation.warehouseCd(),
                item.itemCd(),
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
                order.plantCd(),
                createMesTransNo(order.transferId(), item.transferItemId()),
                item.itemCd(),
                lotNo,
                qty,
                fromLocation.warehouseCd(),
                toLocation.warehouseCd(),
                fromLocation.locationCd(),
                toLocation.locationCd(),
                order.transferNo(),
                order.transferId(),
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
