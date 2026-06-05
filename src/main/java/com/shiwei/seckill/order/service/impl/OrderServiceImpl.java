package com.shiwei.seckill.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.alibaba.csp.sentinel.Entry;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.common.monitoring.OrderMetrics;
import com.shiwei.seckill.common.sentinel.SentinelSupport;
import com.shiwei.seckill.order.cache.OrderCacheNotifier;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Long DEFAULT_USER_ID = 1L;
    private static final String ORDER_DETAIL_CACHE_PREFIX = "order:detail:";
    private static final String ORDER_LIST_CACHE_KEY = "order:list:user:1";
    private static final DateTimeFormatter CURSOR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long DETAIL_CACHE_BASE_MILLIS = 5 * 60 * 1000L;
    private static final long LIST_CACHE_BASE_MILLIS = 2 * 60 * 1000L;
    private static final long EMPTY_CACHE_MILLIS = 60 * 1000L;
    private static final long JITTER_MILLIS = 60 * 1000L;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String NULL_MARKER = "__NULL__";

    private final ConcurrentHashMap<String, Object> cacheLocks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private OrderCacheNotifier orderCacheNotifier;
    @Resource
    private SentinelSupport sentinelSupport;
    @Resource
    private OrderMetrics orderMetrics;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderRecord submit(OrderSubmitReq req) {
        Entry entry = sentinelSupport.enter("order.submit");
        try {
        orderDuplicateGuardService.guardSubmit(DEFAULT_USER_ID);
        OrderEntity order = new OrderEntity();
        order.setOrderNo(generateOrderNo());
        order.setUserId(DEFAULT_USER_ID);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
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
        refreshDetailCaches(record);
        invalidateListCaches();
        orderMetrics.markSubmit();
        return record;
        } finally {
            entry.exit();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OrderRecord> list() {
        String cacheKey = ORDER_LIST_CACHE_KEY;
        CacheValue<List<OrderRecord>> cached = (CacheValue<List<OrderRecord>>) caffeineBuilder.getIfPresent(cacheKey);
        if (isCacheAlive(cached)) {
            return cached.getValue();
        }
        List<OrderRecord> redisCached = readListFromRedis(cacheKey);
        if (redisCached != null) {
            putValueCache(cacheKey, redisCached, LIST_CACHE_BASE_MILLIS);
            return redisCached;
        }
        synchronized (lockFor(cacheKey)) {
            CacheValue<List<OrderRecord>> doubleChecked = (CacheValue<List<OrderRecord>>) caffeineBuilder.getIfPresent(cacheKey);
            if (isCacheAlive(doubleChecked)) {
                return doubleChecked.getValue();
            }
            List<OrderRecord> redisDoubleChecked = readListFromRedis(cacheKey);
            if (redisDoubleChecked != null) {
                putValueCache(cacheKey, redisDoubleChecked, LIST_CACHE_BASE_MILLIS);
                return redisDoubleChecked;
            }
            List<OrderEntity> orders = orderMapper.selectList(
                new LambdaQueryWrapper<OrderEntity>().orderByDesc(OrderEntity::getId)
            );
            Map<Long, List<OrderItemEntity>> itemsMap = listItemsMap(orders);
            List<OrderRecord> result = new ArrayList<>();
            for (OrderEntity order : orders) {
                result.add(toRecord(order, itemsMap.getOrDefault(order.getId(), Collections.emptyList())));
            }
            putValueCache(cacheKey, result, LIST_CACHE_BASE_MILLIS);
            writeListToRedis(cacheKey, result, LIST_CACHE_BASE_MILLIS);
            return result;
        }
    }

    @Override
    public OrderPageResult page(Long lastId, Integer size, String lastCreatedTime) {
        int pageSize = normalizePageSize(size);
        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<OrderEntity>()
            .orderByDesc(OrderEntity::getCreatedAt)
            .orderByDesc(OrderEntity::getId)
            .last("limit " + pageSize);
        LocalDateTime cursorTime = parseCursorTime(lastCreatedTime);
        if (cursorTime != null && lastId != null && lastId > 0) {
            wrapper.and(query -> query
                .lt(OrderEntity::getCreatedAt, cursorTime)
                .or(orQuery -> orQuery.eq(OrderEntity::getCreatedAt, cursorTime).lt(OrderEntity::getId, lastId))
            );
        } else if (lastId != null && lastId > 0) {
            wrapper.lt(OrderEntity::getId, lastId);
        }
        List<OrderEntity> orders = orderMapper.selectList(wrapper);
        Map<Long, List<OrderItemEntity>> itemsMap = listItemsMap(orders);
        List<OrderRecord> records = new ArrayList<>();
        for (OrderEntity order : orders) {
            records.add(toRecord(order, itemsMap.getOrDefault(order.getId(), Collections.emptyList())));
        }
        OrderPageResult result = new OrderPageResult();
        result.setRecords(records);
        result.setHasMore(records.size() == pageSize);
        if (!orders.isEmpty()) {
            OrderEntity lastOrder = orders.get(orders.size() - 1);
            result.setNextLastId(lastOrder.getId());
            result.setNextCreatedTime(lastOrder.getCreatedAt() == null ? null : lastOrder.getCreatedAt().format(CURSOR_FORMATTER));
        } else {
            result.setNextLastId(null);
            result.setNextCreatedTime(null);
        }
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
        RedisDetailValue redisCached = readDetailFromRedis(cacheKey);
        if (redisCached != null) {
            if (redisCached.nullValue) {
                putNullCache(cacheKey);
                return null;
            }
            putValueCache(cacheKey, redisCached.record, DETAIL_CACHE_BASE_MILLIS);
            return redisCached.record;
        }
        synchronized (lockFor(cacheKey)) {
            CacheValue<OrderRecord> doubleChecked = (CacheValue<OrderRecord>) caffeineBuilder.getIfPresent(cacheKey);
            if (isCacheAlive(doubleChecked)) {
                return doubleChecked.isNullValue() ? null : doubleChecked.getValue();
            }
            RedisDetailValue redisDoubleChecked = readDetailFromRedis(cacheKey);
            if (redisDoubleChecked != null) {
                if (redisDoubleChecked.nullValue) {
                    putNullCache(cacheKey);
                    return null;
                }
                putValueCache(cacheKey, redisDoubleChecked.record, DETAIL_CACHE_BASE_MILLIS);
                return redisDoubleChecked.record;
            }
            OrderEntity order = getEntityById(orderId);
            if (order == null) {
                putNullCache(cacheKey);
                writeNullToRedis(cacheKey);
                return null;
            }
            OrderRecord record = toRecord(order, listItems(orderId));
            putValueCache(cacheKey, record, DETAIL_CACHE_BASE_MILLIS);
            writeDetailToRedis(cacheKey, record, DETAIL_CACHE_BASE_MILLIS);
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
        invalidateDetailCaches(orderId);
        invalidateListCaches();
        orderMetrics.markCancel();
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

    private Map<Long, List<OrderItemEntity>> listItemsMap(List<OrderEntity> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> orderIds = new ArrayList<>();
        for (OrderEntity order : orders) {
            orderIds.add(order.getId());
        }
        List<OrderItemEntity> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItemEntity>().in(OrderItemEntity::getOrderId, orderIds).orderByAsc(OrderItemEntity::getId)
        );
        Map<Long, List<OrderItemEntity>> grouped = new HashMap<>();
        for (OrderItemEntity item : items) {
            grouped.computeIfAbsent(item.getOrderId(), key -> new ArrayList<>()).add(item);
        }
        return grouped;
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

    private LocalDateTime parseCursorTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), CURSOR_FORMATTER);
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

    private void invalidateDetailCaches(Long orderId) {
        if (orderId != null) {
            String cacheKey = detailCacheKey(orderId);
            caffeineBuilder.invalidate(cacheKey);
            deleteRedisKey(cacheKey);
            orderCacheNotifier.invalidate(cacheKey);
        }
    }

    private void invalidateListCaches() {
        caffeineBuilder.invalidate(ORDER_LIST_CACHE_KEY);
        deleteRedisKey(ORDER_LIST_CACHE_KEY);
        orderCacheNotifier.invalidate(ORDER_LIST_CACHE_KEY);
    }

    private void refreshDetailCaches(OrderRecord record) {
        putValueCache(detailCacheKey(record.getOrderId()), record, DETAIL_CACHE_BASE_MILLIS);
        writeDetailToRedis(detailCacheKey(record.getOrderId()), record, DETAIL_CACHE_BASE_MILLIS);
        orderCacheNotifier.invalidate(detailCacheKey(record.getOrderId()));
    }

    private void writeDetailToRedis(String key, OrderRecord record, long baseMillis) {
        if (stringRedisTemplate == null || record == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(record),
                ttlWithJitter(baseMillis), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException ignored) {
        }
    }

    private void writeListToRedis(String key, List<OrderRecord> records, long baseMillis) {
        if (stringRedisTemplate == null || records == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(records),
                ttlWithJitter(baseMillis), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException ignored) {
        }
    }

    private void writeNullToRedis(String key) {
        if (stringRedisTemplate == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key, NULL_MARKER, EMPTY_CACHE_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private RedisDetailValue readDetailFromRedis(String key) {
        if (stringRedisTemplate == null) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (NULL_MARKER.equals(value)) {
            return new RedisDetailValue(null, true);
        }
        try {
            return new RedisDetailValue(objectMapper.readValue(value, OrderRecord.class), false);
        } catch (IOException ignored) {
            return null;
        }
    }

    private List<OrderRecord> readListFromRedis(String key) {
        if (stringRedisTemplate == null) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || NULL_MARKER.equals(value)) {
            return null;
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<OrderRecord>>() {});
        } catch (IOException ignored) {
            return null;
        }
    }

    private void deleteRedisKey(String key) {
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete(key);
        }
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

    private static class RedisDetailValue {
        private final OrderRecord record;
        private final boolean nullValue;

        private RedisDetailValue(OrderRecord record, boolean nullValue) {
            this.record = record;
            this.nullValue = nullValue;
        }
    }
}
