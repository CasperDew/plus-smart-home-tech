package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.DeliveryClient;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.delivery.DeliveryDto;
import ru.yandex.practicum.delivery.NewDeliveryRequestDto;
import ru.yandex.practicum.enums.OrderState;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.mapper.OrderToDtoMapper;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.order.CreateNewOrderRequest;
import ru.yandex.practicum.order.OrderDto;
import ru.yandex.practicum.order.ProductReturnRequest;
import ru.yandex.practicum.repository.OrderRepository;
import ru.yandex.practicum.warehouse.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.warehouse.BookedProductsDto;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderToDtoMapper mapper;
    private final DeliveryClient deliveryClient;
    private final PaymentClient paymentClient;
    private final WarehouseClient warehouseClient;

    @Override
    public List<OrderDto> getUserOrders(String username) {
        validateUsername(username);

        List<Order> orders = repository.findAllByUsername(username);
        log.debug("Найдено {} заказов для пользователя: {}", orders.size(), username);

        return orders.stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public OrderDto createOrder(CreateNewOrderRequest request, String username) {
        validateUsername(username);

        // Создание заказ
        Order order = Order.builder()
                .orderId(UUID.randomUUID())
                .shoppingCartId(request.getShoppingCartDto().getShoppingCartId())
                .username(username)
                .products(request.getShoppingCartDto().getProducts())
                .state(OrderState.NEW)
                .fragile(false)
                .totalPrice(0.0)
                .deliveryPrice(0.0)
                .productPrice(0.0)
                .build();

        repository.save(order);
        log.info("Созданный заказ: {}, состояние: {}", order.getOrderId(), order.getState());

        AssemblyProductsForOrderRequest assemblyRequest = AssemblyProductsForOrderRequest.builder()
                .orderId(order.getOrderId())
                .products(request.getShoppingCartDto().getProducts())
                .build();

        BookedProductsDto booked = warehouseClient.assemblyProductsForOrder(assemblyRequest);
        log.debug("Товары, забронированный для доставки: {}", order.getOrderId());

        // Создание доставки
        NewDeliveryRequestDto deliveryRequest = NewDeliveryRequestDto.builder()
                .orderId(order.getOrderId())
                .toAddress(request.getDeliveryAddress())
                .fromAddress(warehouseClient.getWarehouseAddress())
                .totalWeight(booked.getDeliveryWeight())
                .totalVolume(booked.getDeliveryVolume())
                .fragile(booked.getFragile())
                .build();

        DeliveryDto delivery = deliveryClient.createDelivery(deliveryRequest);
        log.debug("Доставка создана: {}", delivery.getDeliveryId());

        // Обновление заказа
        order.setState(OrderState.ASSEMBLED);
        order.setDeliveryId(delivery.getDeliveryId());
        order.setDeliveryWeight(booked.getDeliveryWeight());
        order.setDeliveryVolume(booked.getDeliveryVolume());
        order.setFragile(booked.getFragile());

        Order savedOrder = repository.save(order);
        log.info("Собран заказ: {}", order.getOrderId());

        return mapper.toDto(savedOrder);
    }

    @Transactional
    @Override
    public OrderDto returnProducts(ProductReturnRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new IllegalArgumentException("Запрос не может возвращать null");
        }

        Order order = getOrderOrThrow(request.getOrderId());

        warehouseClient.returnProduct(request.getProducts());
        log.debug("Товары возвращены на склад для оформления заказа: {}", request.getOrderId());

        order.setState(OrderState.PRODUCT_RETURNED);
        Order updatedOrder = repository.save(order);

        log.info("Заказ помечен как возвращенный: {}", request.getOrderId());

        return mapper.toDto(updatedOrder);
    }

    @Override
    public OrderDto calculateTotal(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        OrderDto orderDto = mapper.toDto(order);

        Double productCost = paymentClient.productCost(orderDto);
        Double deliveryCost = deliveryClient.calculateDeliveryCost(orderDto);

        // Расчет итоговой стоимости
        Double tax = productCost * 0.1;
        Double totalCost = productCost + tax + deliveryCost;

        order.setProductPrice(productCost);
        order.setDeliveryPrice(deliveryCost);
        order.setTotalPrice(totalCost);
        order.setState(OrderState.ON_PAYMENT);

        Order updatedOrder = repository.save(order);
        log.info("Общая стоимость: {} для заказа: {}", totalCost, orderId);

        return mapper.toDto(updatedOrder);
    }

    @Transactional
    @Override
    public OrderDto calculateDelivery(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        Double deliveryCost = deliveryClient.calculateDeliveryCost(mapper.toDto(order));
        order.setDeliveryPrice(deliveryCost);

        Order updatedOrder = repository.save(order);
        log.info("Стоимость доставки: {} для заказа: {}", deliveryCost, orderId);

        return mapper.toDto(updatedOrder);
    }

    @Transactional
    @Override
    public OrderDto payment(UUID orderId) {
        Order order = getOrderOrThrow(orderId);

        paymentClient.createPayment(mapper.toDto(order));
        log.debug("Создан платеж для заказа: {}", orderId);

        order.setState(OrderState.ON_PAYMENT);
        Order updatedOrder = repository.save(order);

        log.info("Заказ помечен как оплаченный: {}", orderId);

        return mapper.toDto(updatedOrder);
    }

    @Transactional
    @Override
    public OrderDto paymentSuccess(UUID orderId) {
        return updateOrderState(orderId, OrderState.PAID);
    }

    @Transactional
    @Override
    public OrderDto paymentFailed(UUID orderId) {
        return updateOrderState(orderId, OrderState.PAYMENT_FAILED);
    }

    @Transactional
    @Override
    public OrderDto delivery(UUID orderId) {
        return updateOrderState(orderId, OrderState.DELIVERED);
    }

    @Transactional
    @Override
    public OrderDto deliveryFailed(UUID orderId) {
        return updateOrderState(orderId, OrderState.DELIVERY_FAILED);
    }

    @Transactional
    @Override
    public OrderDto completed(UUID orderId) {
        return updateOrderState(orderId, OrderState.COMPLETED);
    }

    @Transactional
    @Override
    public OrderDto assembly(UUID orderId) {
        return updateOrderState(orderId, OrderState.ASSEMBLED);
    }

    @Transactional
    @Override
    public OrderDto assemblyFailed(UUID orderId) {
        return updateOrderState(orderId, OrderState.ASSEMBLY_FAILED);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }
    }

    private Order getOrderOrThrow(UUID orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Заказ не найден: {}", orderId);
                    return new NoOrderFoundException("Заказ не найден: " + orderId);
                });
    }

    private OrderDto updateOrderState(UUID orderId, OrderState newState) {
        Order order = getOrderOrThrow(orderId);
        order.setState(newState);

        Order updatedOrder = repository.save(order);
        log.info("Состояние заказа {} обновлено до: {}", orderId, newState);

        return mapper.toDto(updatedOrder);
    }
}
