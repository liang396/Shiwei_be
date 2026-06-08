package com.shiwei.seckill.seckill.config;

import com.shiwei.seckill.seckill.model.dto.SeckillMqMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SeckillKafkaConfig {

    @Bean
    public ProducerFactory<String, SeckillMqMessage> seckillProducerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        return new DefaultKafkaProducerFactory<>(seckillProducerConfigs(bootstrapServers));
    }

    @Bean
    public KafkaTemplate<String, SeckillMqMessage> seckillKafkaTemplate(
        ProducerFactory<String, SeckillMqMessage> seckillProducerFactory
    ) {
        return new KafkaTemplate<>(seckillProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, SeckillMqMessage> seckillConsumerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        return new DefaultKafkaConsumerFactory<>(seckillConsumerConfigs(bootstrapServers));
    }

    @Bean(name = "seckillKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SeckillMqMessage> seckillKafkaListenerContainerFactory(
        ConsumerFactory<String, SeckillMqMessage> seckillConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, SeckillMqMessage> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(seckillConsumerFactory);
        return factory;
    }

    Map<String, Object> seckillProducerConfigs(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }

    Map<String, Object> seckillConsumerConfigs(String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.shiwei.seckill.seckill.model.dto");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SeckillMqMessage.class);
        return props;
    }
}
