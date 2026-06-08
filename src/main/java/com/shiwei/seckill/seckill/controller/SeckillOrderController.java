package com.shiwei.seckill.seckill.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.seckill.model.req.SeckillSubmitReq;
import com.shiwei.seckill.seckill.service.SeckillOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/seckill/order")
public class SeckillOrderController {
    private static final long DEFAULT_USER_ID = 1L;

    @Resource
    private SeckillOrderService seckillOrderService;

    @PostMapping("/submit")
    public ApiResponse<?> submit(
        @RequestHeader(value = "X-User-Id", required = false) Long userId,
        @RequestBody SeckillSubmitReq req
    ) {
        return ApiResponse.success(seckillOrderService.submitSeckill(resolveUserId(userId), req));
    }

    @GetMapping("/result")
    public ApiResponse<?> result(
        @RequestHeader(value = "X-User-Id", required = false) Long userId,
        @RequestParam Long activityId
    ) {
        return ApiResponse.success(seckillOrderService.queryResult(resolveUserId(userId), activityId));
    }

    long resolveUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}

