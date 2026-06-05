package com.shiwei.seckill.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_status_log")
public class OrderStatusLogEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Integer sourceStatus;
    private Integer targetStatus;
    private String eventCode;
    private String triggerType;
    private String operatorType;
    private Long operatorId;
    private String requestId;
    private String remark;
    private LocalDateTime createdAt;
}
