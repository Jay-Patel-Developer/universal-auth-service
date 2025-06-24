package com.ecommerce.user.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Kafka producers and topics
 */
@Configuration
@ConditionalOnProperty(name = "features.integration.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.retries:3}")
    private Integer retries;
    
    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    /**
     * Kafka admin configuration for topic management
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Create the user events topic
     */
    @Bean
    public NewTopic userEventsTopic() {
        return new NewTopic("user-events", 3, (short) 2);
    }

    /**
     * Create the user authentication events topic
     */
    @Bean
    public NewTopic userAuthEventsTopic() {
        return new NewTopic("user-auth-events", 3, (short) 2);
    }

    /**
     * Create the user GDPR events topic
     */
    @Bean
    public NewTopic userGdprEventsTopic() {
        return new NewTopic("user-gdpr-events", 3, (short) 2);
    }

    /**
     * Configure the producer factory with reliability settings
     */
    @Bean
    public ProducerFactory<String, Map<String, Object>> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability configs
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Create the Kafka template used by the publisher
     */
    @Bean
    public KafkaTemplate<String, Map<String, Object>> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
