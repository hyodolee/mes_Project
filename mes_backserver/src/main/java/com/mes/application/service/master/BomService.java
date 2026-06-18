package com.mes.application.service.master;

import com.mes.domain.master.bom.dto.BomDto;
import com.mes.domain.master.bom.dto.BomSearchDto;
import com.mes.domain.master.bom.dto.BomUpsertRequest;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.master.BomMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BomService {

    private final BomMapper bomMapper;

    public BomService(BomMapper bomMapper) {
        this.bomMapper = bomMapper;
    }

    public PageResponse<BomDto> getBomList(BomSearchDto searchDto) {
        return PageResponse.createPagedResponse(
                bomMapper.selectBomList(searchDto),
                bomMapper.countBoms(searchDto),
                searchDto
        );
    }

    public BomDto getBom(Long bomId) {
        BomDto bom = bomMapper.getBom(bomId);
        if (bom == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "해당 BOM을 찾을 수 없습니다. ID: " + bomId);
        }
        return bom;
    }

    @Transactional
    public void createBom(BomUpsertRequest request) {
        bomMapper.save(request);
    }

    @Transactional
    public void updateBom(Long bomId, BomUpsertRequest request) {
        getBom(bomId); // 존재 확인
        request.setBomId(bomId);
        bomMapper.update(request);
    }

    @Transactional
    public void deleteBom(Long bomId) {
        getBom(bomId); // 존재 확인
        bomMapper.deleteBom(bomId);
    }
}
