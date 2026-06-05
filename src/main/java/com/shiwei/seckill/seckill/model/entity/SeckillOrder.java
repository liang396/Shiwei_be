package com.shiwei.seckill.seckill.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_order")
public class SeckillOrder {
    @TableId(value = "seckill_order_id", type = IdType.AUTO)
    private Long seckillOrderId;
    private Long activityId;
    private Long seckillGoodsId;
    private Long productId;
    private Long productItemId;
    private Long userId;
    private String orderNo;
    private Integer status;
    private BigDecimal seckillPrice;
    private Integer buyNum;
    private LocalDateTime createdAt;
}
