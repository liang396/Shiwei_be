# ShiWei Backend

> 一个围绕 **订单流转、秒杀削峰、缓存优化、异步解耦** 打造的商城后端项目。  
> 核心目标不是“把接口堆出来”，而是把一条更接近真实交易系统的主链路做扎实。

## 项目亮点

- 基于 **状态机** 统一管理订单状态流转，约束 `待支付 -> 已支付 / 已取消` 等核心跳转
- 使用 **MySQL 乐观锁** 保证并发场景下订单状态一致性，避免支付成功与超时取消冲突
- 使用 **Redis + Lua** 实现重复下单、重复取消的前置防重削峰
- 使用 **Redis ZSet + 定时任务** 实现待支付订单超时关闭
- 使用 **Outbox + Kafka** 解耦返券、通知等旁路流程，并支持失败重试
- 使用 **Caffeine 本地缓存** 优化高频查单，同时处理缓存穿透、击穿、雪崩问题
- 使用 **游标分页** 替代深分页 `offset` 查询，降低大数据量翻页成本
- 商品列表与订单列表均已完成分页化改造，不再一次性全量拉取

---

## 技术栈

- Java 8
- Spring Boot 2.7
- MyBatis-Plus
- MySQL 8
- Redis
- Kafka
- Caffeine
- Maven

---

## 核心业务设计

### 1. 订单流转主链路

普通订单围绕以下事件推进：

- 提交订单
- 模拟支付回调
- 支付超时取消
- 用户主动取消
- 异步返券 / 事件通知

订单状态通过固定规则流转：

- `PENDING_PAY -> PAY_SUCCESS -> PAID`
- `PENDING_PAY -> USER_CANCEL -> CANCELED`
- `PENDING_PAY -> PAY_TIMEOUT -> CANCELED`

所有状态修改统一走状态机服务，而不是在业务代码里直接改状态字段。

### 2. 并发一致性

为避免交易链路中出现状态错乱，项目采用两层保护：

- 业务层：状态机校验非法流转
- 数据层：`order_status + version` 乐观锁更新

这可以避免如下典型问题：

- 支付成功回调和超时取消同时触发
- 重复回调导致订单重复支付
- 用户短时间内重复取消同一订单

### 3. 缓存与性能优化

订单查询缓存不是简单“加个缓存”，而是补齐了三类经典问题：

- 缓存穿透：不存在订单写入短 TTL 空值缓存
- 缓存击穿：热点缓存重建时按 key 本地互斥
- 缓存雪崩：缓存 TTL 引入随机抖动

同时，列表查询使用游标分页接口：

- `GET /order/page?lastId=&size=`
- `GET /product/page?lastId=&size=`

相比传统 `limit offset`，这种方式更适合数据量增长后的持续翻页场景。

### 4. 异步解耦

订单状态变更后，主链路不会直接堆叠后续动作，而是：

1. 同事务写入 `t_order_outbox`
2. 定时任务批量扫描 Outbox
3. 推送 Kafka 事件
4. 消费端处理返券等旁路逻辑

这样可以把主交易链路压短，同时提升系统吞吐能力和可扩展性。

---

## 模块概览

### `order`

负责订单创建、状态流转、超时取消、分页查询、缓存控制。

### `pay`

负责**模拟支付回调接入**。  
项目重点不在支付渠道本身，而在“支付成功事件如何推进订单状态机”。

### `seckill`

负责秒杀活动、库存预扣、Kafka 异步创建订单等高并发链路。

### `promotion`

负责优惠券领取、核销、取消返还。

### `cart / address / profile / auth`

负责基础电商配套能力，支持完整演示流程。

---

## 数据库表

订单流转相关核心表：

- `t_order`
- `t_order_item`
- `t_order_status_log`
- `t_pay_log`
- `t_order_outbox`

秒杀相关表：

- `seckill_activity`
- `seckill_goods`
- `seckill_order`

初始化脚本：

- [sql/schema.sql](D:/拾味商城/shiwei-seckill-backend/sql/schema.sql)
- [sql/demo-data.sql](D:/拾味商城/shiwei-seckill-backend/sql/demo-data.sql)

---

## 本地启动

### 1. 环境准备

- JDK 8
- Maven 3.8+
- MySQL 8
- Redis
- Kafka（可选，未启动时异步链路会报 broker 不可用）

### 2. 创建数据库

项目默认连接：

```sql
CREATE DATABASE shiwei DEFAULT CHARACTER SET utf8mb4;
```

### 3. 导入 SQL

按顺序执行：

1. `sql/schema.sql`
2. `sql/demo-data.sql`

### 4. 配置本地环境

仓库默认 **不提交** `src/main/resources/application.yml`，避免泄露本地敏感配置。  
请自行创建本地配置文件，并补充以下内容：

- MySQL 连接地址
- Redis 地址与密码
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

---

## 关键接口示例

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
GET /order/page?lastId=12&size=6
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

---

## 为什么这个项目适合面试展示

这个项目的重点不只是“商城接口开发”，而是通过一个可运行的后端，展示了下面这些更偏工程和系统设计的能力：

- 如何设计订单状态机
- 如何处理交易并发一致性
- 如何做缓存防穿透 / 击穿 / 雪崩
- 如何避免 MySQL 深分页性能问题
- 如何通过 Outbox + Kafka 进行异步解耦
- 如何用 Redis + Lua 进行高并发前置削峰

如果你在找一个能同时讲 **业务链路、并发控制、缓存设计、异步架构、性能优化** 的后端项目，这个仓库就是为这个目标构建的。
