package ru.yandex.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.enums.PaymentState;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments", schema = "payment_service")
public class Payment {
    @Id
    @NotNull
    @Column(name = "payment_id")
    private UUID paymentId;

    @NotNull
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_total")
    private Double productTotal;

    @Column(name = "delivery_total")
    private Double deliveryTotal;

    @Column(name = "fee_total")
    private Double feeTotal;

    @Column(name = "total_payment")
    private Double totalPayment;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    private PaymentState state;
}
