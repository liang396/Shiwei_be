package com.shiwei.seckill.promotion.model;

import lombok.Data;

@Data
public class UserCouponRecord {
    private Long userId;
    private Long couponId;
    private String claimedAt;
    private String status;
}

