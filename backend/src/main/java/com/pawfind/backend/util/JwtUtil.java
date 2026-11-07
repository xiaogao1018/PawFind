
package com.pawfind.backend.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {

        this.key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId) {

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + expirationMs
        );

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}
