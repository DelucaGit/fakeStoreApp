package se.andaluscalendar.userorderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.andaluscalendar.userorderservice.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
}
