package com.shiwei.seckill.order.service.support;

import com.shiwei.seckill.order.enums.OperatorTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderOperateContext {
    private OperatorTypeEnum operatorType;
    private Long operatorId;
    private String remark;
    private String payChannel;
    private String triggerType;
    private String requestId;
    private LocalDateTime operateTime;
}
