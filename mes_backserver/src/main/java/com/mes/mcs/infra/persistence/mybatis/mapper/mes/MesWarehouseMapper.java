package com.mes.mcs.infra.persistence.mybatis.mapper.mes;

import com.mes.mcs.domain.mes.dto.WarehouseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MesWarehouseMapper {
    List<WarehouseDto> selectWarehouseList(
        @Param("plantCd") String plantCd,
        @Param("warehouseCd") String warehouseCd,
        @Param("useYn") String useYn
    );
}
