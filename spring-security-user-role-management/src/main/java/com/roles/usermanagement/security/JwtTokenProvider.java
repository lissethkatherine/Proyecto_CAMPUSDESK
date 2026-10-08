package com.roles.usermanagement.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * Componente de utilidad para la generación, firmado y validación de tokens JWT.
 * Utiliza el algoritmo HMAC256 y la biblioteca com.auth0:java-jwt.
 */
@Component
public class JwtTokenProvider {

    private final String secretKey;
    private final String issuer;
    private final long expirationTimeInMs;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenProvider(
            @Value("${security.jwt.secret:User_M4n4gement_Secret_Key_2026}") String secretKey,
            @Value("${security.jwt.issuer:User_Role_Management_API}") String issuer,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationTimeInMs
    ) {
        this.secretKey = secretKey;
        this.issuer = issuer;
        this.expirationTimeInMs = expirationTimeInMs;
        this.algorithm = Algorithm.HMAC256(secretKey);
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
    }

    /**
     * Genera un token JWT para un usuario especificando su correo o nombre de usuario y su rol.
     *
     * @param username El correo o nombre de usuario.
     * @param role El rol asignado al usuario.
     * @return El token JWT firmado.
     */
    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationTimeInMs);

        return JWT.create()
                .withSubject(username)
                .withIssuer(issuer)
                .withClaim("role", role)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiration))
                .sign(algorithm);
    }

    /**
     * Valida si un token JWT es válido, no ha expirado y mantiene su firma intacta.
     *
     * @param token El token JWT a validar.
     * @return true si el token es válido, false en caso contrario.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException exception) {
            return false;
        }
    }

    /**
     * Extrae el usuario o subject contenido dentro del token JWT.
     *
     * @param token El token JWT.
     * @return El nombre de usuario o email.
     */
    public String getUsernameFromToken(String token) {
        return verifier.verify(token).getSubject();
    }

    /**
     * Extrae el rol del usuario contenido en las declaraciones (claims) del token JWT.
     *
     * @param token El token JWT.
     * @return El nombre del rol.
     */
    public String getRoleFromToken(String token) {
        return verifier.verify(token).getClaim("role").asString();
    }
}
