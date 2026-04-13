package com.mes.infra.persistence.mybatis.mapper.planning;

import com.mes.domain.planning.workorder.dto.WorkOrderCreateRequest;
import com.mes.domain.planning.workorder.dto.WorkOrderDto;
import com.mes.domain.planning.workorder.dto.WorkOrderSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorkOrderMapper {
    List<WorkOrderDto> selectWorkOrders(
            @Param("plantCd") String plantCd,
            @Param("itemCd") String itemCd,
            @Param("woStatus") String woStatus,
            @Param("woFromDt") LocalDate woFromDt,
            @Param("woToDt") LocalDate woToDt
    );

    WorkOrderDto selectWorkOrderById(@Param("woId") Long woId);

    int insertWorkOrder(@Param("request") WorkOrderCreateRequest request,
                        @Param("woNo") String woNo,
                        @Param("regUserId") String regUserId);

    int updateWorkOrderStatus(@Param("woId") Long woId,
                              @Param("woStatus") String woStatus,
                              @Param("actualStartDtm") LocalDateTime actualStartDtm,
                              @Param("actualEndDtm") LocalDateTime actualEndDtm,
                              @Param("updUserId") String updUserId);

    int updateWorkOrderQty(@Param("woId") Long woId,
                          @Param("goodQty") java.math.BigDecimal goodQty,
                          @Param("defectQty") java.math.BigDecimal defectQty,
                          @Param("updUserId") String updUserId);

    int selectWorkOrderCountByDate(@Param("woDt") LocalDate woDt);

    List<WorkOrderDto> selectWorkOrderList(WorkOrderSearchDto searchDto);

    int countWorkOrders(WorkOrderSearchDto searchDto);
}
