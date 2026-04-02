package ru.yandex.practicum.service;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class SnapshotEventServiceImpl implements SnapshotEventService {
    private final ScenarioRepository scenarioRepository;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public SnapshotEventServiceImpl(
            ScenarioRepository scenarioRepository,
            @GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {
        this.scenarioRepository = scenarioRepository;
        this.hubRouterClient = hubRouterClient;
    }

    @Override
    @Transactional(readOnly = true)
    public void handleSnapshot(SensorsSnapshotAvro snapshot) {
        scenarioRepository.findByHubId(snapshot.getHubId())
                .stream()
                .filter(scenario -> checkScenarioConditions(
                        scenario.getConditions(),
                        snapshot.getSensorsState()))
                .forEach(this::sendDeviceActions);
    }

    private boolean checkScenarioConditions(Map<String, Condition> conditions, Map<String, SensorStateAvro> sensors) {
        return conditions.entrySet().stream().allMatch(entry -> {
            SensorStateAvro state = sensors.get(entry.getKey());
            if (state == null) {
                return false;
            }
            return checkConditions(entry.getValue(), state);
        });
    }

    private boolean checkConditions(Condition condition, SensorStateAvro state) {
        Integer targetValue = condition.getValue();
        ConditionTypeAvro typeAvro = condition.getType();
        ConditionOperationAvro operationAvro = condition.getOperation();

        return switch (state.getData()) {
            case TemperatureSensorAvro temperatureSensor -> switch (typeAvro) {
                case TEMPERATURE -> checkPredicate(operationAvro, targetValue, temperatureSensor.getTemperatureC());
                default -> false;
            };
            case ClimateSensorAvro climateSensor -> switch (typeAvro) {
                case TEMPERATURE -> checkPredicate(operationAvro, targetValue, climateSensor.getTemperatureC());
                case HUMIDITY -> checkPredicate(operationAvro, targetValue, climateSensor.getHumidity());
                case CO2LEVEL -> checkPredicate(operationAvro, targetValue, climateSensor.getCo2Level());
                default -> false;
            };
            case LightSensorAvro lightSensor -> switch (typeAvro) {
                case LUMINOSITY -> checkPredicate(operationAvro, targetValue, lightSensor.getLuminosity());
                default -> false;
            };
            case MotionSensorAvro motion -> switch (typeAvro) {
                case MOTION -> checkPredicate(operationAvro, targetValue, motion.getMotion() ? 1 : 0);
                default -> false;
            };
            case SwitchSensorAvro switchSensor -> switch (typeAvro) {
                case SWITCH -> checkPredicate(operationAvro, targetValue, switchSensor.getState() ? 1 : 0);
                default -> false;
            };
            default -> throw new RuntimeException("Неизвестный сенсор: " + state.getData());
        };
    }

    private boolean checkPredicate(ConditionOperationAvro operation, Integer targetValue, Integer sensorValue) {
        return switch (operation) {
            case EQUALS -> sensorValue.equals(targetValue);
            case GREATER_THAN -> sensorValue > targetValue;
            case LOWER_THAN -> sensorValue < targetValue;
            case null -> throw new RuntimeException("Операция не найдена");
        };
    }

    private void sendDeviceActions(Scenario scenario) {
        log.info("Отправка активности устройства для сценария {}", scenario.getName());
        scenario.getActions().forEach((key, action) -> {
            hubRouterClient.handleDeviceAction(DeviceActionRequest.newBuilder()
                    .setHubId(scenario.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(DeviceActionProto.newBuilder()
                            .setSensorId(key)
                            .setType(ActionTypeProto.valueOf(action.getType().name()))
                            .setValue(action.getValue())
                            .build())
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build());
        });
    }
}
