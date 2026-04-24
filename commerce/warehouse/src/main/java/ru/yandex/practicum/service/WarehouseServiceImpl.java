package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.model.ProductStock;
import ru.yandex.practicum.repository.WarehouseProductStockRepository;
import ru.yandex.practicum.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.warehouse.AddressDto;
import ru.yandex.practicum.warehouse.BookedProductsDto;
import ru.yandex.practicum.warehouse.NewProductInWarehouseRequest;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseServiceImpl implements WarehouseService {
    private final WarehouseProductStockRepository repository;

    private static final String[] ADDRESSES = new String[]{"ADDRESS_1", "ADDRESS_2"};

    private static final String CURRENT_ADDRESS =
            ADDRESSES[Random.from(new SecureRandom()).nextInt(0, ADDRESSES.length)];

    @Transactional
    @Override
    public void registerNewProductInWarehouse(NewProductInWarehouseRequest request) {
        if (repository.existsById(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Товар с ID " + request.getProductId() + " уже существует на складе");
        }

        ProductStock newProduct = ProductStock.builder()
                .productId(request.getProductId())
                .fragile(request.getFragile() != null ? request.getFragile() : false)
                .width(request.getDimension().getWidth())
                .height(request.getDimension().getHeight())
                .depth(request.getDimension().getDepth())
                .weight(request.getWeight())
                .quantity(0L)
                .build();

        repository.save(newProduct);
        log.info("На склад поступил новый товар: {} с размерами: {}x{}x{}, весом: {}, хрупкостью: {}",
                request.getProductId(),
                request.getDimension().getWidth(),
                request.getDimension().getHeight(),
                request.getDimension().getDepth(),
                request.getWeight(),
                request.getFragile()
        );
    }

    @Override
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        Map<UUID, Long> items = cart.getProducts();

        if (items == null || items.isEmpty()) {
            log.debug("Корзина покупок {} пустая", cart.getShoppingCartId());
            return BookedProductsDto.builder()
                    .deliveryWeight(0.0)
                    .deliveryVolume(0.0)
                    .fragile(false)
                    .build();
        }

        Set<UUID> productIds = items.keySet();
        List<ProductStock> stocks = repository.findAllById(productIds);

        Map<UUID, ProductStock> stockMap = new HashMap<>();
        for (ProductStock stock : stocks) {
            stockMap.put(stock.getProductId(), stock);
        }

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean anyFragile = false;
        Map<UUID, Long> missingProducts = new HashMap<>();

        for (Map.Entry<UUID, Long> entry : items.entrySet()) {
            UUID productId = entry.getKey();
            Long requestedQuantity = entry.getValue() == null ? 0L : entry.getValue();

            ProductStock stock = stockMap.get(productId);
            if (stock == null) {
                throw new NoSpecifiedProductInWarehouseException(
                        "Товар с ID " + productId + " не найден на складе");
            }

            if (stock.getQuantity() < requestedQuantity) {
                long missing = requestedQuantity - stock.getQuantity();
                missingProducts.put(productId, missing);
            }
        }

        if (!missingProducts.isEmpty()) {
            String errorMessage = buildMissingProductsMessage(missingProducts);
            log.warn("Недостаточно товаров для корзины покупок {}: {}",
                    cart.getShoppingCartId(), errorMessage);
            throw new ProductInShoppingCartLowQuantityInWarehouseException(errorMessage);
        }

        for (Map.Entry<UUID, Long> entry : items.entrySet()) {
            UUID productId = entry.getKey();
            Long requestedQuantity = entry.getValue();

            ProductStock stock = stockMap.get(productId);

            double itemVolume = stock.volume();
            totalVolume += itemVolume * requestedQuantity;
            totalWeight += stock.getWeight() * requestedQuantity;

            if (Boolean.TRUE.equals(stock.getFragile())) {
                anyFragile = true;
            }
        }

        BookedProductsDto result = BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(anyFragile)
                .build();

        return result;
    }

    @Transactional
    @Override
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }

        ProductStock stock = repository.findById(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Товар с ID " + request.getProductId() + " не найден на складе"));

        Long newQuantity = stock.getQuantity() + request.getQuantity();
        stock.setQuantity(newQuantity);

        repository.save(stock);

        log.info("Добавлено {} единиц товара {} на складе. Новое количество: {}",
                request.getQuantity(), request.getProductId(), newQuantity);

    }

    @Override
    public AddressDto getWarehouseAddress() {
        AddressDto address = AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();

        log.info("Адрес склада: {}", CURRENT_ADDRESS);
        return address;
    }

    private String buildMissingProductsMessage(Map<UUID, Long> missingProducts) {
        if (missingProducts == null || missingProducts.isEmpty()) {
            return "Товар(ы) отсутствует в необходимом количестве на складе";
        }

        return missingProducts.entrySet().stream()
                .map(e -> String.format("Продукт %s — отсутствует %d ед", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
    }
}
