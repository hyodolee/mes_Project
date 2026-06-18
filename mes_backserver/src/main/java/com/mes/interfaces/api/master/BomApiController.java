package com.mes.interfaces.api.master;

import com.mes.application.service.master.BomService;
import com.mes.domain.master.bom.dto.BomDto;
import com.mes.domain.master.bom.dto.BomSearchDto;
import com.mes.domain.master.bom.dto.BomUpsertRequest;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BOM(자재명세서) 관리 API.
 */
@RestController
@RequestMapping("/api/v1/master/boms")
public class BomApiController {

    private final BomService bomService;

    public BomApiController(BomService bomService) {
        this.bomService = bomService;
    }

    @GetMapping
    public ApiResponse<PageResponse<BomDto>> getBoms(BomSearchDto searchDto) {
        return ApiResponse.ok(bomService.getBomList(searchDto));
    }

    @GetMapping("/{bomId}")
    public ApiResponse<BomDto> getBom(@PathVariable("bomId") Long bomId) {
        return ApiResponse.ok(bomService.getBom(bomId));
    }

    @PostMapping
    public ApiResponse<Void> createBom(@Valid @RequestBody BomUpsertRequest request) {
        bomService.createBom(request);
        return ApiResponse.ok();
    }

    @PutMapping("/{bomId}")
    public ApiResponse<Void> updateBom(@PathVariable("bomId") Long bomId, @Valid @RequestBody BomUpsertRequest request) {
        bomService.updateBom(bomId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{bomId}")
    public ApiResponse<Void> deleteBom(@PathVariable("bomId") Long bomId) {
        bomService.deleteBom(bomId);
        return ApiResponse.ok();
    }
}
