package com.shiwei.seckill.seckill.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.seckill.model.req.SeckillSubmitReq;
import com.shiwei.seckill.seckill.service.SeckillOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/seckill/order")
public class SeckillOrderController {
    @Resource
    private SeckillOrderService seckillOrderService;

    @PostMapping("/submit")
    public ApiResponse<?> submit(@RequestBody SeckillSubmitReq req) {
        return ApiResponse.success(seckillOrderService.submitSeckill(1L, req));
    }

    @GetMapping("/result")
    public ApiResponse<?> result(@RequestParam Long activityId) {
        return ApiResponse.success(seckillOrderService.queryResult(1L, activityId));
    }
}
