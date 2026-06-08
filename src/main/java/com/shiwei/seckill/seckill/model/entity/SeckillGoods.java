package com.shiwei.seckill.seckill.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("seckill_goods")
public class SeckillGoods {
    @TableId
    private Long goodsId;
    private Long activityId;
    private Long productId;
    private Long productItemId;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer availableStock;
    private Integer sortNum;
    private Integer status;
}

