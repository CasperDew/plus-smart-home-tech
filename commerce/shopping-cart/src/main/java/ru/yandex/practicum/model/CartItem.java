package ru.yandex.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "cart_items", schema = "shopping_cart", uniqueConstraints = @UniqueConstraint(
        name = "unique_cart_product", columnNames = {"shopping_cart_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_cart_id", nullable = false)
    private ShoppingCart shoppingCart;

    @Column(name = "product_id", nullable = false)
    @NotNull
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    @NotNull
    private Long quantity;
}
