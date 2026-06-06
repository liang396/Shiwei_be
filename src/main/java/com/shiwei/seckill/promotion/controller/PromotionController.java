package com.shiwei.seckill.promotion.controller;

import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.promotion.service.CouponService;
import com.shiwei.seckill.promotion.service.PromotionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/promotion")
public class PromotionController {
    @Resource
    private CouponService couponService;

    @Resource
    private PromotionService promotionService;

    @GetMapping("/coupons")
    public ApiResponse<?> coupons() {
        return ApiResponse.success(couponService.listByUser(1L));
    }

    @GetMapping("/coupons/available")
    public ApiResponse<?> availableCoupons() {
        return ApiResponse.success(couponService.listAvailableByUser(1L));
    }

    @PostMapping("/coupons/{couponId}/claim")
    public ApiResponse<?> claimCoupon(@PathVariable Long couponId) {
        return ApiResponse.successMessage("优惠券领取成功", couponService.claim(1L, couponId));
    }

    @GetMapping("/special-products")
    public ApiResponse<?> specialProducts() {
        return ApiResponse.success(promotionService.listSpecialProducts());
    }

    @PostMapping("/special-products/save")
    public ApiResponse<?> saveSpecialProduct(@org.springframework.web.bind.annotation.RequestBody com.shiwei.seckill.promotion.model.PromotionProduct product) {
        return ApiResponse.successMessage("特价商品保存成功", promotionService.save(product));
    }

    @PostMapping("/special-products/{productId}/toggle")
    public ApiResponse<?> toggleSpecialProduct(@PathVariable Long productId) {
        promotionService.toggleStatus(productId);
        return ApiResponse.successMessage("特价商品状态已更新", null);
    }

    @DeleteMapping("/special-products/{productId}")
    public ApiResponse<?> deleteSpecialProduct(@PathVariable Long productId) {
        promotionService.delete(productId);
        return ApiResponse.successMessage("特价商品已删除", null);
    }
}

