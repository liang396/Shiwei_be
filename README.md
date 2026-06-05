# ShiWei Backend

一个围绕电商交易主链路构建的商城后端项目，重点覆盖订单流转、秒杀高并发、缓存一致性、异步解耦、查询性能优化与基础安全治理。

## 技术栈

- Java 8
- Spring Boot 2.7
- MyBatis-Plus
- MySQL 8
- Redis
- Redisson
- Kafka
- Caffeine
- Sentinel
- Actuator / Micrometer
- Hibernate Validator

## 核心能力

### 1. 订单状态流转

普通订单通过状态机统一管理，核心流转包括：

- `PENDING_PAY -> PAY_SUCCESS -> PAID`
- `PENDING_PAY -> USER_CANCEL -> CANCELED`
- `PENDING_PAY -> PAY_TIMEOUT -> CANCELED`

所有状态变更统一经过状态机服务，不允许业务代码直接修改订单状态。

### 2. 并发一致性控制

订单状态更新使用“状态专属 CAS”方式：

```sql
UPDATE t_order
SET order_status = ?
WHERE id = ?
  AND order_status = ?
```

可避免支付回调、超时取消、重复取消等并发场景下的状态错乱。

### 3. 高并发前置削峰

在订单提交和取消场景中，使用 `Redis + Lua` 做短时防重：

- 防止重复提交订单
- 防止重复取消订单

### 4. 超时取消机制

普通订单超时处理采用双路径：

- 主路径：`Redisson` 延迟队列
- 兜底路径：`Redis ZSet + 定时扫描`

保证处理时效性，同时保留补偿能力。

### 5. 异步解耦与可靠投递

订单状态变更后，通过 `Outbox + Kafka` 处理旁路逻辑：

1. 同事务写入 `t_order_outbox`
2. 定时任务批量扫描待发送消息
3. 推送 Kafka
4. 消费端执行业务逻辑

同时补齐了两类可靠性能力：

- `t_message_processed`：消费幂等
- `t_message_dead_letter`：失败三次进入死信

### 6. 多级缓存一致性

订单详情和订单列表使用两级缓存：

- 一级缓存：本地 `Caffeine`
- 二级缓存：Redis

写操作后通过：

- 删除 Redis 缓存
- 清理本地缓存
- Redis Pub/Sub 广播失效通知

保证多实例间缓存一致性。

### 7. 缓存三大问题处理

订单缓存处理了三类典型问题：

- 缓存穿透：不存在的订单写入短 TTL 空值缓存
- 缓存击穿：热点 key 本地互斥重建
- 缓存雪崩：缓存 TTL 引入随机抖动

### 8. 查询性能优化

订单列表和商品列表都使用游标分页，避免深分页 `offset`：

- `GET /order/page?lastCreatedTime=&lastId=&size=`
- `GET /product/page?lastId=&size=`

订单列表同时消除了 N+1 查询：先批量查询订单明细，再在内存中按 `orderId` 分组装配。

### 9. 秒杀库存一致性

秒杀链路包含：

- Redis 预扣库存
- Lua 原子校验
- Kafka 异步创建秒杀订单
- 失败时回补 Redis 库存
- 超时未支付秒杀单定时回补库存

### 10. Sentinel 限流与降级

核心资源已接入 Sentinel：

- `order.submit`
- `pay.notify`
- `seckill.submit`

同时为非核心接口 `profile.overview` 提供降级返回，高峰期优先保障交易主链路。

### 11. 基础监控

已接入：

- `spring-boot-starter-actuator`
- `Micrometer`

并补充了订单提交、订单取消等基础业务指标。

### 12. 接口安全与敏感信息保护

项目补充了基础安全治理：

- DTO 层统一参数校验（Hibernate Validator）
- 验证码发送、订单提交、地址保存、资料保存接入分钟级防刷
- 手机号、地址等敏感字段采用 AES 加密存储
- 接口返回时对手机号、地址进行脱敏处理

### 13. 分布式 ID

核心业务实体已接入雪花 ID，避免暴露订单量，并为分布式部署预留空间。  
当前雪花算法对时钟回拨做了基础保护：

- 小幅回拨（<= 5ms）等待恢复
- 大幅回拨直接拒绝发号

## 主要模块

### `order`

订单创建、状态流转、超时取消、分页查询、缓存控制、异步事件。

### `pay`

模拟支付回调接入，通过支付成功事件推进订单状态机。

### `seckill`

秒杀活动、库存预扣、Lua 校验、Kafka 异步创建订单、超时库存补偿。

### `promotion`

优惠券领取、核销、取消返还。

### `cart / address / profile / auth`

商城基础配套能力。

## 数据库表

订单相关核心表：

- `t_order`
- `t_order_item`
- `t_order_status_log`
- `t_pay_log`
- `t_order_outbox`
- `t_message_processed`
- `t_message_dead_letter`

秒杀相关表：

- `seckill_activity`
- `seckill_goods`
- `seckill_order`

初始化脚本：

- `sql/schema.sql`
- `sql/demo-data.sql`

## 本地启动

### 1. 环境准备

- JDK 8
- Maven 3.8+
- MySQL 8
- Redis
- Kafka

### 2. 创建数据库

```sql
CREATE DATABASE shiwei DEFAULT CHARACTER SET utf8mb4;
```

### 3. 导入 SQL

按顺序执行：

1. `sql/schema.sql`
2. `sql/demo-data.sql`

### 4. 配置本地环境

仓库默认不提交 `src/main/resources/application.yml`。  
请自行创建本地配置文件，并补充：

- MySQL 连接配置
- Redis 连接配置
- Kafka 地址

### 5. 启动项目

主启动类：

```text
com.shiwei.seckill.ShiweiSeckillApplication
```

默认端口：

```text
8102
```

## 接口示例

### 商品分页

```http
GET /product/page?size=6
GET /product/page?lastId=2006&size=6
```

### 提交订单

```http
POST /order/submit
Content-Type: application/json
```

```json
{
  "addressId": 1,
  "consignee": "demo",
  "mobile": "13800138000",
  "address": "Shanghai Pudong demo road",
  "couponId": 1,
  "couponTitle": "新人券",
  "goodsAmount": 99,
  "discountAmount": 10,
  "payAmount": 89,
  "items": [
    {
      "productId": 2005,
      "productName": "草莓鲜果礼盒",
      "price": 99,
      "quantity": 1
    }
  ]
}
```

### 订单分页

```http
GET /order/page?size=6
GET /order/page?lastCreatedTime=2026-06-05%2012:00:00&lastId=12&size=6
```

### 模拟支付回调

```http
POST /pay/mock/notify
```

表单字段：

```text
out_trade_no=订单号
trade_no=MOCK-20260605-001
trade_status=TRADE_SUCCESS
total_amount=89.00
```

## 当前实现重点

当前版本重点放在以下几个方面：

- 订单状态流转建模
- 并发一致性控制
- Redis 前置削峰与超时取消
- 多级缓存一致性
- MySQL 查询与分页优化
- Outbox + Kafka 异步解耦
- 秒杀库存补偿
- 基础安全治理
