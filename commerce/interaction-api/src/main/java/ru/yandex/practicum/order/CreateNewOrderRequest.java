package ru.yandex.practicum.order;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.warehouse.AddressDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNewOrderRequest {

    @NotNull
    private ShoppingCartDto shoppingCartDto;

    @NotNull
    private AddressDto deliveryAddress;
}
