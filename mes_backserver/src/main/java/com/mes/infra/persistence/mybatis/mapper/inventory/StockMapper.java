package com.mes.infra.persistence.mybatis.mapper.inventory;

import com.mes.domain.inventory.stock.dto.StockDto;
import com.mes.domain.inventory.stock.dto.StockRequest;
import com.mes.domain.inventory.stock.dto.StockSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StockMapper {
    List<StockDto> selectStocks(StockSearchDto searchDto);

    List<StockDto> selectStockList(StockSearchDto searchDto);

    int countStocks(StockSearchDto searchDto);

    int upsertStock(@Param("request") StockRequest request);

    int updateReservedQty(@Param("stockId") Long stockId, @Param("qty") java.math.BigDecimal qty);
}
