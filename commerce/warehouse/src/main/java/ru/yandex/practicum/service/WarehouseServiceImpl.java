package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.cart.ShoppingCartDto;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.model.OrderBooking;
import ru.yandex.practicum.model.ProductStock;
import ru.yandex.practicum.repository.OrderBookingRepository;
import ru.yandex.practicum.repository.WarehouseProductStockRepository;
import ru.yandex.practicum.warehouse.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseServiceImpl implements WarehouseService {
    private final WarehouseProductStockRepository repository;
    private final OrderBookingRepository bookingRepository;

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

    @Override
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        Map<UUID, Long> products = request.getProducts();

        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Товары не могут быть пустыми или содержать нулевое значение");
        }

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean fragile = false;

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            ProductStock productStock = repository.findById(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Продукт с ID " + productId + " не найден на складе"));

            if (productStock.getQuantity() < quantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Недостаточный запас продукции: " + productId);
            }

            productStock.setQuantity(productStock.getQuantity() - quantity);
            repository.save(productStock);

            totalWeight += productStock.getWeight() * quantity;
            totalVolume += productStock.volume() * quantity;
            fragile = fragile || Boolean.TRUE.equals(productStock.getFragile());
        }

        OrderBooking booking = OrderBooking.builder()
                .bookingId(UUID.randomUUID())
                .orderId(request.getOrderId())
                .totalWeight(totalWeight)
                .totalVolume(totalVolume)
                .fragile(fragile)
                .products(products)
                .build();

        bookingRepository.save(booking);

        log.info("Заказ собран: orderId={}, productsCount={}, weight={}, volume={}, fragile={}",
                request.getOrderId(), products.size(), totalWeight, totalVolume, fragile);

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(fragile)
                .build();
    }

    @Override
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        OrderBooking booking = bookingRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Заказ на оформление не найден: " + request.getOrderId()));

        booking.setDeliveryId(request.getDeliveryId());
        bookingRepository.save(booking);

        log.info("Товар отправлен в доставку: orderId={}, deliveryId={}",
                request.getOrderId(), request.getDeliveryId());
    }

    @Override
    public void returnProduct(Map<UUID, Long> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("Товары не могут быть пустыми или содержать нулевое значение.");
        }

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            ProductStock stock = repository.findById(entry.getKey())
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Товар с ID " + entry.getKey() + " не найден не складе"));

            stock.setQuantity(stock.getQuantity() + entry.getValue());
            repository.save(stock);

            log.debug("Возврат товара: productId={}, quantity={}", entry.getKey(), entry.getValue());
        }

        log.info("Товары возвращены на склад: {} товаров", products.size());
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
