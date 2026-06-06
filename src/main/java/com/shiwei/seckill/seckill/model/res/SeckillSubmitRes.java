package com.shiwei.seckill.seckill.model.res;

import lombok.Data;

@Data
public class SeckillSubmitRes {
    private Boolean success;
    private String message;
    private Integer resultStatus;
    private String messageId;
}

