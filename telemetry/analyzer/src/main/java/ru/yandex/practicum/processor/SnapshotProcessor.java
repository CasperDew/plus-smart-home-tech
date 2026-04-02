package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.SnapshotEventService;
import ru.yandex.practicum.telemetry.deserializer.SensorsSnapshotDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {

    private final SnapshotEventService snapshotEventService;

    @Value("${telemetry.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${telemetry.kafka.snapshot-processor.group}")
    private String groupId;

    @Value("${telemetry.kafka.snapshot-processor.topic}")
    private String topic;

    public void start() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorsSnapshotDeserializer.class
                .getCanonicalName());

        KafkaConsumer<String, SensorsSnapshotAvro> consumer = new KafkaConsumer<>(properties);

        try {
            consumer.subscribe(List.of(topic));
            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                        snapshotEventService.handleSnapshot(record.value());
                    }
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {

        } catch (Exception ex) {
            log.error("Ошибка обработки события датчика", ex);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }

    @Override
    public void run() {
        this.start();
    }
}
