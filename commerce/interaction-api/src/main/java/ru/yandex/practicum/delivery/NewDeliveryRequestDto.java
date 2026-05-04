package ru.yandex.practicum.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.warehouse.AddressDto;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewDeliveryRequestDto {
    @NotBlank
    private UUID orderId;

    @NotNull
    private AddressDto toAddress;

    private AddressDto fromAddress;

    private Double totalWeight;

    private Double totalVolume;

    private Boolean fragile;
}
