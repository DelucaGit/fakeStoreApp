package se.andaluscalendar.userorderservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import se.andaluscalendar.userorderservice.dto.auth.AuthTokensResponse;
import se.andaluscalendar.userorderservice.exception.UnauthorizedException;
import se.andaluscalendar.userorderservice.model.RefreshToken;
import se.andaluscalendar.userorderservice.model.StoreUser;
import se.andaluscalendar.userorderservice.repository.RefreshTokenRepository;
import se.andaluscalendar.userorderservice.repository.UserRepository;
import se.andaluscalendar.userorderservice.util.JwtUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthTokenService {
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public AuthTokenService(JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthTokensResponse issueTokensForUser(StoreUser user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());
        persistRefreshToken(user.getId(), refreshToken);
        return new AuthTokensResponse(accessToken, refreshToken);
    }

    @Transactional
    public AuthTokensResponse refreshTokens(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        Claims claims;
        try {
            claims = jwtUtil.validateAndExtractRefreshClaims(rawRefreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid refresh token subject");
        }
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken existingToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or revoked"));

        if (existingToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        if (!existingToken.getUserId().equals(userId)) {
            throw new UnauthorizedException("Refresh token does not belong to this user");
        }

        StoreUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId().toString());
        String newRefreshHash = hashToken(newRefreshToken);

        existingToken.setRevoked(true);
        existingToken.setReplacedByTokenHash(newRefreshHash);
        refreshTokenRepository.save(existingToken);

        persistRefreshToken(user.getId(), newRefreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(user.getId().toString());
        return new AuthTokensResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String rawRefreshToken = extractBearerToken(authorizationHeader);
        Claims claims;
        try {
            claims = jwtUtil.validateAndExtractRefreshClaims(rawRefreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or revoked"));

        if (existingToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        UUID tokenUserId;
        try {
            tokenUserId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid refresh token subject");
        }

        if (!existingToken.getUserId().equals(tokenUserId)) {
            throw new UnauthorizedException("Refresh token does not belong to this user");
        }

        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);
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

    private void persistRefreshToken(UUID userId, String rawRefreshToken) {
        Claims refreshClaims = jwtUtil.validateAndExtractRefreshClaims(rawRefreshToken);
        LocalDateTime expiresAt = refreshClaims.getExpiration().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserId(userId);
        refreshTokenEntity.setTokenHash(hashToken(rawRefreshToken));
        refreshTokenEntity.setExpiresAt(expiresAt);
        refreshTokenEntity.setRevoked(false);

        refreshTokenRepository.save(refreshTokenEntity);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
