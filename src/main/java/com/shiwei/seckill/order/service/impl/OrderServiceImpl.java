package com.shiwei.seckill.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.entity.OrderItemEntity;
import com.shiwei.seckill.order.enums.OperatorTypeEnum;
import com.shiwei.seckill.order.enums.OrderEventEnum;
import com.shiwei.seckill.order.enums.OrderSourceTypeEnum;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import com.shiwei.seckill.order.mapper.OrderItemMapper;
import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.model.OrderItemPayload;
import com.shiwei.seckill.order.model.OrderPageResult;
import com.shiwei.seckill.order.model.OrderRecord;
import com.shiwei.seckill.order.model.OrderSubmitReq;
import com.shiwei.seckill.order.service.OrderDuplicateGuardService;
import com.shiwei.seckill.order.service.OrderService;
import com.shiwei.seckill.order.service.OrderStateMachineService;
import com.shiwei.seckill.order.service.OrderTimeoutService;
import com.shiwei.seckill.order.service.support.OrderOperateContext;
import com.shiwei.seckill.promotion.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Long DEFAULT_USER_ID = 1L;
    private static final String ORDER_DETAIL_CACHE_PREFIX = "order:detail:";
    private static final String ORDER_LIST_CACHE_KEY = "order:list:user:1";
    private static final long DETAIL_CACHE_BASE_MILLIS = 5 * 60 * 1000L;
    private static final long LIST_CACHE_BASE_MILLIS = 2 * 60 * 1000L;
    private static final long EMPTY_CACHE_MILLIS = 60 * 1000L;
    private static final long JITTER_MILLIS = 60 * 1000L;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ConcurrentHashMap<String, Object> cacheLocks = new ConcurrentHashMap<>();

    @Resource
    private CouponService couponService;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private OrderStateMachineService orderStateMachineService;
    @Resource
    private OrderTimeoutService orderTimeoutService;
    @Resource
    private OrderDuplicateGuardService orderDuplicateGuardService;
    @Resource
    private Cache<Object, Object> caffeineBuilder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRecord submit(OrderSubmitReq req) {
        orderDuplicateGuardService.guardSubmit(DEFAULT_USER_ID);
        OrderEntity order = new OrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setUserId(DEFAULT_USER_ID);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setVersion(0);
        order.setGoodsAmount(defaultValue(req.getGoodsAmount()));
        order.setDiscountAmount(defaultValue(req.getDiscountAmount()));
        order.setPayAmount(defaultValue(req.getPayAmount()));
        order.setPayChannel("UNPAID");
        order.setSourceType(OrderSourceTypeEnum.NORMAL.getCode());
        order.setAddressId(req.getAddressId());
        order.setConsignee(req.getConsignee());
        order.setMobile(req.getMobile());
        order.setFullAddress(req.getAddress());
        order.setCouponId(req.getCouponId());
        order.setCouponTitle(req.getCouponTitle());
        orderMapper.insert(order);

        for (OrderItemPayload item : safeItems(req.getItems())) {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(order.getOrderNo());
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName());
            orderItem.setPrice(defaultValue(item.getPrice()));
            orderItem.setQuantity(item.getQuantity() == null ? 1 : item.getQuantity());
            orderItem.setTotalAmount(orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
            orderItemMapper.insert(orderItem);
        }

        if (req.getCouponId() != null) {
            couponService.consume(DEFAULT_USER_ID, req.getCouponId(), defaultValue(req.getGoodsAmount()));
        }
        orderTimeoutService.registerOrderTimeout(order.getOrderNo(), System.currentTimeMillis() + 30L * 60L * 1000L);
        OrderRecord record = toRecord(order, listItems(order.getId()));
        putValueCache(detailCacheKey(order.getId()), record, DETAIL_CACHE_BASE_MILLIS);
        invalidateListCache();
        return record;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OrderRecord> list() {
        String cacheKey = ORDER_LIST_CACHE_KEY;
        CacheValue<List<OrderRecord>> cached = (CacheValue<List<OrderRecord>>) caffeineBuilder.getIfPresent(cacheKey);
        if (isCacheAlive(cached)) {
            return cached.getValue();
        }
        synchronized (lockFor(cacheKey)) {
            CacheValue<List<OrderRecord>> doubleChecked = (CacheValue<List<OrderRecord>>) caffeineBuilder.getIfPresent(cacheKey);
            if (isCacheAlive(doubleChecked)) {
                return doubleChecked.getValue();
            }
            List<OrderEntity> orders = orderMapper.selectList(
                new LambdaQueryWrapper<OrderEntity>().orderByDesc(OrderEntity::getId)
            );
            List<OrderRecord> result = new ArrayList<>();
            for (OrderEntity order : orders) {
                result.add(toRecord(order, listItems(order.getId())));
            }
            putValueCache(cacheKey, result, LIST_CACHE_BASE_MILLIS);
            return result;
        }
    }

    @Override
    public OrderPageResult page(Long lastId, Integer size) {
        int pageSize = normalizePageSize(size);
        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<OrderEntity>()
            .orderByDesc(OrderEntity::getId)
            .last("limit " + pageSize);
        if (lastId != null && lastId > 0) {
            wrapper.lt(OrderEntity::getId, lastId);
        }
        List<OrderEntity> orders = orderMapper.selectList(wrapper);
        List<OrderRecord> records = new ArrayList<>();
        for (OrderEntity order : orders) {
            records.add(toRecord(order, listItems(order.getId())));
        }
        OrderPageResult result = new OrderPageResult();
        result.setRecords(records);
        result.setHasMore(records.size() == pageSize);
        result.setNextLastId(records.isEmpty() ? null : records.get(records.size() - 1).getOrderId());
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OrderRecord detail(Long orderId) {
        String cacheKey = detailCacheKey(orderId);
        CacheValue<OrderRecord> cached = (CacheValue<OrderRecord>) caffeineBuilder.getIfPresent(cacheKey);
        if (isCacheAlive(cached)) {
            return cached.isNullValue() ? null : cached.getValue();
        }
        synchronized (lockFor(cacheKey)) {
            CacheValue<OrderRecord> doubleChecked = (CacheValue<OrderRecord>) caffeineBuilder.getIfPresent(cacheKey);
            if (isCacheAlive(doubleChecked)) {
                return doubleChecked.isNullValue() ? null : doubleChecked.getValue();
            }
            OrderEntity order = getEntityById(orderId);
            if (order == null) {
                putNullCache(cacheKey);
                return null;
            }
            OrderRecord record = toRecord(order, listItems(orderId));
            putValueCache(cacheKey, record, DETAIL_CACHE_BASE_MILLIS);
            return record;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRecord cancel(Long orderId) {
        OrderEntity order = getRequiredOrder(orderId);
        orderDuplicateGuardService.guardCancel(order.getOrderNo());
        orderStateMachineService.fireEvent(
            order,
            OrderEventEnum.USER_CANCEL,
            OrderOperateContext.builder()
                .operatorType(OperatorTypeEnum.USER)
                .operatorId(DEFAULT_USER_ID)
                .remark("用户取消订单")
                .operateTime(LocalDateTime.now())
                .build()
        );
        invalidateDetailCache(orderId);
        invalidateListCache();
        return detail(orderId);
    }

    @Override
    public OrderEntity getEntityById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public OrderEntity getEntityByOrderNo(String orderNo) {
        return orderMapper.selectOne(
            new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderNo, orderNo).last("limit 1")
        );
    }

    private OrderEntity getRequiredOrder(Long orderId) {
        OrderEntity order = getEntityById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private List<OrderItemEntity> listItems(Long orderId) {
        return orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderId, orderId).orderByAsc(OrderItemEntity::getId)
        );
    }

    private OrderRecord toRecord(OrderEntity order, List<OrderItemEntity> items) {
        OrderRecord record = new OrderRecord();
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setCreatedTime(order.getCreatedAt() == null ? null : order.getCreatedAt().format(FORMATTER));
        record.setOrderStatusCode(order.getOrderStatus());
        record.setOrderStatus(OrderStatusEnum.fromCode(order.getOrderStatus()).getDesc());
        record.setAddressId(order.getAddressId());
        record.setConsignee(order.getConsignee());
        record.setMobile(order.getMobile());
        record.setAddress(order.getFullAddress());
        record.setCouponId(order.getCouponId());
        record.setCouponTitle(order.getCouponTitle());
        record.setGoodsAmount(defaultValue(order.getGoodsAmount()));
        record.setDiscountAmount(defaultValue(order.getDiscountAmount()));
        record.setPayAmount(defaultValue(order.getPayAmount()));
        record.setPayChannel(order.getPayChannel());
        record.setItems(toPayloads(items));
        return record;
    }

    private List<OrderItemPayload> toPayloads(List<OrderItemEntity> items) {
        List<OrderItemPayload> payloads = new ArrayList<>();
        for (OrderItemEntity item : items) {
            OrderItemPayload payload = new OrderItemPayload();
            payload.setProductId(item.getProductId());
            payload.setProductName(item.getProductName());
            payload.setPrice(item.getPrice());
            payload.setQuantity(item.getQuantity());
            payloads.add(payload);
        }
        return payloads;
    }

    private List<OrderItemPayload> safeItems(List<OrderItemPayload> items) {
        return items == null ? new ArrayList<OrderItemPayload>() : items;
    }

    private String generateOrderNo() {
        return "SW" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String detailCacheKey(Long orderId) {
        return ORDER_DETAIL_CACHE_PREFIX + orderId;
    }

    private Object lockFor(String cacheKey) {
        return cacheLocks.computeIfAbsent(cacheKey, key -> new Object());
    }

    private long ttlWithJitter(long baseMillis) {
        return baseMillis + ThreadLocalRandom.current().nextLong(JITTER_MILLIS + 1);
    }

    private boolean isCacheAlive(CacheValue<?> value) {
        return value != null && value.getExpireAtMillis() > System.currentTimeMillis();
    }

    private void putValueCache(String key, Object value, long baseMillis) {
        caffeineBuilder.put(key, new CacheValue<>(value, false, System.currentTimeMillis() + ttlWithJitter(baseMillis)));
    }

    private void putNullCache(String key) {
        caffeineBuilder.put(key, new CacheValue<>(null, true, System.currentTimeMillis() + EMPTY_CACHE_MILLIS));
    }

    private void invalidateDetailCache(Long orderId) {
        if (orderId != null) {
            caffeineBuilder.invalidate(detailCacheKey(orderId));
        }
    }

    private void invalidateListCache() {
        caffeineBuilder.invalidate(ORDER_LIST_CACHE_KEY);
    }

    private static class CacheValue<T> {
        private final T value;
        private final boolean nullValue;
        private final long expireAtMillis;

        private CacheValue(T value, boolean nullValue, long expireAtMillis) {
            this.value = value;
            this.nullValue = nullValue;
            this.expireAtMillis = expireAtMillis;
        }

        public T getValue() {
            return value;
        }

        public boolean isNullValue() {
            return nullValue;
        }

        public long getExpireAtMillis() {
            return expireAtMillis;
        }
    }
}
