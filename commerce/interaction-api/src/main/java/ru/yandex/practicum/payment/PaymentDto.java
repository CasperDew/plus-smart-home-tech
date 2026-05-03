package ru.yandex.practicum.payment;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
    @NotNull
    private UUID paymentId;

    private Double totalPayment;
    private Double deliveryTotal;
    private Double feeTotal;
}
