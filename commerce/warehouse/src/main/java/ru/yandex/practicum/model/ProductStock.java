package ru.yandex.practicum.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "product_stock", schema = "warehouse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStock {
    @Id
    @Column(name = "product_id", nullable = false)
    UUID productId;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    Boolean fragile = false;

    @NotNull
    @Column(nullable = false)
    @Min(1)
    Double width;

    @NotNull
    @Column(nullable = false)
    @Min(1)
    Double height;

    @NotNull
    @Column(nullable = false)
    @Min(1)
    Double depth;

    @NotNull
    @Column(nullable = false)
    @Min(1)
    Double weight;

    @NotNull
    @Column(nullable = false)
    @Min(0)
    @Builder.Default
    Long quantity = 0L;

    public Double volume() {
        return width * height * depth;
    }
}
