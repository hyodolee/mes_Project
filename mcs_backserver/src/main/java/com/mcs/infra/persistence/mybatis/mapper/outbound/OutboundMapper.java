package com.mcs.infra.persistence.mybatis.mapper.outbound;

import com.mcs.domain.outbound.dto.OutboundItemDto;
import com.mcs.domain.outbound.dto.OutboundOrderDto;
import com.mcs.domain.outbound.dto.OutboundSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OutboundMapper {
    // Order
    List<OutboundOrderDto> selectOutboundList(OutboundSearchDto searchDto);
    long selectOutboundCount(OutboundSearchDto searchDto);
    Optional<OutboundOrderDto> selectOutboundById(Long outboundId);
    void insertOutboundOrder(OutboundOrderDto orderDto);
    void updateOutboundOrder(OutboundOrderDto orderDto);
    void updateOutboundStatus(@Param("outboundId") Long outboundId, @Param("status") String status, @Param("updUserId") String updUserId);
    void deleteOutboundOrder(Long outboundId);
    
    // Item
    List<OutboundItemDto> selectOutboundItems(Long outboundId);
    void insertOutboundItem(OutboundItemDto itemDto);
    void updateOutboundItem(OutboundItemDto itemDto);
    void updateOutboundItemStatus(@Param("outboundItemId") Long outboundItemId, @Param("status") String status, @Param("updUserId") String updUserId);
    void deleteOutboundItems(Long outboundId);
}
