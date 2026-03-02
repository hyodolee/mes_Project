package com.mes.application.service.master;

import com.mes.domain.master.plant.dto.PlantDto;
import com.mes.domain.master.plant.dto.PlantUpsertRequest;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.master.PlantMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlantService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final PlantMapper plantMapper;

    public PlantService(PlantMapper plantMapper) {
        this.plantMapper = plantMapper;
    }

    public List<PlantDto> getPlants(String companyCd, String plantNm, String useYn) {
        return plantMapper.selectPlants(companyCd, plantNm, useYn);
    }

    public PlantDto getPlant(String plantCd) {
        PlantDto plant = plantMapper.selectPlantById(plantCd);
        if (plant == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "공장을 찾을 수 없습니다. plantCd=" + plantCd);
        }
        return plant;
    }

    public void createPlant(PlantUpsertRequest request) {
        int inserted = plantMapper.insertPlant(request, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "공장 등록에 실패했습니다.");
        }
    }

    public void updatePlant(PlantUpsertRequest request) {
        int updated = plantMapper.updatePlant(request, SYSTEM_USER);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "공장 수정에 실패했습니다.");
        }
    }
}
