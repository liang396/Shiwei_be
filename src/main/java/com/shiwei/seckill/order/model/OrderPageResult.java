package com.shiwei.seckill.order.model;

import lombok.Data;

import java.util.List;

@Data
public class OrderPageResult {
    private List<OrderRecord> records;
    private Long nextLastId;
    private boolean hasMore;
}
