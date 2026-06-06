package com.shiwei.seckill.cart.model.req;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartAddReq {
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
}

