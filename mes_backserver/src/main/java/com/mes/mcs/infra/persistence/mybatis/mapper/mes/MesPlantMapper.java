package com.mes.mcs.infra.persistence.mybatis.mapper.mes;

import com.mes.mcs.domain.mes.dto.PlantDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface MesPlantMapper {
    List<PlantDto> selectPlantList();
}
