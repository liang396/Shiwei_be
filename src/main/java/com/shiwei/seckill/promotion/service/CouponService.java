package com.shiwei.seckill.promotion.service;

import com.shiwei.seckill.promotion.model.Coupon;

import java.util.List;

public interface CouponService {
    List<Coupon> listByUser(Long userId);

    List<Coupon> listAvailableByUser(Long userId);

    Coupon claim(Long userId, Long couponId);

    void consume(Long userId, Long couponId, java.math.BigDecimal goodsAmount);

    void restore(Long userId, Long couponId);
}
