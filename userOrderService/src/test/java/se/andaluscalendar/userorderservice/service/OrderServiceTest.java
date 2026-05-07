package se.andaluscalendar.userorderservice.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.andaluscalendar.userorderservice.client.ProductServiceClient;
import se.andaluscalendar.userorderservice.dto.order.CreateOrderRequest;
import se.andaluscalendar.userorderservice.dto.order.OrderItemCreateRequest;
import se.andaluscalendar.userorderservice.dto.order.OrderResponse;
import se.andaluscalendar.userorderservice.exception.UnauthorizedException;
import se.andaluscalendar.userorderservice.model.OrderItem;
import se.andaluscalendar.userorderservice.model.UserOrder;
import se.andaluscalendar.userorderservice.repository.OrderItemRepository;
import se.andaluscalendar.userorderservice.repository.UserOrderRepository;
import se.andaluscalendar.userorderservice.util.JwtUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private UserOrderRepository userOrderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductServiceClient productServiceClient;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Test/ Create order calculates total and returns saved response")
    void whenCreateOrder_thenReturnsOrderResponse() {
        UUID userId = UUID.randomUUID();
        Claims claims = io.jsonwebtoken.Jwts.claims();
        claims.setSubject(userId.toString());

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new OrderItemCreateRequest(1L, 2),
                new OrderItemCreateRequest(2L, 1)
        ));

        when(jwtUtil.validateAndExtractAccessClaims("access-token")).thenReturn(claims);
        when(productServiceClient.fetchProductPrice(1L)).thenReturn(new BigDecimal("10.00"));
        when(productServiceClient.fetchProductPrice(2L)).thenReturn(new BigDecimal("5.50"));

        when(userOrderRepository.save(any(UserOrder.class))).thenAnswer(invocation -> {
            UserOrder order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            order.setCreatedAt(LocalDateTime.now());
            return order;
        });

        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder("Bearer access-token", request);

        assertEquals(userId, response.userId());
        assertEquals(new BigDecimal("25.50"), response.totalAmount());
        assertEquals(2, response.items().size());
    }

    @Test
    @DisplayName("Test/ Missing authorization header throws UnauthorizedException")
    void whenGetMyOrdersWithoutAuthHeader_thenThrowUnauthorized() {
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> orderService.getMyOrders(null)
        );

        assertEquals("Authorization header is required", exception.getMessage());
    }

    @Test
    @DisplayName("Test/ Get my orders returns only authenticated user orders")
    void whenGetMyOrders_thenReturnMappedOrders() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Claims claims = io.jsonwebtoken.Jwts.claims();
        claims.setSubject(userId.toString());

        UserOrder order = new UserOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal("19.98"));
        order.setCreatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(10L);
        item.setQuantity(2);
        item.setPriceAtPurchase(new BigDecimal("9.99"));

        when(jwtUtil.validateAndExtractAccessClaims("token")).thenReturn(claims);
        when(userOrderRepository.findByUserIdOrderByCreatedAtDesc(eq(userId))).thenReturn(List.of(order));
        when(orderItemRepository.findByOrderIn(List.of(order))).thenReturn(List.of(item));

        List<OrderResponse> responses = orderService.getMyOrders("Bearer token");

        assertEquals(1, responses.size());
        assertEquals(orderId, responses.get(0).orderId());
        assertEquals(1, responses.get(0).items().size());
        assertEquals(10L, responses.get(0).items().getFirst().productId());
    }
}
