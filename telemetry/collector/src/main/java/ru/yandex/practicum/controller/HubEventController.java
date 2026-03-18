package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.mapper.HubEventMapper;
import ru.yandex.practicum.mapper.SensorEventMapper;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.service.EventService;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class HubEventController {
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final EventService eventService;

    @PostMapping("/sensors")
    public ResponseEntity<Void> collectSensor(@Valid @RequestBody SensorEvent sensorEvent) {
        log.info("Получение SensorEvent: {}", sensorEvent);
        eventService.sendSensorEvent(sensorEventMapper.toAvro(sensorEvent));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hubs")
    public ResponseEntity<Void> collectHub(@Valid @RequestBody HubEvent hubEvent) {
        log.info("Получение HubEvent: {}", hubEvent);
        eventService.sendHudEvent(hubEventMapper.toAvro(hubEvent));
        return ResponseEntity.ok().build();
    }
}
