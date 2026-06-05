package com.shiwei.seckill.promotion.model;

import lombok.Data;

@Data
public class Coupon {
    private Long couponId;
    private String title;
    private String value;
    private String description;
    private String tag;
    private String scope;
    private Integer thresholdAmount;
    private String status;
    private boolean claimed;
}
