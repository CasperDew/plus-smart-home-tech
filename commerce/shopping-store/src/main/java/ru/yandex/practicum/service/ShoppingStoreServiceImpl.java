package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.enums.ProductCategory;
import ru.yandex.practicum.enums.ProductState;
import ru.yandex.practicum.enums.QuantityState;
import ru.yandex.practicum.exception.NotFoundProductException;
import ru.yandex.practicum.mapper.ProductToDtoMapper;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.repository.ShoppingStoreProductRepository;
import ru.yandex.practicum.store.ProductDto;
import ru.yandex.practicum.store.SetProductQuantityStateRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShoppingStoreServiceImpl implements ShoppingStoreService {

    private final ShoppingStoreProductRepository repository;
    private final ProductToDtoMapper mapper;

    @Transactional
    @Override
    public Page<ProductDto> getProductsByCategory(ProductCategory category, Pageable pageable) {
        log.debug("Поиск товаров по категориям: {}", category);
        return repository.findByProductCategory(category, pageable)
                .map(mapper::toDto);
    }

    @Transactional
    @Override
    public ProductDto createProduct(ProductDto dto) {
        Product product = mapper.toEntity(dto);

        if (product.getProductId() == null) {
            product.setProductId(UUID.randomUUID());
        }
        if (product.getProductState() == null) {
            product.setProductState(ProductState.ACTIVE);
        }
        if (product.getQuantityState() == null) {
            product.setQuantityState(QuantityState.ENOUGH);
        }

        Product saved = repository.save(product);
        log.info("Создание товара: {} - {}", saved.getProductId(), saved.getProductName());
        return mapper.toDto(saved);
    }

    @Transactional
    @Override
    public ProductDto updateProduct(ProductDto dto) {
        if (dto.getProductId() == null) {
            throw new IllegalArgumentException("Для обновления требуется идентификатор продукта.");
        }

        Product product = repository.findById(dto.getProductId())
                .orElseThrow(() -> new NotFoundProductException("Товар не найден с ID: " + dto.getProductId()));

        mapper.updateEntityFromDto(dto, product);

        Product saved = repository.save(product);
        log.info("Обновление товара: {} - {}", saved.getProductId(), saved.getProductName());
        return mapper.toDto(saved);
    }

    @Transactional
    @Override
    public boolean removeProductFromStore(UUID productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new NotFoundProductException("Товар не найден с ID: " + productId));

        if (product.getProductState() == ProductState.DEACTIVATE) {
            log.warn("Товар готов к отключению: {}", productId);
            return false;
        }

        product.setProductState(ProductState.DEACTIVATE);
        repository.save(product);

        log.info("Деактевация товара: {}", productId);
        return true;
    }

    @Transactional
    @Override
    public boolean setProductQuantityState(SetProductQuantityStateRequest request) {
        Product product = repository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundProductException(
                        "Товар не найден с ID: " + request.getProductId()));

        product.setQuantityState(request.getQuantityState());
        repository.save(product);

        log.info("Обновление статуса товара {}: {}",
                product.getProductId(), request.getQuantityState());
        return true;
    }

    @Override
    public ProductDto getProduct(UUID productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new NotFoundProductException("Товар не найден с ID: " + productId));

        log.debug("Получение товара: {} - {}", product.getProductId(), product.getProductName());
        return mapper.toDto(product);
    }
}
