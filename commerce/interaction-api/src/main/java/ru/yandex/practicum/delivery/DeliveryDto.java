package ru.yandex.practicum.delivery;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.enums.DeliveryState;
import ru.yandex.practicum.warehouse.AddressDto;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryDto {
    @NotNull
    private UUID deliveryId;

    @NotNull
    private UUID orderId;

    @NotNull
    private AddressDto fromAddress;

    @NotNull
    private AddressDto toAddress;

    @NotNull
    private DeliveryState deliveryState;
}
