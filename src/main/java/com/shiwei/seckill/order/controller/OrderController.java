package com.shiwei.seckill.order.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.order.model.OrderSubmitReq;
import com.shiwei.seckill.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;

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
    public ApiResponse<?> page(Long lastId, Integer size, String lastCreatedTime) {
        return ApiResponse.success(orderService.page(lastId, size, lastCreatedTime));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<?> detail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.detail(orderId));
    }

    @PostMapping("/submit")
    public ApiResponse<?> submit(@Valid @RequestBody OrderSubmitReq req) {
        return ApiResponse.successMessage("订单提交成功", orderService.submit(req));
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
