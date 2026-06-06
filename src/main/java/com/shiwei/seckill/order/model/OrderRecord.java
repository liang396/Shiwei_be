package com.shiwei.seckill.order.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRecord {
    private Long orderId;
    private String orderNo;
    private String createdTime;
    private Integer orderStatusCode;
    private Long addressId;
    private String consignee;
    private String mobile;
    private String mobileRaw;
    private String address;
    private String addressRaw;
    private Long couponId;
    private String couponTitle;
    private String couponStatus;
    private BigDecimal goodsAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String orderStatus;
    private String payChannel;
    private String cancelReason;
    private Long payExpireAtMillis;
    private Long payRemainSeconds;
    private List<OrderItemPayload> items;
}

