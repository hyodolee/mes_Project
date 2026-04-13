package com.mcs.infra.persistence.mybatis.mapper.inbound;

import com.mcs.domain.inbound.dto.InboundItemDto;
import com.mcs.domain.inbound.dto.InboundOrderDto;
import com.mcs.domain.inbound.dto.InboundSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface InboundMapper {
    // Order
    List<InboundOrderDto> selectInboundList(InboundSearchDto searchDto);
    long selectInboundCount(InboundSearchDto searchDto);
    Optional<InboundOrderDto> selectInboundById(Long inboundId);
    Optional<InboundOrderDto> selectInboundByNo(String inboundNo);
    void insertInboundOrder(InboundOrderDto orderDto);
    void updateInboundOrder(InboundOrderDto orderDto);
    void updateInboundStatus(@Param("inboundId") Long inboundId, @Param("status") String status, @Param("updUserId") String updUserId);
    void deleteInboundOrder(Long inboundId);
    
    // Item
    List<InboundItemDto> selectInboundItems(Long inboundId);
    void insertInboundItem(InboundItemDto itemDto);
    void updateInboundItem(InboundItemDto itemDto);
    void updateInboundItemStatus(@Param("inboundItemId") Long inboundItemId, @Param("status") String status, @Param("actualQty") Double actualQty, @Param("locationId") Long locationId, @Param("updUserId") String updUserId);
    void deleteInboundItems(Long inboundId);
}
