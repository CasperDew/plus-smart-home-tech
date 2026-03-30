package ru.yandex.practicum.mapper;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
public class SensorEventMapper {
    public static SensorEventAvro toAvro(SensorEventProto sensorEvent) {
        SensorEventProto.PayloadCase payloadCase = sensorEvent.getPayloadCase();
        SpecificRecordBase payload;

        switch (payloadCase) {
            case CLIMATE_SENSOR -> {
                ClimateSensorProto climateSensorEvent = sensorEvent.getClimateSensor();
                payload = ClimateSensorAvro.newBuilder()
                        .setTemperatureC(climateSensorEvent.getTemperatureC())
                        .setHumidity(climateSensorEvent.getHumidity())
                        .setCo2Level(climateSensorEvent.getCo2Level())
                        .build();
            }
            case LIGHT_SENSOR -> {
                LightSensorProto lightSensorEvent = sensorEvent.getLightSensor();
                payload = LightSensorAvro.newBuilder()
                        .setLinkQuality(lightSensorEvent.getLinkQuality())
                        .setLuminosity(lightSensorEvent.getLuminosity())
                        .build();
            }
            case MOTION_SENSOR -> {
                MotionSensorProto motionSensorEvent = sensorEvent.getMotionSensor();
                payload = MotionSensorAvro.newBuilder()
                        .setLinkQuality(motionSensorEvent.getLinkQuality())
                        .setMotion(motionSensorEvent.getMotion())
                        .setVoltage(motionSensorEvent.getVoltage())
                        .build();
            }
            case SWITCH_SENSOR -> {
                SwitchSensorProto switchSensorEvent = sensorEvent.getSwitchSensor();
                payload = SwitchSensorAvro.newBuilder()
                        .setState(switchSensorEvent.getState())
                        .build();
            }
            case TEMPERATURE_SENSOR -> {
                TemperatureSensorProto temperatureSensorEvent = sensorEvent.getTemperatureSensor();
                payload = TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(temperatureSensorEvent.getTemperatureC())
                        .setTemperatureF(temperatureSensorEvent.getTemperatureF())
                        .build();
            }
            default -> {
                throw new IllegalArgumentException("Неизвестный тип события: " + payloadCase);
            }
        }
        return SensorEventAvro.newBuilder()
                .setId(sensorEvent.getId())
                .setHubId(sensorEvent.getHubId())
                .setTimestamp(
                        Instant.ofEpochSecond(
                                sensorEvent.getTimestamp().getSeconds(),
                                sensorEvent.getTimestamp().getNanos()
                        )
                )
                .setPayload(payload)
                .build();
    }
}