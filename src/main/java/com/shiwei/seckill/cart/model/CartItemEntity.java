package com.shiwei.seckill.cart.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cart_item")
public class CartItemEntity {
    @TableId(type = IdType.AUTO)
    private Long cartId;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private Boolean checked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
