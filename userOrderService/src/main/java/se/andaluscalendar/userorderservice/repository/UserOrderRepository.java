package se.andaluscalendar.userorderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.andaluscalendar.userorderservice.model.UserOrder;

import java.util.List;
import java.util.UUID;

public interface UserOrderRepository extends JpaRepository<UserOrder, UUID> {
    List<UserOrder> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
