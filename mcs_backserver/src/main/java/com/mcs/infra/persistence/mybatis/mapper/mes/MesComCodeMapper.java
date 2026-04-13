package com.mcs.infra.persistence.mybatis.mapper.mes;

import com.mcs.domain.mes.dto.ComCodeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MesComCodeMapper {
    List<ComCodeDto> selectComCodeList(
        @Param("grpCd") String grpCd,
        @Param("useYn") String useYn
    );
}
