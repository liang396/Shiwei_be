package com.shiwei.seckill.seckill.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.product.model.Product;
import com.shiwei.seckill.seckill.model.dto.SeckillGoodsSnapshot;
import com.shiwei.seckill.seckill.model.entity.SeckillActivity;
import com.shiwei.seckill.seckill.model.entity.SeckillGoods;
import com.shiwei.seckill.seckill.model.req.SeckillActivityAddReq;
import com.shiwei.seckill.seckill.model.req.SeckillActivityEditReq;
import com.shiwei.seckill.seckill.model.req.SeckillGoodsAddItemReq;
import com.shiwei.seckill.seckill.model.res.SeckillActivityRes;
import com.shiwei.seckill.seckill.model.res.SeckillGoodsRes;
import com.shiwei.seckill.seckill.service.SeckillActivityService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.shiwei.seckill.seckill.config.SeckillRedisKey.activity;
import static com.shiwei.seckill.seckill.config.SeckillRedisKey.stock;

@Service
public class SeckillActivityServiceImpl implements SeckillActivityService {
    private final AtomicLong activityIdGenerator = new AtomicLong(1);
    private final AtomicLong goodsIdGenerator = new AtomicLong(1);
    private final Map<Long, SeckillActivity> activities = new ConcurrentHashMap<>();
    private final Map<Long, List<SeckillGoods>> activityGoods = new ConcurrentHashMap<>();

    @Resource
    private Cache<Object, Object> caffeineBuilder;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addActivity(SeckillActivityAddReq req) {
        validate(req);
        long activityId = activityIdGenerator.getAndIncrement();
        SeckillActivity activityEntity = new SeckillActivity();
        activityEntity.setActivityId(activityId);
        activityEntity.setActivityName(req.getActivityName());
        activityEntity.setActivityStatus(0);
        activityEntity.setStartTime(req.getStartTime());
        activityEntity.setEndTime(req.getEndTime());
        activityEntity.setLimitPerUser(req.getLimitPerUser());
        activityEntity.setActivityDesc(req.getActivityDesc());
        activities.put(activityId, activityEntity);
        activityGoods.put(activityId, buildGoods(activityId, req.getGoodsList(), 0));
        caffeineBuilder.put(activityId, activityEntity);
    }

    @Override
    public void editActivity(SeckillActivityEditReq req) {
        SeckillActivity existing = activities.get(req.getActivityId());
        if (existing == null) {
            throw new BizException("秒杀活动不存在");
        }
        validate(req);
        existing.setActivityName(req.getActivityName());
        existing.setStartTime(req.getStartTime());
        existing.setEndTime(req.getEndTime());
        existing.setLimitPerUser(req.getLimitPerUser());
        existing.setActivityDesc(req.getActivityDesc());
        existing.setActivityStatus(req.getActivityStatus() == null ? existing.getActivityStatus() : req.getActivityStatus());
        activityGoods.put(existing.getActivityId(), buildGoods(existing.getActivityId(), req.getGoodsList(), existing.getActivityStatus()));
        caffeineBuilder.put(existing.getActivityId(), existing);
    }

    @Override
    public void publishActivity(Long activityId) {
        SeckillActivity existing = activities.get(activityId);
        if (existing == null) {
            throw new BizException("秒杀活动不存在");
        }
        existing.setActivityStatus(1);
        warmup(existing, activityGoods.getOrDefault(activityId, new ArrayList<>()));
    }

    @Override
    public List<SeckillActivityRes> listActivities() {
        return activities.values().stream()
                .sorted(Comparator.comparingLong(SeckillActivity::getActivityId))
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    @Override
    public SeckillActivityRes getActivityDetail(Long activityId) {
        SeckillActivity activity = activities.get(activityId);
        return activity == null ? null : toRes(activity);
    }

    @Override
    public SeckillGoodsSnapshot getGoodsSnapshot(Long activityId, Long goodsId) {
        return activityGoods.getOrDefault(activityId, new ArrayList<>()).stream()
                .filter(goods -> goodsId.equals(goods.getGoodsId()))
                .findFirst()
                .map(goods -> new SeckillGoodsSnapshot(activityId, goodsId, goods.getProductId(), goods.getProductItemId(), goods.getSeckillPrice()))
                .orElse(null);
    }

    private void validate(SeckillActivityAddReq req) {
        if (req == null || req.getGoodsList() == null || req.getGoodsList().isEmpty()) {
            throw new BizException("秒杀活动至少需要一个特价商品");
        }
    }

    private List<SeckillGoods> buildGoods(Long activityId, List<SeckillGoodsAddItemReq> reqs, Integer status) {
        List<SeckillGoods> goodsList = new ArrayList<>();
        for (SeckillGoodsAddItemReq req : reqs) {
            SeckillGoods goods = new SeckillGoods();
            goods.setGoodsId(goodsIdGenerator.getAndIncrement());
            goods.setActivityId(activityId);
            goods.setProductId(req.getProductId());
            goods.setProductItemId(req.getProductItemId());
            goods.setSeckillPrice(req.getSeckillPrice());
            goods.setSeckillStock(req.getSeckillStock());
            goods.setAvailableStock(req.getSeckillStock());
            goods.setSortNum(req.getSortNum() == null ? 0 : req.getSortNum());
            goods.setStatus(status);
            goodsList.add(goods);
        }
        return goodsList;
    }

    private void warmup(SeckillActivity activityEntity, List<SeckillGoods> goodsList) {
        if (stringRedisTemplate == null) {
            return;
        }
        String key = activity(activityEntity.getActivityId());
        stringRedisTemplate.opsForHash().put(key, "status", String.valueOf(activityEntity.getActivityStatus()));
        stringRedisTemplate.opsForHash().put(key, "startTime", String.valueOf(activityEntity.getStartTime()));
        stringRedisTemplate.opsForHash().put(key, "endTime", String.valueOf(activityEntity.getEndTime()));
        stringRedisTemplate.opsForHash().put(key, "limitPerUser", String.valueOf(activityEntity.getLimitPerUser()));
        for (SeckillGoods goods : goodsList) {
            stringRedisTemplate.opsForValue().set(stock(activityEntity.getActivityId(), goods.getGoodsId()), String.valueOf(goods.getSeckillStock()));
        }
    }

    private SeckillActivityRes toRes(SeckillActivity activityEntity) {
        SeckillActivityRes res = new SeckillActivityRes();
        res.setActivityId(activityEntity.getActivityId());
        res.setActivityName(activityEntity.getActivityName());
        res.setActivityStatus(activityEntity.getActivityStatus());
        res.setStartTime(activityEntity.getStartTime());
        res.setEndTime(activityEntity.getEndTime());
        res.setLimitPerUser(activityEntity.getLimitPerUser());
        res.setGoodsList(activityGoods.getOrDefault(activityEntity.getActivityId(), new ArrayList<>()).stream()
                .map(this::toGoodsRes)
                .collect(Collectors.toList()));
        return res;
    }

    private SeckillGoodsRes toGoodsRes(SeckillGoods goods) {
        SeckillGoodsRes res = new SeckillGoodsRes();
        res.setGoodsId(goods.getGoodsId());
        res.setProductId(goods.getProductId());
        res.setProductItemId(goods.getProductItemId());
        res.setProductName("商品-" + goods.getProductId());
        res.setProductImage("demo.png");
        res.setOriginalPrice(goods.getSeckillPrice().add(new BigDecimal("20.00")));
        res.setSeckillPrice(goods.getSeckillPrice());
        res.setSeckillStock(goods.getSeckillStock());
        res.setRemainStock(goods.getAvailableStock());
        res.setGoodsStatus(goods.getStatus());
        return res;
    }
}
