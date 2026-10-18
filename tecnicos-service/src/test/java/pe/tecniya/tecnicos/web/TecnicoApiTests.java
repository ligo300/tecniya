package pe.tecniya.tecnicos.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
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
import pe.tecniya.tecnicos.dto.TecnicoDetalleResponse;
import pe.tecniya.tecnicos.repository.TecnicoRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TecnicoApiTests {

    @Autowired
    private WebTestClient cliente;

    @Autowired
    private TecnicoRepository repositorio;

    @Value("${app.jwt.secret}")
    private String secreto;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll().block();
    }

    /** Genera un token igual al que emite auth-service. */
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

    private Map<String, Object> perfil(String documento, double tarifa, List<String> especialidades) {
        return Map.of(
                "nombres", "Luis",
                "apellidos", "Quispe",
                "documento", documento,
                "telefono", "999111222",
                "zona", "Lima Norte",
                "tarifaBase", tarifa,
                "especialidades", especialidades);
    }

    private TecnicoDetalleResponse crearPerfil(long usuarioId, String documento, List<String> esp) {
        return cliente.post().uri("/api/tecnicos")
                .header(HttpHeaders.AUTHORIZATION, token(usuarioId, "TECNICO"))
                .bodyValue(perfil(documento, 50, esp))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TecnicoDetalleResponse.class)
                .returnResult().getResponseBody();
    }

    private void activar(Long id) {
        cliente.put().uri("/api/tecnicos/{id}/estado", id)
                .header(HttpHeaders.AUTHORIZATION, token(1, "ADMIN"))
                .bodyValue(Map.of("estado", "ACTIVO"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void crearPerfilDevuelve201YQuedaPendienteDeValidacion() {
        TecnicoDetalleResponse creado = crearPerfil(10, "12345678", List.of("PC", "CELULAR"));

        assertNotNull(creado.id());
        assertEquals("PENDIENTE_VALIDACION", creado.estado().name());
        assertEquals(2, creado.especialidades().size());
    }

    @Test
    void crearPerfilSinTokenDevuelve401() {
        cliente.post().uri("/api/tecnicos")
                .bodyValue(perfil("12345678", 50, List.of("PC")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void crearPerfilComoClienteDevuelve403() {
        cliente.post().uri("/api/tecnicos")
                .header(HttpHeaders.AUTHORIZATION, token(20, "CLIENTE"))
                .bodyValue(perfil("12345678", 50, List.of("PC")))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void tarifaCeroDevuelve400() {
        cliente.post().uri("/api/tecnicos")
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .bodyValue(perfil("12345678", 0, List.of("PC")))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void sinEspecialidadesDevuelve400() {
        cliente.post().uri("/api/tecnicos")
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .bodyValue(perfil("12345678", 50, List.of()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void documentoRepetidoDevuelve409() {
        crearPerfil(10, "12345678", List.of("PC"));

        cliente.post().uri("/api/tecnicos")
                .header(HttpHeaders.AUTHORIZATION, token(11, "TECNICO"))
                .bodyValue(perfil("12345678", 50, List.of("PC")))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void unTecnicoNoPuedeCrearDosPerfiles() {
        crearPerfil(10, "12345678", List.of("PC"));

        cliente.post().uri("/api/tecnicos")
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .bodyValue(perfil("87654321", 50, List.of("PC")))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void elCatalogoNoMuestraPendientesPeroSiActivos() {
        TecnicoDetalleResponse creado = crearPerfil(10, "12345678", List.of("PC"));

        cliente.get().uri("/api/tecnicos")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);

        activar(creado.id());

        cliente.get().uri("/api/tecnicos")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].documento").doesNotExist()
                .jsonPath("$[0].telefono").doesNotExist();
    }

    @Test
    void elCatalogoFiltraPorTipoDeEquipo() {
        activar(crearPerfil(10, "12345678", List.of("PC")).id());
        activar(crearPerfil(11, "87654321", List.of("TV")).id());

        cliente.get().uri("/api/tecnicos?tipo=TV")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].especialidades[0]").isEqualTo("TV");
    }

    @Test
    void elPerfilPendienteNoEsPublico() {
        TecnicoDetalleResponse creado = crearPerfil(10, "12345678", List.of("PC"));

        cliente.get().uri("/api/tecnicos/{id}", creado.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void elTecnicoVeSuPropioPerfilEnCualquierEstado() {
        crearPerfil(10, "12345678", List.of("PC"));

        cliente.get().uri("/api/tecnicos/mio")
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.estado").isEqualTo("PENDIENTE_VALIDACION");
    }

    @Test
    void soloElAdminListaTecnicosPendientes() {
        crearPerfil(10, "12345678", List.of("PC"));

        cliente.get().uri("/api/tecnicos/gestion?estado=PENDIENTE_VALIDACION")
                .header(HttpHeaders.AUTHORIZATION, token(1, "ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);

        cliente.get().uri("/api/tecnicos/gestion")
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void soloElAdminCambiaElEstado() {
        TecnicoDetalleResponse creado = crearPerfil(10, "12345678", List.of("PC"));

        cliente.put().uri("/api/tecnicos/{id}/estado", creado.id())
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .bodyValue(Map.of("estado", "ACTIVO"))
                .exchange()
                .expectStatus().isForbidden();

        cliente.put().uri("/api/tecnicos/{id}/estado", creado.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "ADMIN"))
                .bodyValue(Map.of("estado", "ACTIVO"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.estado").isEqualTo("ACTIVO");
    }

    @Test
    void unaTransicionInvalidaDevuelve400() {
        TecnicoDetalleResponse creado = crearPerfil(10, "12345678", List.of("PC"));

        cliente.put().uri("/api/tecnicos/{id}/estado", creado.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "ADMIN"))
                .bodyValue(Map.of("estado", "SUSPENDIDO"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void elTecnicoActualizaSuPerfilPeroNoElDeOtro() {
        TecnicoDetalleResponse creado = crearPerfil(10, "12345678", List.of("PC"));

        cliente.put().uri("/api/tecnicos/{id}", creado.id())
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .bodyValue(perfil("12345678", 90, List.of("PC", "TV")))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tarifaBase").isEqualTo(90.0)
                .jsonPath("$.especialidades.length()").isEqualTo(2);

        cliente.put().uri("/api/tecnicos/{id}", creado.id())
                .header(HttpHeaders.AUTHORIZATION, token(99, "TECNICO"))
                .bodyValue(perfil("12345678", 10, List.of("PC")))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void noSePuedeCambiarElDocumento() {
        TecnicoDetalleResponse creado = crearPerfil(10, "12345678", List.of("PC"));

        cliente.put().uri("/api/tecnicos/{id}", creado.id())
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .bodyValue(perfil("00000000", 50, List.of("PC")))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void elAdminEliminaAlTecnico() {
        TecnicoDetalleResponse creado = crearPerfil(10, "12345678", List.of("PC"));
        activar(creado.id());

        cliente.delete().uri("/api/tecnicos/{id}", creado.id())
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .exchange()
                .expectStatus().isForbidden();

        cliente.delete().uri("/api/tecnicos/{id}", creado.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "ADMIN"))
                .exchange()
                .expectStatus().isNoContent();

        cliente.get().uri("/api/tecnicos/{id}", creado.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void unTokenInvalidoSeTrataComoAnonimo() {
        cliente.post().uri("/api/tecnicos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-falso")
                .bodyValue(perfil("12345678", 50, List.of("PC")))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
