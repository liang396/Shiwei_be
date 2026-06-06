package com.shiwei.seckill.seckill.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.seckill.model.req.SeckillActivityAddReq;
import com.shiwei.seckill.seckill.model.req.SeckillActivityEditReq;
import com.shiwei.seckill.seckill.service.SeckillActivityService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/admin/seckill")
public class SeckillManageController {
    @Resource
    private SeckillActivityService seckillActivityService;

    @PostMapping("/activity/add")
    public ApiResponse<?> add(@RequestBody SeckillActivityAddReq req) {
        seckillActivityService.addActivity(req);
        return ApiResponse.successMessage("created", null);
    }

    @PostMapping("/activity/edit")
    public ApiResponse<?> edit(@RequestBody SeckillActivityEditReq req) {
        seckillActivityService.editActivity(req);
        return ApiResponse.successMessage("updated", null);
    }

    @PostMapping("/activity/publish")
    public ApiResponse<?> publish(@RequestParam Long activityId) {
        seckillActivityService.publishActivity(activityId);
        return ApiResponse.successMessage("published", null);
    }
}

