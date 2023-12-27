package com.osstelecom.db.inventory.manager.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.osstelecom.db.inventory.manager.resources.History;


@Configuration
public class KafkaConfiguration {

    public static final String KAFKA_LOCAL_SERVER_CONFIG = "10.200.20.222:9092";
    public static final String GROUP_ID = "netcompass";
    public static final String TOPIC_ID = "netcompass-history";

    @Bean
    public ProducerFactory<String, History> producerFactory() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_LOCAL_SERVER_CONFIG);
        configMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configMap);
    }

    @Bean
    public KafkaTemplate<String, History> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
