package com.shiwei.seckill.common.cache;

import com.shiwei.seckill.order.cache.OrderCacheInvalidationListener;
import com.shiwei.seckill.order.config.OrderCacheConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                       OrderCacheInvalidationListener orderCacheInvalidationListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new MessageListenerAdapter(orderCacheInvalidationListener),
            new ChannelTopic(OrderCacheConstants.ORDER_CACHE_INVALIDATE_CHANNEL));
        return container;
    }
}
