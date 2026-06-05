package com.shiwei.seckill.promotion.service.impl;

import com.shiwei.seckill.promotion.model.PromotionProduct;
import com.shiwei.seckill.promotion.service.PromotionService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PromotionServiceImpl implements PromotionService {
    private final CopyOnWriteArrayList<PromotionProduct> promotionProducts = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void initDefaults() {
        if (!promotionProducts.isEmpty()) {
            return;
        }

        promotionProducts.add(createProduct(2001L, "旗舰折叠屏手机", "高刷大屏与轻薄机身兼顾，热门旗舰限时直降。", "电子产品", "手机平板", "electronics-phone", 7999, 6999, 36, 2680, 96, "爆款直降"));
        promotionProducts.add(createProduct(2003L, "空气炸锅 5L", "少油快烤，家常炸物和烘焙都很省心。", "电子产品", "家电", "electronics-appliance", 399, 299, 88, 3560, 88, "厨房特惠"));
        promotionProducts.add(createProduct(2009L, "冷泡茉莉茶", "清爽回甘，冰镇饮用更适合夏日。", "酒水饮料", "饮料冲调", "drink-tea", 19.9, 15.9, 120, 4680, 95, "清凉好价"));
        promotionProducts.add(createProduct(2011L, "厨房纸巾加厚装", "吸水吸油一步到位，居家高频消耗品。", "家居百货", "纸品湿巾", "home-paper", 32.9, 24.9, 160, 6020, 92, "家庭囤货"));
        promotionProducts.add(createProduct(2013L, "氨基酸洁面乳", "温和清洁不紧绷，日常通勤党友好。", "个护美妆", "护肤彩妆", "beauty-cleanser", 99, 79, 54, 1660, 86, "口碑特价"));
        promotionProducts.add(createProduct(2017L, "轻弹运动跑鞋", "包裹支撑到位，通勤慢跑都合适。", "服饰鞋包", "运动鞋服", "fashion-shoe", 329, 269, 72, 2580, 89, "换季直降"));
    }

    @Override
    public List<PromotionProduct> listSpecialProducts() {
        List<PromotionProduct> result = new ArrayList<>(promotionProducts);
        result.sort(Comparator.comparing(PromotionProduct::getProductId));
        return result;
    }

    @Override
    public PromotionProduct detail(Long productId) {
        return promotionProducts.stream()
            .filter(item -> item.getProductId().equals(productId))
            .findFirst()
            .orElse(null);
    }

    @Override
    public synchronized PromotionProduct save(PromotionProduct product) {
        PromotionProduct target = detail(product.getProductId());
        if (target == null) {
            target = new PromotionProduct();
            promotionProducts.add(target);
        }
        target.setProductId(product.getProductId());
        target.setProductName(product.getProductName());
        target.setProductImage(product.getProductImage());
        target.setDescription(product.getDescription());
        target.setCategory(product.getCategory());
        target.setSubcategory(product.getSubcategory());
        target.setTheme(product.getTheme());
        target.setOriginalPrice(product.getOriginalPrice());
        target.setPromotionPrice(product.getPromotionPrice());
        target.setStock(product.getStock());
        target.setSales(product.getSales());
        target.setPopularity(product.getPopularity());
        target.setTag(product.getTag());
        target.setStatus(product.getStatus() == null || product.getStatus().isEmpty() ? "ACTIVE" : product.getStatus());
        return target;
    }

    @Override
    public synchronized void toggleStatus(Long productId) {
        PromotionProduct target = detail(productId);
        if (target == null) {
            return;
        }
        target.setStatus("ACTIVE".equals(target.getStatus()) ? "INACTIVE" : "ACTIVE");
    }

    @Override
    public synchronized void delete(Long productId) {
        promotionProducts.removeIf(item -> item.getProductId().equals(productId));
    }

    private PromotionProduct createProduct(Long productId, String name, String description, String category, String subcategory,
                                           String theme, double originalPrice, double promotionPrice, int stock, int sales,
                                           int popularity, String tag) {
        PromotionProduct product = new PromotionProduct();
        product.setProductId(productId);
        product.setProductName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setTheme(theme);
        product.setProductImage("demo.png");
        product.setOriginalPrice(BigDecimal.valueOf(originalPrice));
        product.setPromotionPrice(BigDecimal.valueOf(promotionPrice));
        product.setStock(stock);
        product.setSales(sales);
        product.setPopularity(popularity);
        product.setTag(tag);
        product.setStatus("ACTIVE");
        return product;
    }
}
