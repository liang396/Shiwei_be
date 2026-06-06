package com.shiwei.seckill.promotion.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionProduct {
    private Long productId;
    private String productName;
    private String productImage;
    private String description;
    private String category;
    private String subcategory;
    private String theme;
    private BigDecimal originalPrice;
    private BigDecimal promotionPrice;
    private Integer stock;
    private Integer sales;
    private Integer popularity;
    private String tag;
    private String status;
}

