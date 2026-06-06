package com.shiwei.seckill.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_order_item")
public class OrderItemEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long productId;
    private Long productItemId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;
}

