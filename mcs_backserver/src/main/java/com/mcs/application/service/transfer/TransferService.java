package com.mcs.application.service.transfer;

import com.mcs.domain.transfer.dto.TransferItemDto;
import com.mcs.domain.transfer.dto.TransferOrderDto;
import com.mcs.domain.transfer.dto.TransferSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.transfer.TransferMapper;
import com.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

    private final TransferMapper transferMapper;
    private final InventoryMapper inventoryMapper; // 이동 확정 시 양쪽 재고 반영을 위해 사용

    public PageResponse<TransferOrderDto> getTransferList(TransferSearchDto searchDto) {
        List<TransferOrderDto> list = transferMapper.selectTransferList(searchDto);
        long total = transferMapper.selectTransferCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    public TransferOrderDto getTransferOrder(Long transferId) {
        return transferMapper.selectTransferById(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    public List<TransferItemDto> getTransferItems(Long transferId) {
        return transferMapper.selectTransferItems(transferId);
    }

    @Transactional
    public Long createTransferOrder(TransferOrderDto orderDto) {
        transferMapper.insertTransferOrder(orderDto);
        return orderDto.transferId();
    }

    @Transactional
    public void updateTransferOrder(TransferOrderDto orderDto) {
        TransferOrderDto existing = getTransferOrder(orderDto.transferId());
        if (!"REQUESTED".equals(existing.transferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT); // "요청" 상태에서만 정보 수정 가능
        }
        transferMapper.updateTransferOrder(orderDto);
    }

    @Transactional
    public void deleteTransferOrder(Long transferId) {
        TransferOrderDto existing = getTransferOrder(transferId);
        if (!"REQUESTED".equals(existing.transferStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        transferMapper.deleteTransferItems(transferId);
        transferMapper.deleteTransferOrder(transferId);
    }

    @Transactional
    public void changeOrderStatus(Long transferId, String newStatus, String userId) {
        TransferOrderDto order = getTransferOrder(transferId);
        String currentStatus = order.transferStatus();

        if ("COMPLETED".equals(newStatus) && !"COMPLETED".equals(currentStatus)) {
            // 이동 완료 처리 로직: 출발지 차감 & 도착지 증가
            List<TransferItemDto> items = getTransferItems(transferId);
            for (TransferItemDto item : items) {
                // 실제 프로젝트에서는 출발지/도착지 로케이션을 기반으로 재고를 이동시킴
                // 여기서는 예시로 상태만 업데이트 (InventoryMapper 연동 뼈대 구현 필요 시 추가)
                transferMapper.updateTransferItemStatus(item.transferItemId(), "MOVED", userId);
            }
        }
        
        transferMapper.updateTransferStatus(transferId, newStatus, userId);
    }
}
