package com.tracenest.backend.Auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class providing cryptographic JSON Web Token (JWT) signing, parsing,
 * expiration calculation, and validation using JJWT.
 */
@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    /**
     * Generates a signed JWT access token for an authenticated principal.
     *
     * @param authentication Active Spring Security authentication token.
     * @return Compact serialized JWT string.
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateTokenFromUserId(userPrincipal.getId(), userPrincipal.getRole());
    }

    /**
     * Generates a signed JWT token containing user ID as subject and assigned role claim.
     *
     * @param userId Unique database identifier of the user.
     * @param role User authority role name.
     * @return Compact serialized JWT string.
     */
    public String generateTokenFromUserId(String userId, String role) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    /**
     * Reconstructs HMAC-SHA cryptographic signing key from Base64 configuration secret.
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * Parses and extracts the user ID subject claim from a validated JWT string.
     *
     * @param token Serialized JWT access token.
     * @return User ID stored in token subject.
     */
    public String getUserIdFromJwtToken(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    /**
     * Validates signature integrity, expiry status, and payload formatting of a JWT token.
     *
     * @param authToken Raw JWT token string from Authorization header.
     * @return {@code true} if token is valid and unexpired; {@code false} otherwise.
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
