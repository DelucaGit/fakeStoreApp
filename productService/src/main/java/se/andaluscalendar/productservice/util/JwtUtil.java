package se.andaluscalendar.productservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtUtil {

    private final Key accessKey;

    public JwtUtil(@Value("${jwt.access.secretkey}") String accessSecretKey) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecretKey.getBytes(StandardCharsets.UTF_8));
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

    public String validateAndExtractUserId(String accessToken) {
        return validateAndExtractAccessClaims(accessToken).getSubject();
    }
}
