package ru.yandex.practicum.service;

import ru.yandex.practicum.cart.ChangeProductQuantityRequest;
import ru.yandex.practicum.cart.ShoppingCartDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ShoppingCartService {
    ShoppingCartDto getCart(String username);

    ShoppingCartDto addProducts(String username, Map<UUID, Long> products);

    void deactivate(String username);

    ShoppingCartDto removeProducts(String username, List<UUID> productId);

    ShoppingCartDto changeQuantity(String username, ChangeProductQuantityRequest request);


}
