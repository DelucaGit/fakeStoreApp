package se.andaluscalendar.userorderservice.util;

import io.jsonwebtoken.Claims;
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
    private final String dummySecretKey = "andalus-calendar-super-secret-key-for-testing-12345";

    @BeforeEach
    void setUp() {
        // We don't need Spring or @Value here.
        // We just pass our dummy string directly into the constructor.
        jwtUtil = new JwtUtil(dummySecretKey);
    }

    @Test
    @DisplayName("Test/ Generate token successfully creates a JWT")
    void whenGenerateToken_thenReturnsValidJwtString() {
        // Arrange
        String userId = "ny@test.com"; // Or a UUID string, depending on what you pass

        // Act
        String token = jwtUtil.generateToken(userId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // A valid JWT always consists of exactly 3 parts separated by dots (Header.Payload.Signature)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "The token should have a Header, Payload, and Signature");
    }

    @Test
    @DisplayName("Test/ Generated token contains the correct user ID as Subject")
    void whenGenerateToken_thenTokenSubjectMatchesUserId() {
        // Arrange
        String userId = "ny@test.com";
        String token = jwtUtil.generateToken(userId);

        // Act
        // We manually parse the token back to prove your generateToken method built it correctly
        Key key = Keys.hmacShaKeyFor(dummySecretKey.getBytes(StandardCharsets.UTF_8));
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
}