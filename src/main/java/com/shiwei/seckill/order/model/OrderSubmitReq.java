package com.shiwei.seckill.order.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitReq {
    private Long addressId;
    private String consignee;
    private String mobile;
    private String address;
    private Long couponId;
    private String couponTitle;
    private BigDecimal goodsAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private List<OrderItemPayload> items;
}
