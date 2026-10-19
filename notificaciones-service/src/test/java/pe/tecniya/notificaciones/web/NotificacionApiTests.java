package pe.tecniya.notificaciones.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import pe.tecniya.notificaciones.client.TecnicoInfo;
import pe.tecniya.notificaciones.client.TecnicosClient;
import pe.tecniya.notificaciones.domain.Notificacion;
import pe.tecniya.notificaciones.dto.NotificacionResponse;
import pe.tecniya.notificaciones.repository.NotificacionRepository;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificacionApiTests {

    @Autowired
    private WebTestClient cliente;
    @Autowired
    private NotificacionRepository repositorio;

    /** tecnicos-service se simula: aquí no se llama a ningún otro servicio. */
    @MockitoBean
    private TecnicosClient tecnicosClient;

    @Value("${app.jwt.secret}")
    private String secreto;

    private Notificacion clienteUno1;

    @BeforeEach
    void preparar() {
        repositorio.deleteAll().block();
        clienteUno1 = guardar("CLIENTE", 1, "cliente 1 - primera");
        guardar("CLIENTE", 1, "cliente 1 - segunda");
        guardar("TECNICO", 100, "técnico 100");
        guardar("ADMIN", 0, "para el administrador");
    }

    private Notificacion guardar(String tipo, long id, String mensaje) {
        Notificacion n = new Notificacion();
        n.setDestinatarioTipo(tipo);
        n.setDestinatarioId(id);
        n.setEvento("OrdenCreada");
        n.setMensaje(mensaje);
        n.setLeida(false);
        n.setCreadaEn(LocalDateTime.now());
        return repositorio.save(n).block();
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

    private String tokenTecnico(long usuarioId, long perfilId) {
        String token = token(usuarioId, "TECNICO");
        when(tecnicosClient.miPerfil(token)).thenReturn(Mono.just(
                new TecnicoInfo(perfilId, "ACTIVO", new BigDecimal("50"), List.of("PC"))));
        return token;
    }

    private List<NotificacionResponse> listar(String token, String consulta) {
        return cliente.get().uri("/api/notificaciones" + consulta)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificacionResponse.class)
                .returnResult().getResponseBody();
    }

    @Test
    void cadaUsuarioVeSoloLasSuyasDeLaMasNuevaALaMasAntigua() {
        List<NotificacionResponse> delCliente = listar(token(1, "CLIENTE"), "");
        assertEquals(2, delCliente.size());
        assertEquals("cliente 1 - segunda", delCliente.get(0).mensaje());

        assertEquals(0, listar(token(2, "CLIENTE"), "").size());
        assertEquals("técnico 100", listar(tokenTecnico(10, 100), "").get(0).mensaje());
        assertEquals("para el administrador", listar(token(99, "ADMIN"), "").get(0).mensaje());
    }

    @Test
    void marcarComoLeidaYFiltrarPorNoLeidas() {
        cliente.put().uri("/api/notificaciones/{id}/leida", clienteUno1.getId())
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(NotificacionResponse.class)
                .value(n -> assertTrue(n.leida()));

        List<NotificacionResponse> pendientes = listar(token(1, "CLIENTE"), "?soloNoLeidas=true");
        assertEquals(1, pendientes.size());
        assertFalse(pendientes.get(0).leida());
        assertEquals(2, listar(token(1, "CLIENTE"), "").size());
    }

    @Test
    void nadieMarcaLasNotificacionesDeOtro() {
        cliente.put().uri("/api/notificaciones/{id}/leida", clienteUno1.getId())
                .header(HttpHeaders.AUTHORIZATION, token(2, "CLIENTE"))
                .exchange().expectStatus().isForbidden();
        cliente.put().uri("/api/notificaciones/{id}/leida", clienteUno1.getId())
                .header(HttpHeaders.AUTHORIZATION, tokenTecnico(10, 100))
                .exchange().expectStatus().isForbidden();
        cliente.put().uri("/api/notificaciones/{id}/leida", 99999)
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void exigeEstarAutenticado() {
        cliente.get().uri("/api/notificaciones").exchange().expectStatus().isUnauthorized();
        cliente.put().uri("/api/notificaciones/{id}/leida", clienteUno1.getId())
                .exchange().expectStatus().isUnauthorized();
    }
}
