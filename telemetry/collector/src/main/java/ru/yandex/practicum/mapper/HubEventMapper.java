package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.hub.*;

import java.util.stream.Collectors;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        switch (event.getType()) {
            case DEVICE_ADDED -> {
                DeviceAddedEvent added = (DeviceAddedEvent) event;

                builder.setPayload(
                        DeviceAddedEventAvro.newBuilder()
                                .setId(added.getId())
                                .setType(DeviceTypeAvro.valueOf(added.getDeviceType().name()))
                                .build()
                );
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEvent removed = (DeviceRemovedEvent) event;

                builder.setPayload(
                        DeviceRemovedEventAvro.newBuilder()
                                .setId(removed.getId())
                                .build()
                );
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEvent added = (ScenarioAddedEvent) event;

                builder.setPayload(
                        ScenarioAddedEventAvro.newBuilder()
                                .setName(added.getName())
                                .setConditions(added.getConditions().stream()
                                        .map(c -> ScenarioConditionAvro.newBuilder()
                                                .setSensorId(c.getSensorId())
                                                .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                                                .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()))
                                                .setValue(c.getValue())
                                                .build())
                                        .collect(Collectors.toList()))
                                .setActions(added.getActions().stream()
                                        .map(a -> DeviceActionAvro.newBuilder()
                                                .setSensorId(a.getSensorId())
                                                .setType(ActionTypeAvro.valueOf(a.getType().name()))
                                                .setValue(a.getValue())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build()
                );
            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent removed = (ScenarioRemovedEvent) event;

                builder.setPayload(
                        ScenarioRemovedEventAvro.newBuilder()
                                .setName(removed.getName())
                                .build()
                );
            }
        }
        return builder.build();
    }
}
