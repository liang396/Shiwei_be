package com.shiwei.seckill.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message_dead_letter")
public class MessageDeadLetterEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizKey;
    private String eventType;
    private String payload;
    private String failReason;
    private LocalDateTime createdAt;
}
