package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ShoppingStoreErrorHandler {
    @ExceptionHandler(NotFoundProductException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundProduct(NotFoundProductException e) {
        log.warn("Товар не найден: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("PRODUCT_NOT_FOUND")
                .message("Товар не найден")
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

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("Внутренняя ошибка сервера")
                .detail(e.getMessage())
                .build();
    }
}
