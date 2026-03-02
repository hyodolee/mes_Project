package com.mes.application.service.planning;

import com.mes.domain.planning.order.dto.OrderDto;
import com.mes.infra.persistence.mybatis.mapper.planning.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public List<OrderDto> searchOrders(String keyword) {
        return orderMapper.findMockOrders(keyword);
    }
}
