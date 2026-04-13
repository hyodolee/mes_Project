package com.mes.application.service.inventory;

import com.mes.domain.inventory.stock.dto.StockDto;
import com.mes.domain.inventory.stock.dto.StockRequest;
import com.mes.domain.inventory.stock.dto.StockSearchDto;
import com.mes.domain.inventory.trans.dto.TransDto;
import com.mes.domain.inventory.trans.dto.TransRequest;
import com.mes.global.common.dto.PageResponse;
import com.mes.infra.persistence.mybatis.mapper.inventory.StockMapper;
import com.mes.infra.persistence.mybatis.mapper.inventory.TransMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private static final DateTimeFormatter TRANS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StockMapper stockMapper;
    private final TransMapper transMapper;

    public InventoryService(StockMapper stockMapper, TransMapper transMapper) {
        this.stockMapper = stockMapper;
        this.transMapper = transMapper;
    }

    public List<StockDto> getStocks(StockSearchDto searchDto) {
        return stockMapper.selectStocks(searchDto);
    }

    public PageResponse<StockDto> getStockPage(StockSearchDto searchDto) {
        int total = stockMapper.countStocks(searchDto);
        List<StockDto> content = stockMapper.selectStockList(searchDto);
        return PageResponse.createPagedResponse(content, total, searchDto);
    }

    public List<TransDto> getTransHistories(String plantCd, String itemCd, LocalDate fromDt, LocalDate toDt) {
        return transMapper.selectTransHistories(plantCd, itemCd, fromDt, toDt);
    }

    @Transactional
    public void processTransaction(TransRequest request) {
        // 1. Update Stock
        StockRequest stockRequest = StockRequest.builder()
                .plantCd(request.getPlantCd())
                .warehouseCd(
                        request.getTransType().equals("입고") ? request.getToWarehouseCd() : request.getFromWarehouseCd())
                .locationCd(
                        request.getTransType().equals("입고") ? request.getToLocationCd() : request.getFromLocationCd())
                .itemCd(request.getItemCd())
                .lotNo(request.getLotNo())
                .qty(request.getTransType().equals("입고") ? request.getTransQty() : request.getTransQty().negate())
                .unit(request.getUnit())
                .stockStatus("정상")
                .regUserId(request.getRegUserId())
                .build();

        stockMapper.upsertStock(stockRequest);

        // 2. Insert Transaction History
        String transNo = generateTransNo(LocalDate.now());
        transMapper.insertTransHistory(request, transNo, LocalDate.now());
    }

    private String generateTransNo(LocalDate transDt) {
        int count = transMapper.selectTransCountByDate(transDt);
        return "TR" + transDt.format(TRANS_DATE_FMT) + String.format("%04d", count + 1);
    }
}
