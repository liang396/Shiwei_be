package com.shiwei.seckill.order.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.order.model.OrderDetailUpdateReq;
import com.shiwei.seckill.order.model.OrderSubmitReq;
import com.shiwei.seckill.order.service.OrderService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Collections;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Resource
    private OrderService orderService;

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.success(orderService.list());
    }

    @GetMapping("/page")
    public ApiResponse<?> page(Long lastId, Integer size, String lastCreatedTime, Integer status) {
        return ApiResponse.success(orderService.page(lastId, size, lastCreatedTime, status));
    }

    @GetMapping("/{orderKey}")
    public ApiResponse<?> detail(@PathVariable String orderKey) {
        if (orderKey != null && orderKey.startsWith("SW")) {
            return ApiResponse.success(orderService.detailByOrderNo(orderKey));
        }
        return ApiResponse.success(orderService.detail(Long.valueOf(orderKey)));
    }

    @PostMapping("/submit")
    public ApiResponse<?> submit(@Valid @RequestBody OrderSubmitReq req) {
        return ApiResponse.successMessage("订单提交成功", orderService.submit(req));
    }

    @PostMapping("/{orderKey}/detail-options")
    public ApiResponse<?> updateDetailOptions(@PathVariable String orderKey, @RequestBody OrderDetailUpdateReq req) {
        Long orderId;
        if (orderKey != null && orderKey.startsWith("SW")) {
            var order = orderService.getEntityByOrderNo(orderKey);
            if (order == null) {
                return ApiResponse.fail("订单不存在");
            }
            orderId = order.getId();
        } else {
            orderId = Long.valueOf(orderKey);
        }
        return ApiResponse.successMessage("订单详情已更新", orderService.updateDetailOptions(orderId, req));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<?> cancel(@PathVariable Long orderId) {
        return ApiResponse.successMessage("订单已取消", orderService.cancel(orderId));
    }

    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.success(Collections.singletonMap("total", orderService.list().size()));
    }
}
