package com.mcs.interfaces.api.reference;

import com.mcs.domain.mes.dto.ComCodeDto;
import com.mcs.domain.mes.dto.ItemDto;
import com.mcs.domain.mes.dto.PlantDto;
import com.mcs.domain.mes.dto.VendorDto;
import com.mcs.domain.mes.dto.WarehouseDto;
import com.mcs.global.response.ApiResponse;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesComCodeMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesItemMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesPlantMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesVendorMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/references")
@RequiredArgsConstructor
public class ReferenceApiController {

    private final MesPlantMapper mesPlantMapper;
    private final MesWarehouseMapper mesWarehouseMapper;
    private final MesComCodeMapper mesComCodeMapper;
    private final MesVendorMapper mesVendorMapper;
    private final MesItemMapper mesItemMapper;

    @GetMapping("/plants")
    public ApiResponse<List<PlantDto>> getPlants() {
        return ApiResponse.ok(mesPlantMapper.selectPlantList());
    }

    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseDto>> getWarehouses(
            @RequestParam(required = false) String plantCd,
            @RequestParam(required = false) String warehouseCd,
            @RequestParam(defaultValue = "Y") String useYn
    ) {
        return ApiResponse.ok(mesWarehouseMapper.selectWarehouseList(plantCd, warehouseCd, useYn));
    }

    @GetMapping("/vendors")
    public ApiResponse<List<VendorDto>> getVendors(
            @RequestParam(required = false) String vendorCd,
            @RequestParam(required = false) String vendorNm,
            @RequestParam(defaultValue = "Y") String useYn
    ) {
        return ApiResponse.ok(mesVendorMapper.selectVendorList(vendorCd, vendorNm, useYn));
    }

    @GetMapping("/items")
    public ApiResponse<List<ItemDto>> getItems(
            @RequestParam(required = false) String plantCd,
            @RequestParam(required = false) String itemCd,
            @RequestParam(required = false) String itemNm,
            @RequestParam(required = false) String itemType
    ) {
        return ApiResponse.ok(mesItemMapper.selectItemList(plantCd, itemCd, itemNm, itemType));
    }

    @GetMapping("/codes/{grpCd}")
    public ApiResponse<List<ComCodeDto>> getCodes(
            @PathVariable String grpCd,
            @RequestParam(defaultValue = "Y") String useYn
    ) {
        return ApiResponse.ok(mesComCodeMapper.selectComCodeList(grpCd, useYn));
    }
}
