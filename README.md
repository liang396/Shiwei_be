# ShiWei Backend

一个围绕订单流转、秒杀削峰、缓存优化与异步解耦构建的商城后端项目。

## 项目简介

项目以电商交易主链路为核心，覆盖普通订单、秒杀订单、购物车、优惠券、地址、用户资料等基础模块，重点实现了订单状态流转、并发一致性控制、缓存优化、超时取消与异步事件处理等能力。

## 技术栈

- Java 8
- Spring Boot 2.7
- MyBatis-Plus
- MySQL 8
- Redis
- Kafka
- Caffeine
- Maven

## 核心能力

### 1. 订单状态流转

普通订单通过状态机统一管理状态变化，核心流转规则包括：

- `PENDING_PAY -> PAY_SUCCESS -> PAID`
- `PENDING_PAY -> USER_CANCEL -> CANCELED`
- `PENDING_PAY -> PAY_TIMEOUT -> CANCELED`

所有状态变更统一经过状态机服务，避免业务代码直接修改订单状态。

### 2. 并发一致性控制

订单状态更新采用两层控制：

- 业务层：状态机校验合法流转
- 数据层：`order_status + version` 乐观锁更新

可以避免以下问题：

- 支付成功回调与超时取消同时触发
- 重复支付回调导致状态重复推进
- 用户短时间重复取消同一订单

### 3. 高并发前置削峰

在下单和取消订单场景中，使用 `Redis + Lua` 做短时防重：

- 防止重复提交订单
- 防止重复取消订单

该层只负责前置削峰，不替代数据库状态校验。

### 4. 缓存优化

订单查询使用 `Caffeine` 本地缓存，并处理了三类经典问题：

- 缓存穿透：不存在的订单写入短 TTL 空值缓存
- 缓存击穿：热点缓存重建时按 key 本地互斥
- 缓存雪崩：缓存 TTL 引入随机抖动

### 5. 超时取消

待支付订单创建后写入 `Redis ZSet`，后台定时任务扫描到期订单并触发超时取消流转。

### 6. 异步解耦

订单状态变更后，通过 `Outbox + Kafka` 处理旁路逻辑：

1. 同事务写入 `t_order_outbox`
2. 定时任务批量扫描待发送事件
3. 推送 Kafka
4. 消费端处理返券等后续动作

### 7. 分页与查询性能

订单列表与商品列表均使用游标分页接口，避免深分页 `offset` 带来的性能问题：

- `GET /order/page?lastId=&size=`
- `GET /product/page?lastId=&size=`

## 模块划分

### `order`

订单创建、状态流转、超时取消、分页查询、缓存控制。

### `pay`

模拟支付回调接入，通过支付成功事件推进订单状态机。

### `seckill`

秒杀活动、库存预扣、Lua 校验、Kafka 异步订单创建。

### `promotion`

优惠券领取、核销、取消返还。

### `cart / address / profile / auth`

商城基础能力支持模块。

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

### 4. 本地配置

仓库默认不提交 `src/main/resources/application.yml`。请自行创建本地配置文件，并补充：

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

## 当前实现重点

这个项目当前重点放在以下几类问题的工程化处理：

- 订单状态流转建模
- 并发状态一致性
- Redis 前置防重削峰
- 缓存三大问题处理
- MySQL 深分页优化
- Outbox + Kafka 异步解耦
