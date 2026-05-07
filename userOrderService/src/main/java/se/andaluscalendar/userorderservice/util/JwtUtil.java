package se.andaluscalendar.userorderservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component // @Value doesn't work unless it's a component
public class JwtUtil {
    private final Key accessKey;
    private final Key refreshKey;
    private final long accessExpirationTimeMs;
    private final long refreshExpirationTimeMs;

    public JwtUtil(
            @Value("${jwt.access.secretkey}") String accessSecretKey,
            @Value("${jwt.refresh.secretkey}") String refreshSecretKey,
            @Value("${jwt.access.expiration-ms}") long accessExpirationTimeMs,
            @Value("${jwt.refresh.expiration-ms}") long refreshExpirationTimeMs
    ) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecretKey.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecretKey.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationTimeMs = accessExpirationTimeMs;
        this.refreshExpirationTimeMs = refreshExpirationTimeMs;
    }

    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationTimeMs))
                .claim("token_type", "access")
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationTimeMs))
                .claim("token_type", "refresh")
                .signWith(refreshKey)
                .compact();
    }

    public Claims validateAndExtractRefreshClaims(String refreshToken) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(refreshKey)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        String tokenType = claims.get("token_type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }
        return claims;
    }

    public Claims validateAndExtractAccessClaims(String accessToken) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();

        String tokenType = claims.get("token_type", String.class);
        if (!"access".equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }
        return claims;
    }
}
