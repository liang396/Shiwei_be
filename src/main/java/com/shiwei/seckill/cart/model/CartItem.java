package com.shiwei.seckill.cart.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItem {
    private Long cartId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private Boolean checked;
}
