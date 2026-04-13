package com.mcs.infra.persistence.mybatis.mapper.inventory;

import com.mcs.domain.inventory.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface InventoryMapper {
    // 로케이션 재고
    List<LocStockDto> selectLocStockList(LocStockSearchDto searchDto);
    long selectLocStockCount(LocStockSearchDto searchDto);
    Optional<LocStockDto> selectLocStockById(Long locStockId);
    
    // 재고 갱신 (조정, 입출고 등에서 사용)
    void updateLocStockQty(
        @Param("locStockId") Long locStockId,
        @Param("adjustQty") Double adjustQty, // +/- 값
        @Param("updUserId") String updUserId
    );

    // 재고 트랜잭션 이력
    List<LocTransHisDto> selectLocTransHisList(LocTransHisSearchDto searchDto);
    long selectLocTransHisCount(LocTransHisSearchDto searchDto);
    void insertLocTransHis(LocTransHisDto hisDto);
}
