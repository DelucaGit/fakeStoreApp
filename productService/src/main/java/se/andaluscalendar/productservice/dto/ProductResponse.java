package se.andaluscalendar.productservice.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String title,
        BigDecimal price,
        String description,
        String imageUrl
) {
}
