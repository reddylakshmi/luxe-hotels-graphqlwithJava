package com.luxe.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${luxe.jwt.secret:luxe-hotels-super-secret-key-minimum-256-bits-long}") String secret,
            @Value("${luxe.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String guestId, String loyaltyNumber, AuthRole role) {
        return Jwts.builder()
                .subject(guestId)
                .claim("loyaltyNumber", loyaltyNumber)
                .claim("role", role != null ? role.name() : null)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public AuthContext parseToken(String token) {
        // Mock token: "mock-jwt-{guestId}-{role}-{anything}"
        // e.g. "mock-jwt-guest-001-GUEST", "mock-jwt-admin-ADMIN"
        if (token.startsWith("mock-jwt-")) {
            String[] parts = token.split("-");
            // parts: [mock, jwt, guest, 001, ROLE, ...]  or [mock, jwt, admin, ROLE, ...]
            // guestId is parts[2..n-1] (everything between "mock-jwt-" and last segment)
            // role is the last segment if it matches an AuthRole name, else GUEST
            AuthRole role = AuthRole.GUEST;
            String roleCandidate = parts[parts.length - 1].toUpperCase();
            try { role = AuthRole.valueOf(roleCandidate); } catch (IllegalArgumentException ignored) {}
            // rebuild guestId from parts[2] up to (but not including) role segment
            int roleIndex = parts.length - 1;
            StringBuilder guestId = new StringBuilder();
            for (int i = 2; i < roleIndex; i++) {
                if (i > 2) guestId.append("-");
                guestId.append(parts[i]);
            }
            return AuthContext.of(guestId.toString(), null, role);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String guestId = claims.getSubject();
            String loyaltyNumber = claims.get("loyaltyNumber", String.class);
            String roleStr = claims.get("role", String.class);
            AuthRole role = roleStr != null ? AuthRole.valueOf(roleStr) : AuthRole.GUEST;

            return AuthContext.of(guestId, loyaltyNumber, role);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return AuthContext.anonymous();
        }
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
