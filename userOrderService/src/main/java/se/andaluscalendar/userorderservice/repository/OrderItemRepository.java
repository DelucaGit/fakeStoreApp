package se.andaluscalendar.userorderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.andaluscalendar.userorderservice.model.OrderItem;
import se.andaluscalendar.userorderservice.model.UserOrder;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderIn(List<UserOrder> orders);
}
