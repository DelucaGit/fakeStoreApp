package se.andaluscalendar.userorderservice.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        Integer quantity,
        BigDecimal priceAtPurchase,
        BigDecimal lineTotal
) {
}
