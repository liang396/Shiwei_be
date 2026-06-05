package com.shiwei.seckill.promotion.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.promotion.model.Coupon;
import com.shiwei.seckill.promotion.model.UserCouponRecord;
import com.shiwei.seckill.promotion.service.CouponService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CouponServiceImpl implements CouponService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final TypeReference<List<UserCouponRecord>> USER_COUPON_LIST_TYPE = new TypeReference<List<UserCouponRecord>>() {};

    private final List<Coupon> templates = new ArrayList<>();
    private final CopyOnWriteArrayList<UserCouponRecord> userCoupons = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storagePath = Paths.get("data", "user-coupons.json");

    @PostConstruct
    public void initDefaults() {
        loadFromFile();
        templates.clear();
        templates.add(createTemplate(1L, "新人券", "满99减10", "首单可用，限食品生鲜", "新人专享", "食品生鲜", 99));
        templates.add(createTemplate(2L, "品类券", "满199减30", "适用电子产品和家居百货", "限时领券", "电子产品/家居百货", 199));
        templates.add(createTemplate(3L, "运费券", "满39包邮", "食品生鲜和酒水饮料可用", "常用好券", "酒水饮料/食品生鲜", 39));
        templates.add(createTemplate(4L, "爆款券", "满299减50", "适用热门活动商品", "今日推荐", "活动商品", 299));
    }

    @Override
    public List<Coupon> listByUser(Long userId) {
        List<Coupon> result = new ArrayList<>();
        for (Coupon template : templates) {
            result.add(buildCouponView(userId, template));
        }
        return result;
    }

    @Override
    public List<Coupon> listAvailableByUser(Long userId) {
        List<Coupon> result = new ArrayList<>();
        for (Coupon template : templates) {
            Coupon item = buildCouponView(userId, template);
            if (item.isClaimed() && "CLAIMED".equals(item.getStatus())) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public synchronized Coupon claim(Long userId, Long couponId) {
        Coupon template = templates.stream()
            .filter(item -> item.getCouponId().equals(couponId))
            .findFirst()
            .orElseThrow(() -> new BizException("优惠券不存在"));

        UserCouponRecord existing = findRecord(userId, couponId);
        if (existing == null) {
            UserCouponRecord record = new UserCouponRecord();
            record.setUserId(userId);
            record.setCouponId(couponId);
            record.setClaimedAt(LocalDateTime.now().format(FORMATTER));
            record.setStatus("CLAIMED");
            userCoupons.add(record);
            persist();
            existing = record;
        }

        Coupon result = buildCouponView(userId, template);
        result.setClaimed(true);
        result.setStatus(existing.getStatus());
        return result;
    }

    @Override
    public synchronized void consume(Long userId, Long couponId, BigDecimal goodsAmount) {
        if (couponId == null) {
            return;
        }

        Coupon template = templates.stream()
            .filter(item -> item.getCouponId().equals(couponId))
            .findFirst()
            .orElseThrow(() -> new BizException("优惠券不存在"));

        UserCouponRecord record = findRecord(userId, couponId);
        if (record == null || !"CLAIMED".equals(record.getStatus())) {
            throw new BizException("优惠券不可用");
        }

        if (goodsAmount == null || goodsAmount.compareTo(BigDecimal.valueOf(template.getThresholdAmount())) < 0) {
            throw new BizException("未达到优惠券使用门槛");
        }

        record.setStatus("USED");
        persist();
    }

    @Override
    public synchronized void restore(Long userId, Long couponId) {
        if (couponId == null) {
            return;
        }
        UserCouponRecord record = findRecord(userId, couponId);
        if (record == null) {
            return;
        }
        if (!"USED".equals(record.getStatus())) {
            return;
        }
        record.setStatus("CLAIMED");
        persist();
    }

    private Coupon createTemplate(Long id, String title, String value, String description, String tag, String scope, Integer thresholdAmount) {
        Coupon coupon = new Coupon();
        coupon.setCouponId(id);
        coupon.setTitle(title);
        coupon.setValue(value);
        coupon.setDescription(description);
        coupon.setTag(tag);
        coupon.setScope(scope);
        coupon.setThresholdAmount(thresholdAmount);
        coupon.setStatus("AVAILABLE");
        coupon.setClaimed(false);
        return coupon;
    }

    private UserCouponRecord findRecord(Long userId, Long couponId) {
        return userCoupons.stream()
            .filter(item -> item.getUserId().equals(userId) && item.getCouponId().equals(couponId))
            .findFirst()
            .orElse(null);
    }

    private Coupon buildCouponView(Long userId, Coupon template) {
        Coupon item = new Coupon();
        item.setCouponId(template.getCouponId());
        item.setTitle(template.getTitle());
        item.setValue(template.getValue());
        item.setDescription(template.getDescription());
        item.setTag(template.getTag());
        item.setScope(template.getScope());
        item.setThresholdAmount(template.getThresholdAmount());
        UserCouponRecord record = findRecord(userId, template.getCouponId());
        item.setClaimed(record != null);
        item.setStatus(record == null ? "AVAILABLE" : record.getStatus());
        return item;
    }

    private void loadFromFile() {
        if (!Files.exists(storagePath)) {
            return;
        }

        try {
            List<UserCouponRecord> loaded = objectMapper.readValue(storagePath.toFile(), USER_COUPON_LIST_TYPE);
            userCoupons.clear();
            userCoupons.addAll(loaded);
        } catch (IOException e) {
            throw new BizException("优惠券数据加载失败");
        }
    }

    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), userCoupons);
        } catch (IOException e) {
            throw new BizException("优惠券状态保存失败");
        }
    }
}
