package com.mes.infra.persistence.mybatis.mapper.master;

import com.mes.domain.master.bom.dto.BomDto;
import com.mes.domain.master.bom.dto.BomSearchDto;
import com.mes.domain.master.bom.dto.BomUpsertRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BomMapper {
    List<BomDto> selectBomList(BomSearchDto searchDto);

    int countBoms(BomSearchDto searchDto);

    BomDto getBom(@Param("bomId") Long bomId);

    void save(BomUpsertRequest request);

    void update(BomUpsertRequest request);

    void deleteBom(@Param("bomId") Long bomId);
}
