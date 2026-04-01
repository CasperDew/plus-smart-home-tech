package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.KafkaClient;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AggregatorServiceImpl implements AggregatorService {
    private final KafkaClient kafkaClient;
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    @Override
    public void handleEvent(SensorEventAvro sensorEventAvro) {
        Optional<SensorsSnapshotAvro> sensorsSnapshotAvroOptional = updateState(sensorEventAvro);
        if (sensorsSnapshotAvroOptional.isPresent()) {
            SensorsSnapshotAvro sensorSnapshotAvro = sensorsSnapshotAvroOptional.get();

            log.info("Сохранение состояния датчика {}", sensorSnapshotAvro);

            kafkaClient.send("telemetry.snapshots.v1", sensorSnapshotAvro, sensorSnapshotAvro.getTimestamp(),
                    sensorSnapshotAvro.getHubId());
        }
    }

    private Optional<SensorsSnapshotAvro> updateState(SensorEventAvro sensorEventAvro) {
        if (!snapshots.containsKey(sensorEventAvro.getHubId())) {
            SensorsSnapshotAvro sensorsSnapshotAvro = SensorsSnapshotAvro.newBuilder()
                    .setHubId(sensorEventAvro.getHubId())
                    .setSensorsState(new HashMap<>())
                    .setTimestamp(sensorEventAvro.getTimestamp())
                    .build();
            snapshots.put(sensorEventAvro.getHubId(), sensorsSnapshotAvro);
        }

        SensorsSnapshotAvro sensorsSnapshotAvro = snapshots.get(sensorEventAvro.getHubId());

        if (sensorsSnapshotAvro.getSensorsState().containsKey(sensorEventAvro.getId())) {
            SensorStateAvro oldState = sensorsSnapshotAvro.getSensorsState().get(sensorEventAvro.getId());
            if (oldState.getTimestamp().isAfter(sensorEventAvro.getTimestamp()) ||
                    oldState.getData().equals(sensorEventAvro.getPayload())) {
                return Optional.empty();
            }
        }
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(sensorEventAvro.getTimestamp())
                .setData(sensorEventAvro.getPayload())
                .build();

        sensorsSnapshotAvro.getSensorsState().put(sensorEventAvro.getId(), newState);
        sensorsSnapshotAvro.setTimestamp(sensorEventAvro.getTimestamp());

        return Optional.of(sensorsSnapshotAvro);
    }
}
