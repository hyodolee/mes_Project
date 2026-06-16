package com.mes.mcs.infra.persistence.mybatis.mapper.route;

import com.mes.mcs.domain.route.dto.RouteEdgeDto;
import com.mes.mcs.domain.route.dto.RouteNodeDto;
import com.mes.mcs.domain.route.dto.RouteSearchDto;
import com.mes.mcs.domain.route.dto.RouteStepDto;
import com.mes.mcs.domain.route.dto.TransferRouteDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RouteMapper {
    List<RouteNodeDto> selectRouteNodes(RouteSearchDto searchDto);
    List<RouteEdgeDto> selectRouteEdges(RouteSearchDto searchDto);
    List<RouteEdgeDto> selectUsableRouteEdges(String plantCd);
    Optional<RouteNodeDto> selectRouteNodeByLocation(@Param("plantCd") String plantCd, @Param("locationId") Long locationId);

    Optional<TransferRouteDto> selectTransferRoute(Long transferId);
    List<RouteStepDto> selectTransferRouteSteps(Long transferRouteId);
    void deleteTransferRouteStepsByTransferId(Long transferId);
    void deleteTransferRouteByTransferId(Long transferId);
    void insertTransferRoute(TransferRouteDto routeDto);
    void insertTransferRouteStep(RouteStepDto stepDto);
    void updateTransferRouteStatus(
            @Param("transferId") Long transferId,
            @Param("routeStatus") String routeStatus,
            @Param("updUserId") String updUserId
    );

    void updateRouteEdgeStatus(
            @Param("routeEdgeId") Long routeEdgeId,
            @Param("edgeStatus") String edgeStatus,
            @Param("updUserId") String updUserId
    );
}
