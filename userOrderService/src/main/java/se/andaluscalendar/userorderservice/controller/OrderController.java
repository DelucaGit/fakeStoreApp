package se.andaluscalendar.userorderservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.andaluscalendar.userorderservice.dto.order.CreateOrderRequest;
import se.andaluscalendar.userorderservice.dto.order.OrderResponse;
import se.andaluscalendar.userorderservice.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(authorizationHeader, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(authorizationHeader));
    }
}
