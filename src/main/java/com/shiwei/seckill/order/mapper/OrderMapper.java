package com.shiwei.seckill.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiwei.seckill.order.entity.OrderEntity;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper extends BaseMapper<OrderEntity> {
    int updateStatus(@Param("orderId") Long orderId,
                     @Param("sourceStatus") Integer sourceStatus,
                     @Param("targetStatus") Integer targetStatus,
                     @Param("payChannel") String payChannel,
                     @Param("payTime") java.time.LocalDateTime payTime,
                     @Param("canceledTime") java.time.LocalDateTime canceledTime);
}

