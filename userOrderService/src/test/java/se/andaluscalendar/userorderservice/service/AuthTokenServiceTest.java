package se.andaluscalendar.userorderservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthTokenService authTokenService;

    @Test
    @DisplayName("Test/ Refresh token blank input throws UnauthorizedException")
    void whenRefreshTokenIsBlank_thenThrowUnauthorized() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                authTokenService.refreshTokens(" ")
        );
        assertEquals("Refresh token is required", ex.getMessage());
    }

    @Test
    @DisplayName("Test/ Refresh token rotates and persists new token")
    void whenRefreshTokenIsValid_thenRotateAndReturnNewTokens() {
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = "old-refresh-token";
        String newRefreshToken = "new-refresh-token";
        String newAccessToken = "new-access-token";

        Claims oldClaims = claimsWithSubjectAndExpiry(userId, minutesFromNow(30));
        Claims newClaims = claimsWithSubjectAndExpiry(userId, minutesFromNow(60));

        RefreshToken existingToken = new RefreshToken();
        existingToken.setUserId(userId);
        existingToken.setTokenHash(hashToken(rawRefreshToken));
        existingToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        existingToken.setRevoked(false);

        StoreUser user = new StoreUser();
        user.setId(userId);

        when(jwtUtil.validateAndExtractRefreshClaims(rawRefreshToken)).thenReturn(oldClaims);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken)))
                .thenReturn(Optional.of(existingToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtUtil.generateRefreshToken(userId.toString())).thenReturn(newRefreshToken);
        when(jwtUtil.validateAndExtractRefreshClaims(newRefreshToken)).thenReturn(newClaims);
        when(jwtUtil.generateAccessToken(userId.toString())).thenReturn(newAccessToken);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokensResponse response = authTokenService.refreshTokens(rawRefreshToken);

        assertEquals(newAccessToken, response.accessToken());
        assertEquals(newRefreshToken, response.refreshToken());

        ArgumentCaptor<RefreshToken> savedTokensCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(savedTokensCaptor.capture());
        List<RefreshToken> savedTokens = savedTokensCaptor.getAllValues();

        RefreshToken revokedOldToken = savedTokens.getFirst();
        assertTrue(revokedOldToken.isRevoked());
        assertEquals(hashToken(newRefreshToken), revokedOldToken.getReplacedByTokenHash());

        RefreshToken persistedNewToken = savedTokens.get(1);
        assertEquals(userId, persistedNewToken.getUserId());
        assertEquals(hashToken(newRefreshToken), persistedNewToken.getTokenHash());
        assertFalse(persistedNewToken.isRevoked());
    }

    @Test
    @DisplayName("Test/ Expired refresh token is revoked and rejected")
    void whenRefreshTokenExpired_thenRevokeAndThrowUnauthorized() {
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = "expired-refresh-token";

        Claims claims = claimsWithSubjectAndExpiry(userId, minutesFromNow(-1));
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUserId(userId);
        expiredToken.setTokenHash(hashToken(rawRefreshToken));
        expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        expiredToken.setRevoked(false);

        when(jwtUtil.validateAndExtractRefreshClaims(rawRefreshToken)).thenReturn(claims);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken)))
                .thenReturn(Optional.of(expiredToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                authTokenService.refreshTokens(rawRefreshToken)
        );

        assertEquals("Refresh token has expired", ex.getMessage());
        assertTrue(expiredToken.isRevoked());
        verify(refreshTokenRepository, times(1)).save(expiredToken);
    }

    @Test
    @DisplayName("Test/ Logout with valid refresh token revokes token")
    void whenLogoutWithValidToken_thenRevokeToken() {
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = "logout-refresh-token";
        String authHeader = "Bearer " + rawRefreshToken;

        Claims claims = claimsWithSubjectAndExpiry(userId, minutesFromNow(30));
        RefreshToken existingToken = new RefreshToken();
        existingToken.setUserId(userId);
        existingToken.setTokenHash(hashToken(rawRefreshToken));
        existingToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        existingToken.setRevoked(false);

        when(jwtUtil.validateAndExtractRefreshClaims(rawRefreshToken)).thenReturn(claims);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(rawRefreshToken)))
                .thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authTokenService.logout(authHeader);

        assertTrue(existingToken.isRevoked());
        verify(refreshTokenRepository, times(1)).save(existingToken);
    }

    private Claims claimsWithSubjectAndExpiry(UUID userId, Date expiryDate) {
        Claims claims = Jwts.claims();
        claims.setSubject(userId.toString());
        claims.setExpiration(expiryDate);
        return claims;
    }

    private Date minutesFromNow(int minutes) {
        return Date.from(LocalDateTime.now().plusMinutes(minutes).atZone(ZoneId.systemDefault()).toInstant());
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
