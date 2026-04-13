package com.mcs.application.service.outbound;

import com.mcs.domain.outbound.dto.OutboundItemDto;
import com.mcs.domain.outbound.dto.OutboundOrderDto;
import com.mcs.domain.outbound.dto.OutboundSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.outbound.OutboundMapper;
import com.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundService {

    private final OutboundMapper outboundMapper;
    private final InventoryMapper inventoryMapper; // 출고 확정 시 재고 차감을 위해 사용

    public PageResponse<OutboundOrderDto> getOutboundList(OutboundSearchDto searchDto) {
        List<OutboundOrderDto> list = outboundMapper.selectOutboundList(searchDto);
        long total = outboundMapper.selectOutboundCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    public OutboundOrderDto getOutboundOrder(Long outboundId) {
        return outboundMapper.selectOutboundById(outboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    public List<OutboundItemDto> getOutboundItems(Long outboundId) {
        return outboundMapper.selectOutboundItems(outboundId);
    }

    @Transactional
    public Long createOutboundOrder(OutboundOrderDto orderDto) {
        outboundMapper.insertOutboundOrder(orderDto);
        return orderDto.outboundId();
    }

    @Transactional
    public void updateOutboundOrder(OutboundOrderDto orderDto) {
        OutboundOrderDto existing = getOutboundOrder(orderDto.outboundId());
        if (!"REQUESTED".equals(existing.outboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT); // "요청" 상태에서만 정보 수정 가능
        }
        outboundMapper.updateOutboundOrder(orderDto);
    }

    @Transactional
    public void deleteOutboundOrder(Long outboundId) {
        OutboundOrderDto existing = getOutboundOrder(outboundId);
        if (!"REQUESTED".equals(existing.outboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        outboundMapper.deleteOutboundItems(outboundId);
        outboundMapper.deleteOutboundOrder(outboundId);
    }

    @Transactional
    public void changeOrderStatus(Long outboundId, String newStatus, String userId) {
        OutboundOrderDto order = getOutboundOrder(outboundId);
        String currentStatus = order.outboundStatus();

        if ("SHIPPED".equals(newStatus) && !"SHIPPED".equals(currentStatus)) {
            // 출고 확정 처리 로직: 아이템 재고 차감
            List<OutboundItemDto> items = getOutboundItems(outboundId);
            for (OutboundItemDto item : items) {
                // 실제 프로젝트에서는 picking된 수량(pickedQty) 등을 기반으로 차감
                // 여기서는 예시로 상태만 업데이트 (InventoryMapper 연동 뼈대 구현 필요 시 추가)
                outboundMapper.updateOutboundItemStatus(item.outboundItemId(), "SHIPPED", userId);
            }
        }
        
        outboundMapper.updateOutboundStatus(outboundId, newStatus, userId);
    }
}
