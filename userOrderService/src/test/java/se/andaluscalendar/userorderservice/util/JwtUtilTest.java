package se.andaluscalendar.userorderservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // The jjtw library requires the secret key to be at least 32 characters long for HMAC-SHA algorithm
    private final String dummyAccessSecretKey = "andalus-calendar-access-secret-key-for-testing-12345";
    private final String dummyRefreshSecretKey = "andalus-calendar-refresh-secret-key-for-testing-12345";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(dummyAccessSecretKey, dummyRefreshSecretKey, 3600000, 604800000);
    }

    @Test
    @DisplayName("Test/ Generate access token successfully creates a JWT")
    void whenGenerateAccessToken_thenReturnsValidJwtString() {
        // Arrange
        String userId = "ny@test.com";

        // Act
        String token = jwtUtil.generateAccessToken(userId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // A valid JWT always consists of exactly 3 parts separated by dots (Header.Payload.Signature)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "The token should have a Header, Payload, and Signature");
    }

    @Test
    @DisplayName("Test/ Generated access token contains the correct user ID as Subject")
    void whenGenerateAccessToken_thenTokenSubjectMatchesUserId() {
        // Arrange
        String userId = "ny@test.com";
        String token = jwtUtil.generateAccessToken(userId);

        // Act
        // We manually parse the token back to prove your generateToken method built it correctly
        Key key = Keys.hmacShaKeyFor(dummyAccessSecretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Assert
        assertEquals(userId, claims.getSubject(), "The subject inside the token should match the provided userId");

        // We can also assert that the expiration date was set in the future
        assertTrue(claims.getExpiration().after(new Date()), "Expiration date should be in the future");
    }

    @Test
    @DisplayName("Test/ Refresh token validates with refresh parser")
    void whenValidateRefreshClaims_thenSuccess() {
        String userId = "user-123";
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        Claims claims = jwtUtil.validateAndExtractRefreshClaims(refreshToken);

        assertEquals(userId, claims.getSubject());
        assertEquals("refresh", claims.get("token_type", String.class));
    }

    @Test
    @DisplayName("Test/ Access token validates with access parser")
    void whenValidateAccessClaims_thenSuccess() {
        String userId = "user-456";
        String accessToken = jwtUtil.generateAccessToken(userId);

        Claims claims = jwtUtil.validateAndExtractAccessClaims(accessToken);

        assertEquals(userId, claims.getSubject());
        assertEquals("access", claims.get("token_type", String.class));
    }

    @Test
    @DisplayName("Test/ Access token rejected by refresh parser")
    void whenValidateRefreshClaimsWithAccessToken_thenThrows() {
        String accessToken = jwtUtil.generateAccessToken("user-789");
        assertThrows(JwtException.class, () -> jwtUtil.validateAndExtractRefreshClaims(accessToken));
    }
}