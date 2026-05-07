package se.andaluscalendar.userorderservice.dto.order;

import java.util.List;

public record CreateOrderRequest(
        List<OrderItemCreateRequest> items
) {
}
