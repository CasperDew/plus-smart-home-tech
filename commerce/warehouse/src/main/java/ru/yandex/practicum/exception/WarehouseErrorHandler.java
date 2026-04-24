package ru.yandex.practicum.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WarehouseErrorHandler {
    @ExceptionHandler(NoSpecifiedProductInWarehouseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoSpecifiedProduct(NoSpecifiedProductInWarehouseException e) {
        log.warn("Товар не найден на складе");
        return ErrorResponse.builder()
                .error("NO_PRODUCT_IN_WAREHOUSE")
                .message("Товар не найден на складе")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleLowQuantity(ProductInShoppingCartLowQuantityInWarehouseException e) {
        log.warn("Недостаточный запас на складе: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("INSUFFICIENT_STOCK")
                .message("Недостаточный запас на складе")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(SpecifiedProductAlreadyInWarehouseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleProductAlreadyExists(SpecifiedProductAlreadyInWarehouseException e) {
        log.warn("Товар уже есть на складе: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("PRODUCT_ALREADY_EXISTS")
                .message("Товар уже есть на складе")
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
}
