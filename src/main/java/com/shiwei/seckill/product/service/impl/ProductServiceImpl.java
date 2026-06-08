package com.shiwei.seckill.product.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.product.model.Product;
import com.shiwei.seckill.product.model.ProductPageResult;
import com.shiwei.seckill.product.model.ProductSaveReq;
import com.shiwei.seckill.product.service.ProductService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ProductServiceImpl implements ProductService {
    private static final TypeReference<List<Product>> PRODUCT_LIST_TYPE = new TypeReference<List<Product>>() {};
    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 20;

    private final CopyOnWriteArrayList<Product> products = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storagePath = Paths.get("data", "products.json");

    @PostConstruct
    public void initDefaults() {
        loadFromFile();
        if (!products.isEmpty()) {
            return;
        }

        products.add(createProduct(2001L, 3001L, "旗舰折叠屏手机", "高刷大屏与轻薄机身兼顾，热门旗舰限时直降。", "电子产品", "手机平板", "electronics-phone", "6999", 36, 2680, 96, true));
        products.add(createProduct(2002L, 3002L, "轻薄办公笔记本", "14 英寸高色域屏，适合学习办公和移动创作。", "电子产品", "电脑", "electronics-laptop", "5299", 42, 1920, 91, true));
        products.add(createProduct(2003L, 3003L, "空气炸锅 5L", "少油快烤，家常炸物和烘焙都很省心。", "电子产品", "家电", "electronics-appliance", "299", 88, 3560, 88, false));
        products.add(createProduct(2005L, 3005L, "草莓鲜果礼盒", "果香浓郁，冷链到家，适合全家分享。", "食品生鲜", "生鲜果蔬", "fresh-fruit", "49.9", 120, 4250, 93, true));
        products.add(createProduct(2006L, 3006L, "鲜活大虾套餐", "鲜活直送，家庭聚餐更省心。", "食品生鲜", "鲜肉水产", "fresh-seafood", "88", 58, 2380, 90, true));
        products.add(createProduct(2009L, 3009L, "冷泡茉莉茶", "清爽回甘，冰镇饮用更适合夏日。", "酒水饮料", "饮料冲调", "drink-tea", "15.9", 166, 4680, 95, true));
        products.add(createProduct(2011L, 3011L, "厨房纸巾加厚装", "吸水吸油一步到位，居家高频消耗品。", "家居百货", "纸品湿巾", "home-paper", "24.9", 160, 6020, 92, true));
        products.add(createProduct(2017L, 3017L, "轻弹运动跑鞋", "包裹支撑到位，通勤慢跑都合适。", "服饰鞋包", "运动鞋服", "fashion-shoe", "269", 72, 2580, 89, true));
        persist();
    }

    @Override
    public List<Product> list() {
        return sortedProducts();
    }

    @Override
    public ProductPageResult page(Long lastId, Integer size) {
        List<Product> sorted = sortedProducts();
        int pageSize = normalizePageSize(size);
        List<Product> records = new ArrayList<>();
        for (Product product : sorted) {
            if (lastId != null && lastId > 0 && product.getProductId() >= lastId) {
                continue;
            }
            records.add(product);
            if (records.size() == pageSize) {
                break;
            }
        }

        ProductPageResult result = new ProductPageResult();
        result.setRecords(records);
        result.setHasMore(records.size() == pageSize && !records.isEmpty() && records.get(records.size() - 1).getProductId() > sorted.get(sorted.size() - 1).getProductId());
        result.setNextLastId(records.isEmpty() ? null : records.get(records.size() - 1).getProductId());
        if (!records.isEmpty()) {
            long minId = sorted.get(sorted.size() - 1).getProductId();
            result.setHasMore(records.get(records.size() - 1).getProductId() > minId);
        }
        return result;
    }

    @Override
    public Product detail(Long productId) {
        return products.stream()
            .filter(item -> item.getProductId().equals(productId))
            .findFirst()
            .orElse(null);
    }

    @Override
    public synchronized Product save(ProductSaveReq req) {
        validate(req);

        Product target = products.stream()
            .filter(item -> Objects.equals(item.getProductId(), req.getProductId()))
            .findFirst()
            .orElse(null);

        if (target == null) {
            target = new Product();
            target.setProductId(req.getProductId());
            products.add(target);
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

        persist();
        return target;
    }

    private List<Product> sortedProducts() {
        List<Product> list = new ArrayList<>(products);
        list.sort(Comparator.comparing(Product::getProductId).reversed());
        return list;
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
        return gallery.isEmpty() ? resolveProductImage(req.getTheme()) : gallery.get(0);
    }

    private List<String> resolveGallery(ProductSaveReq req) {
        if (req.getProductImages() != null && !req.getProductImages().isEmpty()) {
            return req.getProductImages().stream()
                .filter(item -> item != null && !item.trim().isEmpty())
                .map(String::trim)
                .limit(5)
                .toList();
        }
        return resolveProductImages(req.getTheme());
    }

    private Product createProduct(Long productId, Long productItemId, String name, String description, String category,
                                  String subcategory, String theme, String price, int stock, int sales,
                                  int popularity, boolean featured) {
        Product product = new Product();
        product.setProductId(productId);
        product.setProductItemId(productItemId);
        product.setProductName(name);
        product.setProductImage(resolveProductImage(theme));
        product.setProductImages(resolveProductImages(theme));
        product.setDescription(description);
        product.setDetailContent(description);
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setTheme(theme);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setSales(sales);
        product.setPopularity(popularity);
        product.setFeatured(featured);
        return product;
    }

    private void loadFromFile() {
        if (!Files.exists(storagePath)) {
            return;
        }

        try {
            List<Product> loaded = objectMapper.readValue(storagePath.toFile(), PRODUCT_LIST_TYPE);
            products.clear();
            products.addAll(loaded);
        } catch (IOException e) {
            throw new BizException("商品数据加载失败");
        }
    }

    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), products);
        } catch (IOException e) {
            throw new BizException("商品数据保存失败");
        }
    }

    private String resolveProductImage(String theme) {
        List<String> images = resolveProductImages(theme);
        return images.isEmpty() ? "https://source.unsplash.com/featured/900x900/?product" : images.get(0);
    }

    private List<String> resolveProductImages(String theme) {
        if ("electronics-phone".equals(theme)) {
            return Arrays.asList(
                "https://source.unsplash.com/featured/900x900/?foldable,smartphone,front",
                "https://source.unsplash.com/featured/900x900/?foldable,smartphone,back",
                "https://source.unsplash.com/featured/900x900/?foldable,smartphone,screen",
                "https://source.unsplash.com/featured/900x900/?smartphone,closeup,design",
                "https://source.unsplash.com/featured/900x900/?foldable,phone,lifestyle"
            );
        }
        if ("electronics-laptop".equals(theme)) {
            return Arrays.asList(
                "https://source.unsplash.com/featured/900x900/?laptop,computer,desk",
                "https://source.unsplash.com/featured/900x900/?laptop,keyboard",
                "https://source.unsplash.com/featured/900x900/?laptop,screen",
                "https://source.unsplash.com/featured/900x900/?notebook,computer,work",
                "https://source.unsplash.com/featured/900x900/?laptop,minimal"
            );
        }
        if ("electronics-appliance".equals(theme)) {
            return Arrays.asList(
                "https://source.unsplash.com/featured/900x900/?air,fryer,kitchen",
                "https://source.unsplash.com/featured/900x900/?air,fryer,cooking",
                "https://source.unsplash.com/featured/900x900/?kitchen,appliance,airfryer",
                "https://source.unsplash.com/featured/900x900/?air,fryer,food",
                "https://source.unsplash.com/featured/900x900/?countertop,appliance"
            );
        }
        if ("fresh-fruit".equals(theme)) {
            return Arrays.asList(
                "https://source.unsplash.com/featured/900x900/?strawberry,fruit,box",
                "https://source.unsplash.com/featured/900x900/?berries,fruit,basket",
                "https://source.unsplash.com/featured/900x900/?fresh,fruit,closeup",
                "https://source.unsplash.com/featured/900x900/?strawberry,market",
                "https://source.unsplash.com/featured/900x900/?fruit,gift,box"
            );
        }
        if ("fresh-seafood".equals(theme)) {
            return Arrays.asList(
                "https://source.unsplash.com/featured/900x900/?shrimp,seafood",
                "https://source.unsplash.com/featured/900x900/?prawn,seafood,plate",
                "https://source.unsplash.com/featured/900x900/?fresh,shrimp,market",
                "https://source.unsplash.com/featured/900x900/?seafood,cooking",
                "https://source.unsplash.com/featured/900x900/?shrimp,dinner"
            );
        }
        if ("drink-tea".equals(theme)) {
            return Arrays.asList(
                "https://source.unsplash.com/featured/900x900/?tea,cold,drink",
                "https://source.unsplash.com/featured/900x900/?iced,tea,cup",
                "https://source.unsplash.com/featured/900x900/?tea,refreshment",
                "https://source.unsplash.com/featured/900x900/?drink,summer,tea",
                "https://source.unsplash.com/featured/900x900/?tea,bottle"
            );
        }
        if ("home-paper".equals(theme)) {
            return Arrays.asList(
                "https://source.unsplash.com/featured/900x900/?paper,towel,home",
                "https://source.unsplash.com/featured/900x900/?kitchen,paper,towel",
                "https://source.unsplash.com/featured/900x900/?home,cleaning,paper",
                "https://source.unsplash.com/featured/900x900/?household,paper",
                "https://source.unsplash.com/featured/900x900/?tissue,roll"
            );
        }
        if ("fashion-shoe".equals(theme)) {
            return Arrays.asList(
                "https://source.unsplash.com/featured/900x900/?running,shoes,sneakers",
                "https://source.unsplash.com/featured/900x900/?sport,shoe,closeup",
                "https://source.unsplash.com/featured/900x900/?sneakers,fashion",
                "https://source.unsplash.com/featured/900x900/?running,shoe,lifestyle",
                "https://source.unsplash.com/featured/900x900/?athletic,footwear"
            );
        }
        return Arrays.asList(
            "https://source.unsplash.com/featured/900x900/?product",
            "https://source.unsplash.com/featured/900x900/?product,detail",
            "https://source.unsplash.com/featured/900x900/?shopping,item",
            "https://source.unsplash.com/featured/900x900/?store,product",
            "https://source.unsplash.com/featured/900x900/?goods,display"
        );
    }
}

