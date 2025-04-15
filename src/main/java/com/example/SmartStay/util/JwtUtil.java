package com.example.SmartStay.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.example.SmartStay.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Algorithm secondaryAlgorithm;
    private final JWTVerifier secondaryVerifier;

    /**
     * Constructor for JwtUtil.
     * The reason to use the secondary secret key is because
     * we want to be able to generate tokens for users who need to
     * confirm their email address. We don't want to use the same secret key
     * because the jwt token generated for email confirmation not expires.
     * In any case, if someone with a quantum computer can break the secret key of the emails
     * they will not be able to generate tokens for users who are already logged in.
     * @param secretKey
     * @param secondarySecretKey
     */
    public JwtUtil(@Value("${JWT_SECRET_KEY}") String secretKey, 
        @Value("${JWT_SECONDARY_SECRET_KEY}") String secondarySecretKey) {
            this.algorithm = Algorithm.HMAC256(secretKey);
            this.verifier = JWT.require(algorithm).build();
            this.secondaryAlgorithm = Algorithm.HMAC256(secondarySecretKey);
            this.secondaryVerifier = JWT.require(secondaryAlgorithm).build();
        }

    public String generateToken(User user) {
        return JWT.create()
                .withSubject(user.getEmail())
                .withClaim("id", user.getId())
                .withClaim("name", user.getFirstName())
                .withClaim("lastName", user.getLastName())
                .withClaim("isAdmin", user.isAdmin())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 24 hours
                .sign(algorithm);
    }

    public String generateConfirmUserToken(String email) {
        return JWT.create()
                .withSubject(email)
                .withIssuedAt(new Date())
                .sign(secondaryAlgorithm);
    }

    public boolean verifyConfirmUserToken(String token) {
        try {
            secondaryVerifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailFromConfirmUserToken(String token) {
        try {
            DecodedJWT claims = secondaryVerifier.verify(token);
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private String getStringFromToken(String token) {
        // split token into parts
        String[] parts = token.split(" ");
        if (parts.length != 2) {
            return null;
        }

        return parts[1];
    }

    private DecodedJWT extractClaims(String token) {
        try {
            String tokenString = getStringFromToken(token);
            if (tokenString == null) {
                return null;
            }

            return verifier.verify(tokenString);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isTokenExpired(String token) {
        DecodedJWT claims = extractClaims(token);
        if (claims == null) {
            return true;
        }
        return claims.getExpiresAt().before(new Date());
    }

    public boolean isValidToken(String token) {
        String tokenString = getStringFromToken(token);
        if (tokenString == null) {
            return false;
        }

        return !isTokenExpired(token);
    }

    public boolean isAdmin(String token) {
        if (!isValidToken(token)) return false;

        DecodedJWT claims = extractClaims(token);
        return claims.getClaim("isAdmin").asBoolean();
    }

    public String getEmail(String token) {
        if (!isValidToken(token)) return null;

        DecodedJWT claims = extractClaims(token);
        return claims.getSubject();
    }

    public String getId(String token) {
        if (!isValidToken(token)) return null;

        DecodedJWT claims = extractClaims(token);
        return claims.getClaim("id").asString();
    }
}