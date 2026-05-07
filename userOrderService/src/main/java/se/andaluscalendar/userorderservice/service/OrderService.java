package se.andaluscalendar.userorderservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import se.andaluscalendar.userorderservice.client.ProductServiceClient;
import se.andaluscalendar.userorderservice.dto.order.CreateOrderRequest;
import se.andaluscalendar.userorderservice.dto.order.OrderItemCreateRequest;
import se.andaluscalendar.userorderservice.dto.order.OrderItemResponse;
import se.andaluscalendar.userorderservice.dto.order.OrderResponse;
import se.andaluscalendar.userorderservice.exception.UnauthorizedException;
import se.andaluscalendar.userorderservice.model.OrderItem;
import se.andaluscalendar.userorderservice.model.UserOrder;
import se.andaluscalendar.userorderservice.repository.OrderItemRepository;
import se.andaluscalendar.userorderservice.repository.UserOrderRepository;
import se.andaluscalendar.userorderservice.util.JwtUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final UserOrderRepository userOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductServiceClient productServiceClient;
    private final JwtUtil jwtUtil;

    public OrderService(UserOrderRepository userOrderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductServiceClient productServiceClient,
                        JwtUtil jwtUtil) {
        this.userOrderRepository = userOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productServiceClient = productServiceClient;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public OrderResponse createOrder(String authorizationHeader, CreateOrderRequest request) {
        UUID userId = extractUserIdFromAccessToken(authorizationHeader);
        validateCreateOrderRequest(request);

        List<PreparedOrderItem> preparedItems = request.items().stream()
                .map(this::prepareOrderItem)
                .toList();

        BigDecimal totalAmount = preparedItems.stream()
                .map(PreparedOrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UserOrder order = new UserOrder();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        UserOrder savedOrder = userOrderRepository.save(order);

        List<OrderItem> savedItems = preparedItems.stream().map(item -> {
            OrderItem entity = new OrderItem();
            entity.setOrder(savedOrder);
            entity.setProductId(item.productId());
            entity.setQuantity(item.quantity());
            entity.setPriceAtPurchase(item.priceAtPurchase());
            return entity;
        }).toList();
        savedItems = orderItemRepository.saveAll(savedItems);

        return mapToOrderResponse(savedOrder, savedItems);
    }

    public List<OrderResponse> getMyOrders(String authorizationHeader) {
        UUID userId = extractUserIdFromAccessToken(authorizationHeader);
        List<UserOrder> orders = userOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<OrderItem> items = orderItemRepository.findByOrderIn(orders);
        Map<UUID, List<OrderItem>> itemsByOrderId = items.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream()
                .map(order -> mapToOrderResponse(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    private PreparedOrderItem prepareOrderItem(OrderItemCreateRequest requestItem) {
        if (requestItem.productId() == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        if (requestItem.quantity() == null || requestItem.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        BigDecimal price = productServiceClient.fetchProductPrice(requestItem.productId());
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(requestItem.quantity()));
        return new PreparedOrderItem(requestItem.productId(), requestItem.quantity(), price, lineTotal);
    }

    private UUID extractUserIdFromAccessToken(String authorizationHeader) {
        String accessToken = extractBearerToken(authorizationHeader);
        Claims claims;
        try {
            claims = jwtUtil.validateAndExtractAccessClaims(accessToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid access token");
        }

        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid access token subject");
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization header must use Bearer token");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new UnauthorizedException("Bearer token is missing");
        }
        return token;
    }

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must include at least one item");
        }
    }

    private OrderResponse mapToOrderResponse(UserOrder order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses
        );
    }

    private record PreparedOrderItem(
            Long productId,
            Integer quantity,
            BigDecimal priceAtPurchase,
            BigDecimal lineTotal
    ) {
    }
}
