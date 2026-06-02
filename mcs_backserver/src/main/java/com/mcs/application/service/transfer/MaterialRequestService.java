package com.mcs.application.service.transfer;

import com.mcs.application.service.route.RouteService;
import com.mcs.domain.inventory.dto.LocStockDto;
import com.mcs.domain.location.dto.LocationDto;
import com.mcs.domain.route.dto.RouteOptimizeRequest;
import com.mcs.domain.route.dto.RouteOptimizeResultDto;
import com.mcs.domain.transfer.dto.MaterialRequestDto;
import com.mcs.domain.transfer.dto.MaterialRequestResultDto;
import com.mcs.domain.transfer.dto.TransferItemDto;
import com.mcs.domain.transfer.dto.TransferOrderDto;
import com.mcs.domain.transfer.dto.TransferSearchDto;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import com.mcs.infra.persistence.mybatis.mapper.location.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialRequestService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final InventoryMapper inventoryMapper;
    private final LocationMapper locationMapper;
    private final RouteService routeService;
    private final TransferService transferService;

    @Transactional
    public MaterialRequestResultDto createMaterialRequest(MaterialRequestDto request) {
        validateRequest(request);

        String optimizeRule = normalizeOptimizeRule(request.optimizeRule());
        LocStockDto sourceStock = selectSourceStock(request);
        LocationDto destination = selectDestination(request.plantCd(), sourceStock.locationId(), optimizeRule);

        String transferNo = createTransferNo(request.woId());
        Long transferId = transferService.createTransferOrder(new TransferOrderDto(
                null,
                request.plantCd(),
                transferNo,
                "REQUESTED",
                sourceStock.locationId(),
                destination.locationId(),
                createTransferReason(request),
                SYSTEM_USER,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                optimizeRule
        ));

        transferService.createTransferItem(transferId, new TransferItemDto(
                null,
                transferId,
                request.itemCd(),
                normalizeLotNo(sourceStock.lotNo()),
                request.requestQty(),
                "REQUESTED",
                SYSTEM_USER,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        return new MaterialRequestResultDto(
                transferId,
                transferNo,
                sourceStock.locationId(),
                sourceStock.locationCd(),
                destination.locationId(),
                destination.locationCd(),
                request.itemCd(),
                normalizeLotNo(sourceStock.lotNo()),
                request.requestQty(),
                optimizeRule
        );
    }

    @Transactional
    public int cancelMaterialRequestsByWorkOrder(Long woId) {
        if (woId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        TransferSearchDto searchDto = new TransferSearchDto();
        searchDto.setTransferNo("MES-" + woId + "-");
        searchDto.setSize(100);

        int cancelledCount = 0;
        for (TransferOrderDto transfer : transferService.getTransferList(searchDto).getContent()) {
            if ("CANCELLED".equals(transfer.transferStatus())) {
                continue;
            }
            if ("COMPLETED".equals(transfer.transferStatus())) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "MCS 자재 이동이 이미 완료되어 작업오더만 취소할 수 없습니다. 재고 복구 또는 반품 이동을 먼저 처리하세요. 이동번호="
                                + transfer.transferNo()
                );
            }
            transferService.changeOrderStatus(transfer.transferId(), "CANCELLED", SYSTEM_USER);
            cancelledCount++;
        }
        return cancelledCount;
    }

    private void validateRequest(MaterialRequestDto request) {
        if (request.plantCd() == null || request.plantCd().isBlank()
                || request.itemCd() == null || request.itemCd().isBlank()
                || request.requestQty() == null || request.requestQty() <= 0
                || request.woId() == null
                || request.woNo() == null || request.woNo().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private LocStockDto selectSourceStock(MaterialRequestDto request) {
        List<LocStockDto> stocks = inventoryMapper.selectAvailableLocStocksForMaterialRequest(
                request.plantCd(),
                request.itemCd(),
                request.requestQty()
        );
        if (stocks.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "MCS에서 자동 배정 가능한 가용 재고가 없습니다. 품목=" + request.itemCd()
            );
        }
        return stocks.get(0);
    }

    private LocationDto selectDestination(String plantCd, Long sourceLocationId, String optimizeRule) {
        List<LocationDto> candidates = locationMapper.selectAutoTransferDestinations(plantCd, sourceLocationId);
        for (LocationDto candidate : candidates) {
            try {
                RouteOptimizeResultDto result = routeService.optimize(new RouteOptimizeRequest(
                        plantCd,
                        sourceLocationId,
                        candidate.locationId(),
                        optimizeRule
                ));
                if (Boolean.TRUE.equals(result.routeAvailable())) {
                    return candidate;
                }
            } catch (BusinessException ignored) {
                // Try next destination candidate.
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "MCS에서 자동 배정 가능한 도착 로케이션/경로를 찾을 수 없습니다.");
    }

    private String createTransferNo(Long woId) {
        long timePart = System.currentTimeMillis() % 1_000_000_000_000L;
        return "MES-" + woId + "-" + timePart;
    }

    private String createTransferReason(MaterialRequestDto request) {
        String reason = request.requestReason();
        if (reason == null || reason.isBlank()) {
            reason = "MES 작업오더 자재 요청";
        }
        return reason + " / WO=" + request.woNo() + " / WC=" + defaultText(request.workcenterCd());
    }

    private String normalizeOptimizeRule(String optimizeRule) {
        return optimizeRule == null || optimizeRule.isBlank() ? "AVOID_CONGESTION" : optimizeRule;
    }

    private String normalizeLotNo(String lotNo) {
        return lotNo == null ? "" : lotNo.trim();
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
