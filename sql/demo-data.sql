-- Import after schema.sql

DELETE FROM `seckill_goods`;
DELETE FROM `seckill_activity`;
DELETE FROM `seckill_order`;
DELETE FROM `auth_user`;
DELETE FROM `product`;

INSERT INTO `seckill_activity`
(`activity_id`, `activity_name`, `activity_status`, `start_time`, `end_time`, `limit_per_user`)
VALUES
(1, '618 Special Price', 1, 1717400000000, 1917400000000, 1);

INSERT INTO `seckill_goods`
(`goods_id`, `activity_id`, `product_id`, `product_item_id`, `seckill_price`, `seckill_stock`, `available_stock`)
VALUES
(1, 1, 1001, 2001, 59.90, 20, 20);

INSERT INTO `auth_user`
(`user_id`, `username`, `password`, `nickname`, `phone`, `created_at`)
VALUES
(1, 'demo', '$2a$10$ebh25P8o8S9T3lJxjN3fcOX88gu4cWq3rZ0w0lY0xQ0zP8A0M6Hde', '演示用户', '13800138000', NOW());
INSERT INTO `product`
(`product_id`, `product_item_id`, `product_name`, `product_image`, `price`, `stock`, `sales`, `popularity`)
VALUES
(1001, 2001, 'Special Mango Box', 'demo.png', 129.00, 99, 2680, 96);
