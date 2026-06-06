package com.shiwei.seckill.product.service;

import com.shiwei.seckill.product.model.Product;
import com.shiwei.seckill.product.model.ProductPageResult;
import com.shiwei.seckill.product.model.ProductSaveReq;

import java.util.List;

public interface ProductService {
    List<Product> list();

    ProductPageResult page(Long lastId, Integer size);

    Product detail(Long productId);

    Product save(ProductSaveReq req);
}

