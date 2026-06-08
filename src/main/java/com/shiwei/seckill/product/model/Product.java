package com.shiwei.seckill.product.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("product")
public class Product {
    @TableId
    private Long productId;
    private Long productItemId;
    private String productName;
    private String productImage;
    @TableField("product_images")
    @JsonIgnore
    private String productImagesJson;
    @TableField(exist = false)
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

