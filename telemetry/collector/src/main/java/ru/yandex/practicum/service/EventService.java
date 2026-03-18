package ru.yandex.practicum.service;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

public interface EventService {
    void sendSensorEvent(SensorEventAvro sensorEventAvro);

    void sendHudEvent(HubEventAvro hubEventAvro);
}
