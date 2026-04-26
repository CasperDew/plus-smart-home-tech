package ru.yandex.practicum.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DimensionDto {

    @NotNull
    @Min(value = 1)
    private Double width;

    @NotNull
    @Min(value = 1)
    private Double height;

    @NotNull
    @Min(value = 1)
    private Double depth;
}