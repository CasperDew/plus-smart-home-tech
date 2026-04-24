package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cart.ChangeProductQuantityRequest;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
import ru.yandex.practicum.exception.NotFoundCartException;
import ru.yandex.practicum.mapper.CartMapper;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ShoppingCart;
import ru.yandex.practicum.model.ShoppingCartState;
import ru.yandex.practicum.repository.ShoppingCartRepository;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingCartServiceImpl implements ShoppingCartService {
    private final ShoppingCartRepository repository;
    private final CartMapper mapper;
    private final WarehouseClient warehouseClient;

    @Override
    public ShoppingCartDto getCart(String username) {
        validateUsername(username);
        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseGet(() -> createNewCart(username));

        log.debug("Извлеченная корзина: {} для пользователя: {}", shoppingCart.getShoppingCartId(), username);

        return mapper.toDto(shoppingCart);
    }

    @Transactional
    @Override
    public ShoppingCartDto addProducts(String username, Map<UUID, Long> products) {
        validateUsername(username);

        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Товар не может быть пустым");
        }

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseGet(() -> createNewCart(username));

        checkCartActive(shoppingCart);

        addProductsToCart(shoppingCart, products);

        ShoppingCart saveCart = repository.save(shoppingCart);

        log.info("В корзину добавлены товары: username = {}, cartId = {}, products = {}",
                username, shoppingCart.getShoppingCartId(), products);

        return mapper.toDto(saveCart);
    }

    @Transactional
    @Override
    public void deactivate(String username) {
        validateUsername(username);

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseGet(() -> createNewCart(username));

        shoppingCart.setState(ShoppingCartState.DEACTIVATED);
        repository.save(shoppingCart);

        log.info("Деактевированная корзина: username={}, cartId={}",
                username, shoppingCart.getShoppingCartId());
    }

    @Transactional
    @Override
    public ShoppingCartDto removeProducts(String username, List<UUID> productId) {
        validateUsername(username);

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseThrow(() -> new NotFoundCartException("Корзина не найдена для пользователя: " + username));

        checkCartActive(shoppingCart);

        if (!shoppingCart.getItems().removeIf(i -> productId.contains(i.getProductId()))) {
            throw new NoProductsInShoppingCartException("Товары не найдены в корзине");
        }

        ShoppingCart saved = repository.save(shoppingCart);
        log.info("Удаление товаров: user={}, productIds={}", username, productId);

        return mapper.toDto(saved);
    }

    @Transactional
    @Override
    public ShoppingCartDto changeQuantity(String username, ChangeProductQuantityRequest request) {
        validateUsername(username);

        if (request.getNewQuantity() <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }

        ShoppingCart shoppingCart = repository.findByUsernameWithItems(username)
                .orElseThrow(() -> new NotFoundCartException("Корзина не найдена для пользователя: " + username));

        checkCartActive(shoppingCart);

        CartItem item = shoppingCart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseThrow(() -> new NoProductsInShoppingCartException("Товары не найдены в корзине"));

        Long oldQuantity = item.getQuantity();
        item.setQuantity(request.getNewQuantity());

        ShoppingCart saved = repository.save(shoppingCart);

        // Проверить доступность при увеличении
        if (request.getNewQuantity() > oldQuantity) {
            ShoppingCartDto checkDto = mapper.toDto(saved);
            warehouseClient.checkAvailability(checkDto);
        }

        item.setQuantity(request.getNewQuantity());

        log.info("Изменение количества: user={}, product={}, newQty={}",
                username, request.getProductId(), request.getNewQuantity());

        return mapper.toDto(saved);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Имя пользователя не может быть пустым");
        }
    }

    private void checkCartActive(ShoppingCart cart) {
        if (cart.getState() == ShoppingCartState.DEACTIVATED) {
            cart.setState(ShoppingCartState.ACTIVE);
            log.info("Восстановленная корзина: cartId={}, user={}",
                    cart.getShoppingCartId(), cart.getUsername());
        }
    }

    private void checkProductsAvailability(UUID cartId, Map<UUID, Long> products) {
        ShoppingCartDto checkDto = ShoppingCartDto.builder()
                .shoppingCartId(cartId)
                .products(products)
                .build();

        warehouseClient.checkAvailability(checkDto);

        log.debug("Проверено наличие продуктов - cartId: {}, productsCount: {}",
                cartId, products.size());
    }

    private ShoppingCart createNewCart(String username) {
        ShoppingCart newCart = ShoppingCart.builder()
                .shoppingCartId(UUID.randomUUID())
                .username(username)
                .state(ShoppingCartState.ACTIVE)
                .items(new HashSet<>())
                .build();

        ShoppingCart savedCart = repository.save(newCart);
        log.info("Создание новой корзины: username={}, cartId={}",
                username, savedCart.getShoppingCartId());

        return savedCart;
    }

    private void addProductsToCart(ShoppingCart shoppingCart, Map<UUID, Long> products) {
        Map<UUID, CartItem> itemMap = new HashMap<>();
        for (CartItem item : shoppingCart.getItems()) {
            itemMap.put(item.getProductId(), item);
        }

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Количество должно быть положительным для продукта: " + productId);
            }

            CartItem existingItem = itemMap.get(productId);

            if (existingItem != null) {
                // Увеличить количество существующего товара
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
            } else {
                // Добавить новый товар
                CartItem newItem = CartItem.builder()
                        .id(UUID.randomUUID())
                        .shoppingCart(shoppingCart)
                        .productId(productId)
                        .quantity(quantity)
                        .build();
                shoppingCart.getItems().add(newItem);
                itemMap.put(productId, newItem);
            }
        }
    }
}
