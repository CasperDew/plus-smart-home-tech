package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.Action;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.model.Sensor;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventServiceImpl implements HubEventService {
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;

    @Override
    public void handleHubEvent(HubEventAvro hubEventAvro) {
        Object payload = hubEventAvro.getPayload();

        switch (payload) {
            case DeviceAddedEventAvro event -> {
                if (!sensorRepository.existsByIdAndHubId(event.getId(), hubEventAvro.getHubId())) {
                    log.info("Добавление сенсора: {}", hubEventAvro);
                    sensorRepository.save(Sensor.builder()
                            .id(event.getId())
                            .hubId(hubEventAvro.getHubId())
                            .build());
                }
            }
            case DeviceRemovedEventAvro event -> {
                log.info("Удаление сенсора: {}", hubEventAvro);
                sensorRepository.deleteById(event.getId());
            }
            case ScenarioAddedEventAvro event -> {
                log.info("Добавление/обновление сценария: {}", hubEventAvro);
                Scenario scenario = scenarioRepository
                        .findByHubIdAndName(hubEventAvro.getHubId(), event.getName())
                        .orElse(Scenario.builder()
                                .hubId(hubEventAvro.getHubId())
                                .name(event.getName()).build());

                scenario.setActions(event.getActions()
                        .stream()
                        .collect(Collectors.toMap(DeviceActionAvro::getSensorId, a -> Action.builder()
                                .type(a.getType())
                                .value(a.getValue())
                                .build())));

                scenario.setConditions(event.getConditions()
                        .stream()
                        .collect(Collectors.toMap(ScenarioConditionAvro::getSensorId, c -> Condition.builder()
                                .type(c.getType())
                                .operation(c.getOperation())
                                .value(convertToInteger(c.getValue()))
                                .build())));

                scenarioRepository.save(scenario);
            }
            case ScenarioRemovedEventAvro event -> {
                log.info("Удаление сценария: {}", hubEventAvro);
                scenarioRepository.deleteByHubIdAndName(hubEventAvro.getHubId(), event.getName());
            }
            default -> throw new IllegalArgumentException("Неизвестный тип payload: " + payload);

        }
    }

    private Integer convertToInteger(Object value) {
        return switch (value) {
            case null -> null;
            case Integer i -> i;
            case Boolean b -> b ? 1 : 0;
            default -> throw new RuntimeException("Неизвестный тип value: " + value);
        };
    }
}
