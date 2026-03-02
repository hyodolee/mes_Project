package com.mes.infra.persistence.mybatis.mapper.master;

import com.mes.domain.master.plant.dto.PlantDto;
import com.mes.domain.master.plant.dto.PlantUpsertRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlantMapper {
    List<PlantDto> selectPlants(@Param("companyCd") String companyCd, @Param("plantNm") String plantNm, @Param("useYn") String useYn);

    PlantDto selectPlantById(@Param("plantCd") String plantCd);

    int insertPlant(@Param("request") PlantUpsertRequest request, @Param("regUserId") String regUserId);

    int updatePlant(@Param("request") PlantUpsertRequest request, @Param("updUserId") String updUserId);
}
