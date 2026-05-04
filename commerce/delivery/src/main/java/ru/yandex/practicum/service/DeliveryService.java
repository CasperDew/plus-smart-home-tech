package ru.yandex.practicum.service;

import ru.yandex.practicum.delivery.DeliveryDto;
import ru.yandex.practicum.delivery.NewDeliveryRequestDto;
import ru.yandex.practicum.order.OrderDto;

import java.util.UUID;

public interface DeliveryService {

    DeliveryDto createDelivery(NewDeliveryRequestDto request);

    DeliveryDto markDeliverySuccessful(UUID orderId);

    DeliveryDto markDeliveryPicked(UUID orderId);

    DeliveryDto markDeliveryFailed(UUID orderId);

    Double calculateDeliveryCost(OrderDto orderDto);
}
