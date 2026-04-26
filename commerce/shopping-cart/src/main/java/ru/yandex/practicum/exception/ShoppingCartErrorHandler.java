package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ShoppingCartErrorHandler {
    @ExceptionHandler(NoProductsInShoppingCartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoProductsInShoppingCart(NoProductsInShoppingCartException e) {
        log.warn("Нет товаров в корзине покупок: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Нет товара в корзине покупок")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(NotAuthorizedUserException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleNotAuthorizedUser(NotAuthorizedUserException e) {
        log.warn("Пользователь не авторизован: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("UNAUTHORIZED")
                .message("Пользователь не авторизован")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(NotFoundCartException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCartNotFound(NotFoundCartException e) {
        log.warn("Карзина не найдена: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("CART_NOT_FOUND")
                .message("Карзина не найдена")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Недопустимый параметр запроса: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Недопустимый параметр запроса")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalState(IllegalStateException e) {
        log.warn("Недопустимая операция: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Недопустимая операция")
                .detail(e.getMessage())
                .build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEmptyBody(HttpMessageNotReadableException e) {
        log.warn("Пустое или неверное тело запроса: {}", e.getMessage());
        return ErrorResponse.builder()
                .error("BAD_REQUEST")
                .message("Требуется текст запроса")
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
