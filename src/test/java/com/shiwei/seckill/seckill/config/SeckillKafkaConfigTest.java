package com.shiwei.seckill.seckill.config;

import com.shiwei.seckill.seckill.model.dto.SeckillMqMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeckillKafkaConfigTest {

    @Test
    void shouldUseJsonSerializationForSeckillMessages() {
        SeckillKafkaConfig config = new SeckillKafkaConfig();

        Map<String, Object> producerProps = config.seckillProducerConfigs("127.0.0.1:9092");
        Map<String, Object> consumerProps = config.seckillConsumerConfigs("127.0.0.1:9092");

        assertEquals(StringSerializer.class, producerProps.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertEquals(JsonSerializer.class, producerProps.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, consumerProps.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(JsonDeserializer.class, consumerProps.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals(SeckillMqMessage.class, consumerProps.get(JsonDeserializer.VALUE_DEFAULT_TYPE));
    }
}
