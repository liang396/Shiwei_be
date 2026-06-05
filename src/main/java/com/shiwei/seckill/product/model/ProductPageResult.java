package com.shiwei.seckill.product.model;

import lombok.Data;

import java.util.List;

@Data
public class ProductPageResult {
    private List<Product> records;
    private Long nextLastId;
    private boolean hasMore;
}
