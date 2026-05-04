package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class DeliveryErrorHandler {
    @ExceptionHandler(NoDeliveryFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoDeliveryFound(NoDeliveryFoundException e) {
        log.warn("Доставка не найдена: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("DELIVERY_NOT_FOUND")
                .message("Доставка не найдена")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(NotEnoughInfoInOrderToCalculateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotEnoughInfoInOrderToCalculate(NotEnoughInfoInOrderToCalculateException e) {
        log.warn("Недостаточно информации для расчета заказа: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("INSUFFICIENT_ORDER_INFO")
                .message("Недостаточно информации для расчета заказа")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalState(IllegalStateException e) {
        log.warn("Неверная операция: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Неверная операция")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Неверный параметр: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Неверный параметр запроса")
                .detail(e.getMessage())
                .build();
    }
}
