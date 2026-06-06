package com.shiwei.seckill.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message_processed")
public class MessageProcessedEntity {
    @TableId
    private String eventId;
    private LocalDateTime processedTime;
}

