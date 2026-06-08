package com.shiwei.seckill.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.product.model.Product;
import com.shiwei.seckill.product.service.ProductService;
import com.shiwei.seckill.seckill.mapper.SeckillActivityMapper;
import com.shiwei.seckill.seckill.mapper.SeckillGoodsMapper;
import com.shiwei.seckill.seckill.model.dto.SeckillGoodsSnapshot;
import com.shiwei.seckill.seckill.model.entity.SeckillActivity;
import com.shiwei.seckill.seckill.model.entity.SeckillGoods;
import com.shiwei.seckill.seckill.model.req.SeckillActivityAddReq;
import com.shiwei.seckill.seckill.model.req.SeckillActivityEditReq;
import com.shiwei.seckill.seckill.model.req.SeckillGoodsAddItemReq;
import com.shiwei.seckill.seckill.model.res.SeckillActivityRes;
import com.shiwei.seckill.seckill.model.res.SeckillGoodsRes;
import com.shiwei.seckill.seckill.service.SeckillActivityService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.shiwei.seckill.seckill.config.SeckillRedisKey.activity;
import static com.shiwei.seckill.seckill.config.SeckillRedisKey.goodsSnapshot;
import static com.shiwei.seckill.seckill.config.SeckillRedisKey.stock;

@Service
public class SeckillActivityServiceImpl implements SeckillActivityService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private Cache<Object, Object> caffeineBuilder;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ProductService productService;
    @Resource
    private SeckillActivityMapper seckillActivityMapper;
    @Resource
    private SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public void addActivity(SeckillActivityAddReq req) {
        validate(req);
        SeckillActivity activityEntity = new SeckillActivity();
        activityEntity.setActivityId(req instanceof SeckillActivityEditReq editReq ? editReq.getActivityId() : System.currentTimeMillis());
        activityEntity.setActivityName(req.getActivityName());
        activityEntity.setActivityStatus(0);
        activityEntity.setStartTime(req.getStartTime());
        activityEntity.setEndTime(req.getEndTime());
        activityEntity.setLimitPerUser(req.getLimitPerUser());
        activityEntity.setActivityDesc(req.getActivityDesc());
        seckillActivityMapper.insert(activityEntity);
        replaceGoods(activityEntity.getActivityId(), req.getGoodsList(), 0);
        caffeineBuilder.put(activityEntity.getActivityId(), activityEntity);
    }

    @Override
    public void editActivity(SeckillActivityEditReq req) {
        SeckillActivity existing = seckillActivityMapper.selectById(req.getActivityId());
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
        seckillActivityMapper.updateById(existing);
        replaceGoods(existing.getActivityId(), req.getGoodsList(), existing.getActivityStatus());
        caffeineBuilder.put(existing.getActivityId(), existing);
    }

    @Override
    public void publishActivity(Long activityId) {
        SeckillActivity existing = seckillActivityMapper.selectById(activityId);
        if (existing == null) {
            throw new BizException("秒杀活动不存在");
        }
        existing.setActivityStatus(1);
        seckillActivityMapper.updateById(existing);
        List<SeckillGoods> goodsList = listGoods(activityId);
        warmup(existing, goodsList);
    }

    @Override
    public List<SeckillActivityRes> listActivities() {
        return seckillActivityMapper.selectList(
            new LambdaQueryWrapper<SeckillActivity>().orderByAsc(SeckillActivity::getActivityId)
        ).stream().map(this::toRes).collect(Collectors.toList());
    }

    @Override
    public SeckillActivityRes getActivityDetail(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        return activity == null ? null : toRes(activity);
    }

    @Override
    public SeckillGoodsSnapshot getGoodsSnapshot(Long activityId, Long goodsId) {
        if (stringRedisTemplate != null) {
            String snapshotJson = stringRedisTemplate.opsForValue().get(goodsSnapshot(activityId, goodsId));
            if (snapshotJson != null && !snapshotJson.isBlank()) {
                try {
                    return objectMapper.readValue(snapshotJson, SeckillGoodsSnapshot.class);
                } catch (JsonProcessingException ignored) {
                }
            }
        }
        SeckillGoods goods = seckillGoodsMapper.selectOne(
            new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActivityId, activityId)
                .eq(SeckillGoods::getGoodsId, goodsId)
                .last("limit 1")
        );
        if (goods == null) {
            return null;
        }
        return new SeckillGoodsSnapshot(activityId, goodsId, goods.getProductId(), goods.getProductItemId(), goods.getSeckillPrice());
    }

    private void validate(SeckillActivityAddReq req) {
        if (req == null || req.getGoodsList() == null || req.getGoodsList().isEmpty()) {
            throw new BizException("秒杀活动至少需要一个特价商品");
        }
    }

    private void replaceGoods(Long activityId, List<SeckillGoodsAddItemReq> reqs, Integer status) {
        seckillGoodsMapper.delete(new LambdaQueryWrapper<SeckillGoods>().eq(SeckillGoods::getActivityId, activityId));
        long goodsIdSeed = System.currentTimeMillis();
        for (SeckillGoodsAddItemReq req : reqs) {
            SeckillGoods goods = new SeckillGoods();
            goods.setGoodsId(goodsIdSeed++);
            goods.setActivityId(activityId);
            goods.setProductId(req.getProductId());
            goods.setProductItemId(req.getProductItemId());
            goods.setSeckillPrice(req.getSeckillPrice());
            goods.setSeckillStock(req.getSeckillStock());
            goods.setAvailableStock(req.getSeckillStock());
            goods.setSortNum(req.getSortNum() == null ? 0 : req.getSortNum());
            goods.setStatus(status);
            seckillGoodsMapper.insert(goods);
        }
    }

    private List<SeckillGoods> listGoods(Long activityId) {
        return seckillGoodsMapper.selectList(
            new LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getActivityId, activityId)
                .orderByAsc(SeckillGoods::getSortNum)
                .orderByAsc(SeckillGoods::getGoodsId)
        );
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
            stringRedisTemplate.opsForValue().set(stock(activityEntity.getActivityId(), goods.getGoodsId()), String.valueOf(goods.getAvailableStock()));
            try {
                stringRedisTemplate.opsForValue().set(
                    goodsSnapshot(activityEntity.getActivityId(), goods.getGoodsId()),
                    objectMapper.writeValueAsString(new SeckillGoodsSnapshot(
                        activityEntity.getActivityId(),
                        goods.getGoodsId(),
                        goods.getProductId(),
                        goods.getProductItemId(),
                        goods.getSeckillPrice()
                    ))
                );
            } catch (JsonProcessingException ignored) {
            }
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
        res.setGoodsList(listGoods(activityEntity.getActivityId()).stream()
            .map(this::toGoodsRes)
            .collect(Collectors.toList()));
        return res;
    }

    private SeckillGoodsRes toGoodsRes(SeckillGoods goods) {
        Product product = productService.detail(goods.getProductId());
        SeckillGoodsRes res = new SeckillGoodsRes();
        res.setGoodsId(goods.getGoodsId());
        res.setProductId(goods.getProductId());
        res.setProductItemId(goods.getProductItemId());
        res.setProductName(product != null ? product.getProductName() : ("商品-" + goods.getProductId()));
        res.setProductImage(product != null ? product.getProductImage() : "");
        res.setOriginalPrice(product != null ? product.getPrice() : goods.getSeckillPrice());
        res.setSeckillPrice(goods.getSeckillPrice());
        res.setSeckillStock(goods.getSeckillStock());
        res.setRemainStock(goods.getAvailableStock());
        res.setGoodsStatus(goods.getStatus());
        return res;
    }
}
