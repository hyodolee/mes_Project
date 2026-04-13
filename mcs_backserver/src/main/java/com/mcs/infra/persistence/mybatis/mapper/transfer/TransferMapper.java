package com.mcs.infra.persistence.mybatis.mapper.transfer;

import com.mcs.domain.transfer.dto.TransferItemDto;
import com.mcs.domain.transfer.dto.TransferOrderDto;
import com.mcs.domain.transfer.dto.TransferSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TransferMapper {
    // Order
    List<TransferOrderDto> selectTransferList(TransferSearchDto searchDto);
    long selectTransferCount(TransferSearchDto searchDto);
    Optional<TransferOrderDto> selectTransferById(Long transferId);
    void insertTransferOrder(TransferOrderDto orderDto);
    void updateTransferOrder(TransferOrderDto orderDto);
    void updateTransferStatus(@Param("transferId") Long transferId, @Param("status") String status, @Param("updUserId") String updUserId);
    void deleteTransferOrder(Long transferId);
    
    // Item
    List<TransferItemDto> selectTransferItems(Long transferId);
    void insertTransferItem(TransferItemDto itemDto);
    void updateTransferItem(TransferItemDto itemDto);
    void updateTransferItemStatus(@Param("transferItemId") Long transferItemId, @Param("status") String status, @Param("updUserId") String updUserId);
    void deleteTransferItems(Long transferId);
}
