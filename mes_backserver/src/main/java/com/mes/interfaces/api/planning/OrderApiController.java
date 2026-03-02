package com.mes.interfaces.api.planning;

import com.mes.application.service.planning.OrderService;
import com.mes.domain.planning.order.dto.OrderDto;
import com.mes.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planning/orders")
public class OrderApiController {

    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/search")
    public ApiResponse<List<OrderDto>> searchOrders(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(orderService.searchOrders(keyword));
    }
}
