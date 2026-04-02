package ru.yandex.practicum.kafka;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

@Slf4j
@Component
public class KafkaClient {
    private final Producer<String, SpecificRecordBase> producer;

    public KafkaClient(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.producer.key-serializer}") String keySerializerClass,
            @Value("${spring.kafka.producer.value-serializer}") String valueSerializerClass
    ) {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass);

        producer = new KafkaProducer<>(config);
    }

    public void send(String topic, SpecificRecordBase record, Instant timestamp, String key) {
        ProducerRecord<String, SpecificRecordBase> producerRecord =
                new ProducerRecord<>(topic, null, timestamp.toEpochMilli(), key, record);
        try {
            producer.send(producerRecord);
            producer.flush();
        } catch (Exception ex) {
            log.error("Ошибка чтения данных ", ex);
        }
    }

    @PreDestroy
    public void closeConnection() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}

