package com.shiwei.seckill.admin.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("products", 0);
        data.put("activities", 0);
        data.put("orders", 0);
        data.put("users", 0);
        return ApiResponse.success(data);
    }
}

