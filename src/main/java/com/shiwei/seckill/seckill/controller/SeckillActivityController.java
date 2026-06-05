package com.shiwei.seckill.seckill.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.seckill.service.SeckillActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/seckill/activity")
public class SeckillActivityController {
    @Resource
    private SeckillActivityService seckillActivityService;

    @GetMapping("/list")
    public ApiResponse<?> list() {
        return ApiResponse.success(seckillActivityService.listActivities());
    }

    @GetMapping("/{activityId}")
    public ApiResponse<?> detail(@PathVariable Long activityId) {
        return ApiResponse.success(seckillActivityService.getActivityDetail(activityId));
    }
}
