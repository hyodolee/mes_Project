package com.mes.mcs.interfaces.api.reference;

import com.mes.mcs.domain.mes.dto.ComCodeDto;
import com.mes.mcs.domain.mes.dto.ItemDto;
import com.mes.mcs.domain.mes.dto.PlantDto;
import com.mes.mcs.domain.mes.dto.VendorDto;
import com.mes.mcs.domain.mes.dto.WarehouseDto;
import com.mes.global.response.ApiResponse;
import com.mes.mcs.infra.persistence.mybatis.mapper.mes.MesComCodeMapper;
import com.mes.mcs.infra.persistence.mybatis.mapper.mes.MesItemMapper;
import com.mes.mcs.infra.persistence.mybatis.mapper.mes.MesPlantMapper;
import com.mes.mcs.infra.persistence.mybatis.mapper.mes.MesVendorMapper;
import com.mes.mcs.infra.persistence.mybatis.mapper.mes.MesWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("mcsReferenceApiController")
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
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "warehouseCd", required = false) String warehouseCd,
            @RequestParam(name = "useYn", defaultValue = "Y") String useYn
    ) {
        return ApiResponse.ok(mesWarehouseMapper.selectWarehouseList(plantCd, warehouseCd, useYn));
    }

    @GetMapping("/vendors")
    public ApiResponse<List<VendorDto>> getVendors(
            @RequestParam(name = "vendorCd", required = false) String vendorCd,
            @RequestParam(name = "vendorNm", required = false) String vendorNm,
            @RequestParam(name = "useYn", defaultValue = "Y") String useYn
    ) {
        return ApiResponse.ok(mesVendorMapper.selectVendorList(vendorCd, vendorNm, useYn));
    }

    @GetMapping("/items")
    public ApiResponse<List<ItemDto>> getItems(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "itemCd", required = false) String itemCd,
            @RequestParam(name = "itemNm", required = false) String itemNm,
            @RequestParam(name = "itemType", required = false) String itemType
    ) {
        return ApiResponse.ok(mesItemMapper.selectItemList(plantCd, itemCd, itemNm, itemType));
    }

    @GetMapping("/codes/{grpCd}")
    public ApiResponse<List<ComCodeDto>> getCodes(
            @PathVariable("grpCd") String grpCd,
            @RequestParam(name = "useYn", defaultValue = "Y") String useYn
    ) {
        return ApiResponse.ok(mesComCodeMapper.selectComCodeList(grpCd, useYn));
    }
}
