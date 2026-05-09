package se.andaluscalendar.productservice.client;

import java.math.BigDecimal;

public record FakeStoreProductDto(
        Long id,
        String title,
        BigDecimal price,
        String description,
        String image
) {
}
