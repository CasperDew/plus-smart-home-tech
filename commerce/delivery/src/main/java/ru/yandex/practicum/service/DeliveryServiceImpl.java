package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.delivery.DeliveryDto;
import ru.yandex.practicum.delivery.NewDeliveryRequestDto;
import ru.yandex.practicum.enums.DeliveryState;
import ru.yandex.practicum.exception.NoDeliveryFoundException;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.mapper.DeliveryToDtoMapper;
import ru.yandex.practicum.model.Address;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.order.OrderDto;
import ru.yandex.practicum.repository.DeliveryRepository;
import ru.yandex.practicum.warehouse.AddressDto;
import ru.yandex.practicum.warehouse.ShippedToDeliveryRequest;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository repository;
    private final DeliveryToDtoMapper mapper;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    @Transactional
    @Override
    public DeliveryDto createDelivery(NewDeliveryRequestDto request) {
        validateDeliveryRequest(request);

        Delivery delivery = mapper.toEntity(request);
        Delivery savedDelivery = repository.save(delivery);

        log.info("Доставка создана: deliveryId={}, orderId={}, state={}",
                savedDelivery.getDeliveryId(), savedDelivery.getOrderId(), savedDelivery.getDeliveryState());

        return mapper.toDto(savedDelivery);
    }

    @Transactional
    @Override
    public DeliveryDto markDeliverySuccessful(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);

        delivery.setDeliveryState(DeliveryState.DELIVERED);
        Delivery updatedDelivery = repository.save(delivery);

        orderClient.delivery(orderId);

        log.info("Доставка отмечена как успешная: orderId={}, deliveryId={}",
                orderId, delivery.getDeliveryId());

        return mapper.toDto(updatedDelivery);
    }

    @Transactional
    @Override
    public DeliveryDto markDeliveryPicked(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);

        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        Delivery updatedDelivery = repository.save(delivery);

        warehouseClient.shippedToDelivery(
                ShippedToDeliveryRequest.builder()
                        .orderId(orderId)
                        .deliveryId(delivery.getDeliveryId())
                        .build()
        );

        orderClient.assembly(orderId);

        log.info("Доставка получена: orderId={}, deliveryId={}",
                orderId, delivery.getDeliveryId());

        return mapper.toDto(updatedDelivery);
    }

    @Transactional
    @Override
    public DeliveryDto markDeliveryFailed(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);

        delivery.setDeliveryState(DeliveryState.FAILED);
        Delivery updatedDelivery = repository.save(delivery);

        orderClient.deliveryFailed(orderId);

        log.info("Доставка отмечена как неудавшаяся: orderId={}, deliveryId={}",
                orderId, delivery.getDeliveryId());

        return mapper.toDto(updatedDelivery);
    }

    @Transactional
    @Override
    public Double calculateDeliveryCost(OrderDto orderDto) {
        if (orderDto == null || orderDto.getDeliveryVolume() == null || orderDto.getDeliveryWeight() == null) {
            throw new NotEnoughInfoInOrderToCalculateException("Недостаточно информации для расчета стоимости доставки");
        }

        Delivery delivery = getDeliveryByOrderIdOrThrow(orderDto.getOrderId());
        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();

        Double cost = calculateDeliveryPrice(
                warehouseAddress,
                delivery.getToAddress(),
                orderDto.getDeliveryWeight(),
                orderDto.getDeliveryVolume(),
                orderDto.getFragile()
        );

        log.debug("Стоимость доставки расчитана: orderId={}, cost={}",
                orderDto.getOrderId(), cost);

        return cost;
    }

    private Delivery getDeliveryByOrderIdOrThrow(UUID orderId) {
        if (orderId == null) {
            throw new NoOrderFoundException("Id заказа не может быть пустым");
        }

        return repository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("Доставка по заказу не найдена: {}", orderId);
                    return new NoDeliveryFoundException("Доставка по заказу не найдена: " + orderId);
                });
    }

    private void validateDeliveryRequest(NewDeliveryRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Запрос на доставку не может быть пустым");
        }

        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Требуется ID заказа");
        }

        if (request.getFromAddress() == null) {
            throw new IllegalArgumentException("Требуется адрес отправителя");
        }

        if (request.getToAddress() == null) {
            throw new IllegalArgumentException("Требуется адрес получателя");
        }

        if (request.getTotalWeight() == null || request.getTotalWeight() <= 0) {
            throw new IllegalArgumentException("Общий вес должен быть положительным");
        }

        if (request.getTotalVolume() == null || request.getTotalVolume() <= 0) {
            throw new IllegalArgumentException("Общий объем должен быть положительным");
        }

        if (request.getFragile() == null) {
            throw new IllegalArgumentException("Требуется флаг \"Хрупкий\"");
        }
    }

    // Алгоритм расчета стоимости доставки
    private Double calculateDeliveryPrice(AddressDto fromAddress, Address toAddress,
                                          Double weight, Double volume, Boolean fragile) {
        double baseCost = 5.0;
        double cost = baseCost;

        if (fromAddress.getStreet().contains("ADDRESS_2")) {
            cost += baseCost * 2;
        } else if (fromAddress.getStreet().contains("ADDRESS_1")) {
            cost += baseCost * 1;
        }

        // Хрупкость
        if (Boolean.TRUE.equals(fragile)) {
            cost += cost * 0.2;
        }

        // Вес
        cost += weight * 0.3;

        // Объем
        cost += volume * 0.2;

        // Разные улицы
        if (!fromAddress.getStreet().equalsIgnoreCase(toAddress.getStreet())) {
            cost += cost * 0.2;
        }

        // Округление до 2 знаков
        return Math.round(cost * 100.0) / 100.0;
    }
}
