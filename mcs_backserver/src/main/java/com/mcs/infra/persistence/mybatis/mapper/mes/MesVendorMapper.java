package com.mcs.infra.persistence.mybatis.mapper.mes;

import com.mcs.domain.mes.dto.VendorDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MesVendorMapper {
    List<VendorDto> selectVendorList(
            @Param("vendorCd") String vendorCd,
            @Param("vendorNm") String vendorNm,
            @Param("useYn") String useYn
    );
}
