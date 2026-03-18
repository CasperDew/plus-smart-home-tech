package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.serializer.AvroSerializer;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${collector.topics.sensors}")
    private String sensorsTopic;

    @Value("${collector.topics.hubs}")
    private String hubsTopic;

    @Override
    public void sendSensorEvent(SensorEventAvro sensorEventAvro) {
        byte[] payload = AvroSerializer.serialize(sensorEventAvro);
        kafkaTemplate.send(sensorsTopic, sensorEventAvro.getId(), payload);
    }

    @Override
    public void sendHudEvent(HubEventAvro hubEventAvro) {
        byte[] payload = AvroSerializer.serialize(hubEventAvro);
        kafkaTemplate.send(hubsTopic, hubEventAvro.getHubId(), payload);
    }
}
