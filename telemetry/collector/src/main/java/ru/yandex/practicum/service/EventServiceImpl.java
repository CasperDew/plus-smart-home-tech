package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.KafkaClient;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.mapper.HubEventMapper;
import ru.yandex.practicum.mapper.SensorEventMapper;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final KafkaClient kafkaClient;

    @Value("${collector.topics.sensors}")
    private String sensorsTopic;

    @Value("${collector.topics.hubs}")
    private String hubsTopic;

    @Override
    public void sendSensorEvent(SensorEvent sensorEvent) {
        SensorEventAvro sensorEventAvro = SensorEventMapper.toAvro(sensorEvent);
        log.info("Sensor Event {}", sensorEvent);
        kafkaClient.send(
                sensorsTopic,
                sensorEventAvro,
                sensorEventAvro.getTimestamp(),
                sensorEventAvro.getHubId());
    }

    @Override
    public void sendHudEvent(HubEvent hubEvent) {
        HubEventAvro hubEventAvro = HubEventMapper.toAvro(hubEvent);
        log.info("Hub event {}", hubEvent);
        kafkaClient.send(
                hubsTopic,
                hubEventAvro,
                hubEventAvro.getTimestamp(),
                hubEventAvro.getHubId()
        );
    }
}
