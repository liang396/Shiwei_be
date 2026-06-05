package com.shiwei.seckill.profile.controller;

import com.shiwei.seckill.address.service.AddressService;
import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.order.model.OrderRecord;
import com.shiwei.seckill.order.service.OrderService;
import com.shiwei.seckill.promotion.service.CouponService;
import com.shiwei.seckill.profile.model.UserProfileSaveReq;
import com.shiwei.seckill.profile.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    @Resource
    private OrderService orderService;

    @Resource
    private AddressService addressService;

    @Resource
    private UserProfileService userProfileService;

    @Resource
    private CouponService couponService;

    @GetMapping("/overview")
    public ApiResponse<?> overview() {
        List<OrderRecord> orders = orderService.list();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", buildUser());
        data.put("orderStats", buildOrderStats(orders));
        data.put("recentOrders", orders.stream().limit(4).collect(Collectors.toList()));
        data.put("addresses", addressService.list());
        data.put("coupons", couponService.listByUser(1L));
        data.put("reviews", buildReviews());
        return ApiResponse.success(data);
    }

    @PostMapping("/user/save")
    public ApiResponse<?> saveUser(@RequestBody UserProfileSaveReq req) {
        return ApiResponse.successMessage("个人信息保存成功", userProfileService.save(req));
    }

    private Object buildUser() {
        return userProfileService.currentProfile();
    }

    private Map<String, Object> buildOrderStats(List<OrderRecord> orders) {
        long pending = orders.stream().filter(order -> "待支付".equals(order.getOrderStatus())).count();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pending", pending);
        map.put("total", orders.size());
        return map;
    }

    private List<Map<String, Object>> buildReviews() {
        return Arrays.asList(
            review(1L, "白色连衣裙", "面料柔和，尺码合适，发货也快。", 5),
            review(2L, "浅蓝针织衫", "颜色和页面一致，适合春秋搭配。", 4)
        );
    }

    private Map<String, Object> review(Long reviewId, String productName, String content, int score) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reviewId", reviewId);
        map.put("productName", productName);
        map.put("content", content);
        map.put("score", score);
        return map;
    }
}
