package se.andaluscalendar.userorderservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component // @Value doesn't work unless it's a component
public class JwtUtil {
    // I produktion (AWS), hämta denna från en miljövariabel!
    private final Key key;
    private final long expirationTime = 3600000; // 1 timme i millisekunder

    public JwtUtil(@Value("${jwt.secretkey}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }
}
