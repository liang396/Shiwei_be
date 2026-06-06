package com.shiwei.seckill.promotion.service;

import com.shiwei.seckill.promotion.model.PromotionProduct;

import java.util.List;

public interface PromotionService {
    List<PromotionProduct> listSpecialProducts();

    PromotionProduct detail(Long productId);

    PromotionProduct save(PromotionProduct product);

    void toggleStatus(Long productId);

    void delete(Long productId);
}

