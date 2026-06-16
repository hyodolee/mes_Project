package com.mes.mcs.infra.persistence.mybatis.mapper.inventory;

import com.mes.mcs.domain.inventory.dto.*;
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
    Optional<LocStockDto> selectLocStockForUpdate(
        @Param("plantCd") String plantCd,
        @Param("locationId") Long locationId,
        @Param("itemCd") String itemCd,
        @Param("lotNo") String lotNo
    );
    List<LocStockDto> selectAvailableLocStocksForMaterialRequest(
        @Param("plantCd") String plantCd,
        @Param("itemCd") String itemCd,
        @Param("requestQty") Double requestQty
    );
    void insertLocStock(LocStockDto locStockDto);
    
    // 재고 갱신 (조정, 입출고 등에서 사용)
    void updateLocStockQty(
        @Param("locStockId") Long locStockId,
        @Param("adjustQty") Double adjustQty, // +/- 값
        @Param("updUserId") String updUserId
    );

    // 재고 트랜잭션 이력
    List<LocTransHisDto> selectLocTransHisList(LocTransHisSearchDto searchDto);
    long selectLocTransHisCount(LocTransHisSearchDto searchDto);
    List<LocTransHisDto> selectLocTransHisByRef(
        @Param("refType") String refType,
        @Param("refNo") String refNo,
        @Param("refId") Long refId
    );
    void insertLocTransHis(LocTransHisDto hisDto);

    void syncLocationUsage(
        @Param("locationId") Long locationId,
        @Param("updUserId") String updUserId
    );

    int updateMesWarehouseStockQty(
        @Param("plantCd") String plantCd,
        @Param("warehouseCd") String warehouseCd,
        @Param("itemCd") String itemCd,
        @Param("lotNo") String lotNo,
        @Param("adjustQty") Double adjustQty,
        @Param("updUserId") String updUserId
    );

    void upsertMesWarehouseStockQty(
        @Param("plantCd") String plantCd,
        @Param("warehouseCd") String warehouseCd,
        @Param("itemCd") String itemCd,
        @Param("lotNo") String lotNo,
        @Param("adjustQty") Double adjustQty,
        @Param("regUserId") String regUserId
    );

    void insertMesTransferHistory(
        @Param("plantCd") String plantCd,
        @Param("transNo") String transNo,
        @Param("itemCd") String itemCd,
        @Param("lotNo") String lotNo,
        @Param("transQty") Double transQty,
        @Param("fromWarehouseCd") String fromWarehouseCd,
        @Param("toWarehouseCd") String toWarehouseCd,
        @Param("fromLocationCd") String fromLocationCd,
        @Param("toLocationCd") String toLocationCd,
        @Param("refNo") String refNo,
        @Param("refId") Long refId,
        @Param("transUserId") String transUserId,
        @Param("regUserId") String regUserId
    );
}
