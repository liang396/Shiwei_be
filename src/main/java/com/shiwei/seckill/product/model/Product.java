package com.shiwei.seckill.product.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Product {
    private Long productId;
    private Long productItemId;
    private String productName;
    private String productImage;
    private List<String> productImages;
    private String description;
    private String detailContent;
    private String category;
    private String subcategory;
    private String theme;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private Integer popularity;
    private Boolean featured;
}

