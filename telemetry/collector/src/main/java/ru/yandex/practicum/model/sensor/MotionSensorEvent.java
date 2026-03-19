package ru.yandex.practicum.model.sensor;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MotionSensorEvent extends SensorEvent {

    @NotNull
    private Integer linkQuality;

    @NotNull
    private boolean motion;

    @NotNull
    private Integer voltage;

    @Override
    public SensorEventType getType() {
        return SensorEventType.MOTION_SENSOR_EVENT;
    }
}