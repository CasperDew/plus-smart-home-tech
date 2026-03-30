package ru.yandex.practicum.mapper;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;

@Component
public class HubEventMapper {

    public static HubEventAvro toAvro(HubEventProto hubEvent) {
        HubEventProto.PayloadCase payloadCase = hubEvent.getPayloadCase();
        SpecificRecordBase payload;

        switch (payloadCase) {
            case DEVICE_ADDED -> {
                DeviceAddedEventProto added = hubEvent.getDeviceAdded();

                payload = DeviceAddedEventAvro.newBuilder()
                        .setId(added.getId())
                        .setType(DeviceTypeAvro.valueOf(added.getType().name()))
                        .build();
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEventProto removed = hubEvent.getDeviceRemoved();

                payload = DeviceRemovedEventAvro.newBuilder()
                        .setId(removed.getId())
                        .build();
            }
            case SCENARIO_ADDED -> {
                ScenarioAddedEventProto added = hubEvent.getScenarioAdded();

                List<ScenarioConditionAvro> scenarioConditionAvroList = added.getConditionList().stream()
                        .map(c -> {
                                    ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                                            .setSensorId(c.getSensorId())
                                            .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                                            .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()));

                                    switch (c.getValueCase()) {
                                        case ScenarioConditionProto.ValueCase.BOOL_VALUE -> builder.setValue(c.getBoolValue());
                                        case ScenarioConditionProto.ValueCase.INT_VALUE -> builder.setValue(c.getIntValue());
                                    }

                                    return builder.build();
                                }
                        ).toList();

                List<DeviceActionAvro> deviceActionAvroList = added.getActionList().stream()
                        .map(a -> DeviceActionAvro.newBuilder()
                                .setSensorId(a.getSensorId())
                                .setType(ActionTypeAvro.valueOf(a.getType().name()))
                                .setValue(a.getValue())
                                .build()).toList();

                payload = ScenarioAddedEventAvro.newBuilder()
                        .setName(added.getName())
                        .setConditions(scenarioConditionAvroList)
                        .setActions(deviceActionAvroList)
                        .build();

            }
            case SCENARIO_REMOVED -> {
                ScenarioRemovedEventProto removed = hubEvent.getScenarioRemoved();

                payload = ScenarioRemovedEventAvro.newBuilder()
                        .setName(removed.getName())
                        .build();
            }
            default -> {
                throw new IllegalArgumentException("Неизвестный тип события: " + payloadCase);
            }
        }
        return HubEventAvro.newBuilder()
                .setHubId(hubEvent.getHubId())
                .setTimestamp(
                        Instant.ofEpochSecond(
                                hubEvent.getTimestamp().getSeconds(),
                                hubEvent.getTimestamp().getNanos()
                        )
                )
                .setPayload(payload)
                .build();
    }
}
