package ru.yandex.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "order_booking", schema = "warehouse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderBooking {
    @Id
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @NotNull
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @NotNull
    @Column(name = "total_weight", nullable = false)
    private Double totalWeight;

    @NotNull
    @Column(name = "total_volume", nullable = false)
    private Double totalVolume;

    @NotNull
    @Column(name = "fragile", nullable = false)
    private Boolean fragile;

    @ElementCollection
    @CollectionTable(
            name = "order_booking_products",
            schema = "warehouse",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Map<UUID, Long> products = new HashMap<>();
}
