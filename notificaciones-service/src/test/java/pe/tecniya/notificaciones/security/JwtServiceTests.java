package pe.tecniya.notificaciones.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

/** Prueba unitaria (sin Spring): el servicio solo acepta tokens firmados con su clave y vigentes. */
class JwtServiceTests {

    private static final String SECRETO = "clave-de-pruebas-que-supera-los-32-caracteres";
    private static final String OTRO_SECRETO = "otra-clave-distinta-que-supera-los-32-caracteres";

    private final JwtService servicio = new JwtService(SECRETO);

    private String token(String secreto, long minutosDeVida) {
        return Jwts.builder()
                .subject("5")
                .claim("email", "ana@correo.com")
                .claim("rol", "CLIENTE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + minutosDeVida * 60_000))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void leeLosDatosDeUnTokenValido() {
        JwtUser usuario = servicio.leer(token(SECRETO, 10));

        assertEquals(5L, usuario.id());
        assertEquals("ana@correo.com", usuario.email());
        assertEquals("CLIENTE", usuario.rol());
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        assertThrows(JwtException.class, () -> servicio.leer(token(OTRO_SECRETO, 10)));
    }

    @Test
    void rechazaUnTokenVencido() {
        assertThrows(JwtException.class, () -> servicio.leer(token(SECRETO, -5)));
    }

    @Test
    void rechazaTextoQueNoEsUnToken() {
        assertThrows(JwtException.class, () -> servicio.leer("esto-no-es-un-token"));
    }
}
