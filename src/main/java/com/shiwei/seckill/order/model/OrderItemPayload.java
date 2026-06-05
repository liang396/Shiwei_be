package com.shiwei.seckill.order.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemPayload {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
}
