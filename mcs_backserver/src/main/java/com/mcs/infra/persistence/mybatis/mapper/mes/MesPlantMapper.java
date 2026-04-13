package com.mcs.infra.persistence.mybatis.mapper.mes;

import com.mcs.domain.mes.dto.PlantDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface MesPlantMapper {
    List<PlantDto> selectPlantList();
}
