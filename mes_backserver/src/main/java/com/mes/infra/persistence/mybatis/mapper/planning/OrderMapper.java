package com.mes.infra.persistence.mybatis.mapper.planning;

import com.mes.domain.planning.order.dto.OrderDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 임시로 생산계획(PLN_PROD_PLAN) 테이블의 데이터를 수주 데이터인 것처럼 조회하여 사용합니다.
     * 실제로는 SCM이나 영업 모듈의 수주(Sales Order) 테이블을 조회해야 합니다.
     */
    List<OrderDto> findMockOrders(@Param("keyword") String keyword);
}
