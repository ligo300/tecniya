package pe.tecniya.solicitudes.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Solo valida tokens: el que los emite es auth-service con la misma clave. */
@Service
public class JwtService {

    private final SecretKey clave;

    public JwtService(@Value("${app.jwt.secret}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public JwtUser leer(String token) {
        Claims claims = Jwts.parser().verifyWith(clave).build()
                .parseSignedClaims(token).getPayload();
        return new JwtUser(Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("rol", String.class));
    }
}
