ALTER TABLE `product` ADD COLUMN `product_images` text;
ALTER TABLE `product` ADD COLUMN `description` text;
ALTER TABLE `product` ADD COLUMN `detail_content` text;
ALTER TABLE `product` ADD COLUMN `category` varchar(64) DEFAULT NULL;
ALTER TABLE `product` ADD COLUMN `subcategory` varchar(64) DEFAULT NULL;
ALTER TABLE `product` ADD COLUMN `theme` varchar(64) DEFAULT NULL;
ALTER TABLE `product` ADD COLUMN `featured` tinyint NOT NULL DEFAULT 0;

ALTER TABLE `seckill_activity` ADD COLUMN `activity_desc` varchar(255) DEFAULT NULL;

ALTER TABLE `seckill_goods` ADD COLUMN `sort_num` int NOT NULL DEFAULT 0;
ALTER TABLE `seckill_goods` ADD COLUMN `status` tinyint NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS `cart_item` (
  `cart_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL DEFAULT 1,
  `product_id` bigint NOT NULL,
  `product_name` varchar(128) DEFAULT NULL,
  `product_image` varchar(255) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `quantity` int NOT NULL DEFAULT 1,
  `checked` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cart_id`),
  UNIQUE KEY `uk_cart_user_product` (`user_id`, `product_id`)
);
