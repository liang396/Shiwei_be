package com.shiwei.seckill.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.product.mapper.ProductMapper;
import com.shiwei.seckill.product.model.Product;
import com.shiwei.seckill.product.model.ProductPageResult;
import com.shiwei.seckill.product.model.ProductSaveReq;
import com.shiwei.seckill.product.service.ProductService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

@Service
public class ProductServiceImpl implements ProductService {
    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 20;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ProductMapper productMapper;

    @PostConstruct
    public void initDefaults() {
        if (productMapper.selectCount(null) > 0) {
            return;
        }
        saveDefault(2001L, 3001L, "旗舰折叠屏手机", "高刷大屏与轻薄机身兼顾，热门旗舰限时直降。", "电子产品", "手机平板", "electronics-phone", "6999", 36, 2680, 96, true);
        saveDefault(2002L, 3002L, "轻薄办公笔记本", "14 英寸高色域屏，适合学习办公和移动创作。", "电子产品", "电脑", "electronics-laptop", "5299", 42, 1920, 91, true);
        saveDefault(2003L, 3003L, "空气炸锅 5L", "少油快烤，家常炸物和烘焙都很省心。", "电子产品", "家电", "electronics-appliance", "299", 88, 3560, 88, false);
        saveDefault(2005L, 3005L, "草莓鲜果礼盒", "果香浓郁，冷链到家，适合全家分享。", "食品生鲜", "生鲜果蔬", "fresh-fruit", "49.9", 120, 4250, 93, true);
        saveDefault(2006L, 3006L, "鲜活大虾套餐", "鲜活直送，家庭聚餐更省心。", "食品生鲜", "鲜肉水产", "fresh-seafood", "88", 58, 2380, 90, true);
        saveDefault(2009L, 3009L, "冷泡茉莉茶", "清爽回甘，冰镇饮用更适合夏日。", "酒水饮料", "饮料冲调", "drink-tea", "15.9", 166, 4680, 95, true);
        saveDefault(2011L, 3011L, "厨房纸巾加厚装", "吸水吸油一步到位，居家高频消耗品。", "家居百货", "纸品湿巾", "home-paper", "24.9", 160, 6020, 92, true);
        saveDefault(2017L, 3017L, "轻弹运动跑鞋", "包裹支撑到位，通勤慢跑都合适。", "服饰鞋包", "运动鞋服", "fashion-shoe", "269", 72, 2580, 89, true);
    }

    @Override
    public List<Product> list() {
        return deserializeFields(productMapper.selectList(
            new LambdaQueryWrapper<Product>().orderByDesc(Product::getProductId)
        ));
    }

    @Override
    public ProductPageResult page(Long lastId, Integer size) {
        int pageSize = normalizePageSize(size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
            .orderByDesc(Product::getProductId)
            .last("limit " + pageSize);
        if (lastId != null && lastId > 0) {
            wrapper.lt(Product::getProductId, lastId);
        }

        List<Product> records = deserializeFields(productMapper.selectList(wrapper));
        ProductPageResult result = new ProductPageResult();
        result.setRecords(records);
        result.setHasMore(records.size() == pageSize);
        result.setNextLastId(records.isEmpty() ? null : records.get(records.size() - 1).getProductId());
        return result;
    }

    @Override
    public Product detail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return null;
        }
        deserialize(product);
        return product;
    }

    @Override
    public synchronized Product save(ProductSaveReq req) {
        validate(req);

        Product target = productMapper.selectById(req.getProductId());
        if (target == null) {
            target = new Product();
            target.setProductId(req.getProductId());
        }

        target.setProductItemId(req.getProductItemId());
        target.setProductName(req.getProductName().trim());
        target.setProductImage(resolveCoverImage(req));
        target.setProductImages(resolveGallery(req));
        target.setDescription(req.getDescription());
        target.setDetailContent(req.getDetailContent());
        target.setCategory(req.getCategory());
        target.setSubcategory(req.getSubcategory());
        target.setTheme(req.getTheme());
        target.setPrice(req.getPrice());
        target.setStock(req.getStock());
        target.setSales(req.getSales() == null ? 0 : req.getSales());
        target.setPopularity(req.getPopularity() == null ? 0 : req.getPopularity());
        target.setFeatured(Boolean.TRUE.equals(req.getFeatured()));

        Product persisted = copyForPersistence(target);
        if (productMapper.selectById(target.getProductId()) == null) {
            productMapper.insert(persisted);
        } else {
            productMapper.updateById(persisted);
        }
        return target;
    }

    private void saveDefault(Long productId, Long productItemId, String name, String description, String category,
                             String subcategory, String theme, String price, int stock, int sales, int popularity,
                             boolean featured) {
        ProductSaveReq req = new ProductSaveReq();
        req.setProductId(productId);
        req.setProductItemId(productItemId);
        req.setProductName(name);
        req.setDescription(description);
        req.setDetailContent(description);
        req.setCategory(category);
        req.setSubcategory(subcategory);
        req.setTheme(theme);
        req.setPrice(new BigDecimal(price));
        req.setStock(stock);
        req.setSales(sales);
        req.setPopularity(popularity);
        req.setFeatured(featured);
        save(req);
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void validate(ProductSaveReq req) {
        if (req.getProductId() == null) {
            throw new BizException("productId 不能为空");
        }
        if (req.getProductItemId() == null) {
            throw new BizException("productItemId 不能为空");
        }
        if (req.getProductName() == null || req.getProductName().trim().isEmpty()) {
            throw new BizException("productName 不能为空");
        }
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("price 必须大于 0");
        }
        if (req.getStock() == null || req.getStock() < 0) {
            throw new BizException("stock 不能小于 0");
        }
    }

    private String resolveCoverImage(ProductSaveReq req) {
        if (req.getProductImage() != null && !req.getProductImage().trim().isEmpty()) {
            return req.getProductImage().trim();
        }
        List<String> gallery = resolveGallery(req);
        return gallery.isEmpty() ? null : gallery.get(0);
    }

    private List<String> resolveGallery(ProductSaveReq req) {
        if (req.getProductImages() == null) {
            return new ArrayList<>();
        }
        return req.getProductImages().stream()
            .filter(item -> item != null && !item.trim().isEmpty())
            .map(String::trim)
            .toList();
    }

    private Product copyForPersistence(Product source) {
        Product copy = new Product();
        copy.setProductId(source.getProductId());
        copy.setProductItemId(source.getProductItemId());
        copy.setProductName(source.getProductName());
        copy.setProductImage(source.getProductImage());
        copy.setProductImages(source.getProductImages());
        copy.setDescription(source.getDescription());
        copy.setDetailContent(source.getDetailContent());
        copy.setCategory(source.getCategory());
        copy.setSubcategory(source.getSubcategory());
        copy.setTheme(source.getTheme());
        copy.setPrice(source.getPrice());
        copy.setStock(source.getStock());
        copy.setSales(source.getSales());
        copy.setPopularity(source.getPopularity());
        copy.setFeatured(source.getFeatured());
        copy.setDescription(source.getDescription());
        copy.setDetailContent(source.getDetailContent());
        copy.setCategory(source.getCategory());
        copy.setSubcategory(source.getSubcategory());
        copy.setTheme(source.getTheme());
        copy.setProductImagesJson(serializeImages(source.getProductImages()));
        return copy;
    }

    private List<Product> deserializeFields(List<Product> products) {
        for (Product product : products) {
            deserialize(product);
        }
        return products;
    }

    private void deserialize(Product product) {
        if (product == null) {
            return;
        }
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            return;
        }
        String rawJson = product.getProductImagesJson();
        if (rawJson != null && !rawJson.isBlank()) {
            try {
                product.setProductImages(objectMapper.readValue(rawJson, STRING_LIST_TYPE));
            } catch (JsonProcessingException e) {
                product.setProductImages(new ArrayList<>());
            }
            return;
        }
        product.setProductImages(new ArrayList<>());
    }

    private String serializeImages(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images == null ? Collections.emptyList() : images);
        } catch (JsonProcessingException e) {
            throw new BizException("商品图片序列化失败");
        }
    }
}
