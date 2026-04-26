package ru.yandex.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "carts", schema = "shopping_cart")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCart{
    @Id
    @Column(name = "shopping_cart_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID shoppingCartId;

    @Column(name = "username", nullable = false, unique = true)
    @NotBlank
    @Size(max = 255)
    private String username;

    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CartItem> items = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ShoppingCartState state = ShoppingCartState.ACTIVE;
}
