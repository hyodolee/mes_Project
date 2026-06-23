package com.mes.mcs.infra.persistence.mybatis.mapper.zone;

import com.mes.mcs.domain.zone.dto.ZoneDto;
import com.mes.mcs.domain.zone.dto.ZoneSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ZoneMapper {
    List<ZoneDto> selectZoneList(ZoneSearchDto searchDto);
    long selectZoneCount(ZoneSearchDto searchDto);
    Optional<ZoneDto> selectZoneById(Long zoneId);
    Optional<ZoneDto> selectZoneByCd(@Param("warehouseCd") String warehouseCd, @Param("zoneCd") String zoneCd);
    void insertZone(ZoneDto zoneDto);
    void updateZone(ZoneDto zoneDto);
    void deleteZone(Long zoneId);
}
