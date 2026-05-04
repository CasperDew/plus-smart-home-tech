package ru.yandex.practicum.service;

import ru.yandex.practicum.order.OrderDto;
import ru.yandex.practicum.payment.PaymentDto;

import java.util.UUID;

public interface PaymentService {
    PaymentDto createPayment(OrderDto orderDto);

    Double calculateProductCost(OrderDto orderDto);

    Double calculateTotalCost(OrderDto orderDto);

    void processPaymentSuccess(UUID paymentId);

    void processPaymentFailed(UUID paymentId);
}
