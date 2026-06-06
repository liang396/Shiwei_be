CREATE TABLE IF NOT EXISTS `seckill_activity` (
  `activity_id` bigint NOT NULL,
  `activity_name` varchar(64) NOT NULL,
  `activity_status` tinyint NOT NULL DEFAULT 0,
  `start_time` bigint NOT NULL,
  `end_time` bigint NOT NULL,
  `limit_per_user` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`activity_id`)
);

CREATE TABLE IF NOT EXISTS `seckill_goods` (
  `goods_id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `product_item_id` bigint NOT NULL,
  `seckill_price` decimal(10,2) NOT NULL,
  `seckill_stock` int NOT NULL DEFAULT 0,
  `available_stock` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`goods_id`)
);

CREATE TABLE IF NOT EXISTS `seckill_order` (
  `seckill_order_id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `seckill_goods_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_item_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `order_no` varchar(64) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `seckill_price` decimal(10,2) NOT NULL,
  `buy_num` int NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`seckill_order_id`)
);

CREATE TABLE IF NOT EXISTS `auth_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `phone` varchar(20) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_auth_user_username` (`username`),
  UNIQUE KEY `uk_auth_user_phone` (`phone`)
);

CREATE TABLE IF NOT EXISTS `product` (
  `product_id` bigint NOT NULL,
  `product_item_id` bigint NOT NULL,
  `product_name` varchar(128) NOT NULL,
  `product_image` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock` int NOT NULL DEFAULT 0,
  `sales` int NOT NULL DEFAULT 0,
  `popularity` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`product_id`)
);

CREATE TABLE IF NOT EXISTS `t_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL DEFAULT 1,
  `order_status` tinyint NOT NULL DEFAULT 0,
  `goods_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `discount_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `pay_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `pay_channel` varchar(32) DEFAULT NULL,
  `source_type` tinyint NOT NULL DEFAULT 0,
  `address_id` bigint DEFAULT NULL,
  `consignee` varchar(64) DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `full_address` varchar(1024) DEFAULT NULL,
  `coupon_id` bigint DEFAULT NULL,
  `coupon_title` varchar(128) DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `canceled_time` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `order_status`),
  KEY `idx_user_status_created_id` (`user_id`, `order_status`, `created_at`, `id`),
  KEY `idx_created_at` (`created_at`)
);

CREATE TABLE IF NOT EXISTS `t_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `product_id` bigint DEFAULT NULL,
  `product_item_id` bigint DEFAULT NULL,
  `product_name` varchar(128) DEFAULT NULL,
  `product_image` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `quantity` int NOT NULL DEFAULT 1,
  `total_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_order_no` (`order_no`)
);

CREATE TABLE IF NOT EXISTS `t_order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `source_status` tinyint NOT NULL,
  `target_status` tinyint NOT NULL,
  `event_code` varchar(32) NOT NULL,
  `trigger_type` varchar(32) DEFAULT NULL,
  `operator_type` varchar(32) NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `request_id` varchar(64) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_status_log_order_id` (`order_id`)
);

CREATE TABLE IF NOT EXISTS `t_pay_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `pay_order_no` varchar(64) NOT NULL,
  `pay_channel` varchar(32) NOT NULL,
  `trade_status` varchar(32) NOT NULL,
  `pay_amount` decimal(10,2) NOT NULL DEFAULT 0.00,
  `notify_payload` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_order_no` (`pay_order_no`)
);

CREATE TABLE IF NOT EXISTS `t_order_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_key` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `payload` text,
  `send_status` tinyint NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_outbox_status` (`send_status`, `created_at`)
);

CREATE TABLE IF NOT EXISTS `t_message_processed` (
  `event_id` varchar(64) NOT NULL,
  `processed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`)
);

CREATE TABLE IF NOT EXISTS `t_message_dead_letter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_key` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `payload` text,
  `fail_reason` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
