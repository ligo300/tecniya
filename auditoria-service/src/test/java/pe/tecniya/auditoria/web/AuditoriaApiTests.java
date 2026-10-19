package pe.tecniya.auditoria.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import pe.tecniya.auditoria.dto.AuditoriaResponse;
import pe.tecniya.auditoria.eventos.EventoConsumidor;
import pe.tecniya.auditoria.eventos.EventoTecniya;
import pe.tecniya.auditoria.repository.AuditoriaRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuditoriaApiTests {

    @Autowired
    private WebTestClient cliente;
    @Autowired
    private AuditoriaRepository repositorio;
    @Autowired
    private EventoConsumidor consumidor;

    @Value("${app.jwt.secret}")
    private String secreto;

    @BeforeEach
    void preparar() {
        repositorio.deleteAll().block();
        consumidor.recibir(evento("OrdenCreada", 1));
        consumidor.recibir(evento("OrdenCambioEstado", 1));
        consumidor.recibir(evento("OrdenCreada", 2));
    }

    private EventoTecniya evento(String tipo, int ordenId) {
        return new EventoTecniya(tipo, LocalDateTime.now().toString(),
                Map.of("ordenId", ordenId, "codigo", "ORD-" + ordenId));
    }

    private String token(long usuarioId, String rol) {
        SecretKey clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        return "Bearer " + Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("email", "usuario" + usuarioId + "@correo.com")
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(clave)
                .compact();
    }

    private List<AuditoriaResponse> listar(String consulta) {
        return cliente.get().uri("/api/auditoria" + consulta)
                .header(HttpHeaders.AUTHORIZATION, token(99, "ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AuditoriaResponse.class)
                .returnResult().getResponseBody();
    }

    @Test
    void elAdministradorVeLosRegistrosDelMasNuevoAlMasAntiguo() {
        List<AuditoriaResponse> lista = listar("");
        assertEquals(3, lista.size());
        assertEquals(2L, lista.get(0).entidadId());
        assertEquals("OrdenCambioEstado", lista.get(1).tipo());
    }

    @Test
    void filtraPorTipo() {
        assertEquals(2, listar("?tipo=OrdenCreada").size());
        assertEquals(0, listar("?tipo=PagoRegistrado").size());
    }

    @Test
    void soloElAdministradorPuedeConsultar() {
        cliente.get().uri("/api/auditoria")
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/auditoria")
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/auditoria")
                .exchange().expectStatus().isUnauthorized();
    }
}
