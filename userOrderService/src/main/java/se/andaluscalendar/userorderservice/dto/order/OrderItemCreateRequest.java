package se.andaluscalendar.userorderservice.dto.order;

public record OrderItemCreateRequest(
        Long productId,
        Integer quantity
) {
}
