package se.andaluscalendar.userorderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.andaluscalendar.userorderservice.model.StoreUser;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<StoreUser, UUID> {

    Optional<StoreUser> findByEmail(String email);
}
