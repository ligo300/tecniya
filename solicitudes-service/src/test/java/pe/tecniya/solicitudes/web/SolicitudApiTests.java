package pe.tecniya.solicitudes.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import pe.tecniya.solicitudes.client.OrdenesClient;
import pe.tecniya.solicitudes.client.TecnicoInfo;
import pe.tecniya.solicitudes.client.TecnicosClient;
import pe.tecniya.solicitudes.dto.OfertaResponse;
import pe.tecniya.solicitudes.eventos.EventoPublisherEnMemoria;
import pe.tecniya.solicitudes.dto.SolicitudResponse;
import pe.tecniya.solicitudes.exception.ReglaNegocioException;
import pe.tecniya.solicitudes.repository.OfertaRepository;
import pe.tecniya.solicitudes.repository.SolicitudRepository;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SolicitudApiTests {

    @Autowired
    private WebTestClient cliente;

    @Autowired
    private SolicitudRepository solicitudes;

    @Autowired
    private OfertaRepository ofertas;

    /** Sin RabbitMQ en las pruebas: los eventos quedan en memoria. */
    @Autowired
    private EventoPublisherEnMemoria eventos;

    /** tecnicos-service se simula: aquí no se llama a ningún otro servicio. */
    @MockitoBean
    private TecnicosClient tecnicosClient;

    /** ordenes-service también se simula; se comprueba que se le pida crear la orden. */
    @MockitoBean
    private OrdenesClient ordenesClient;

    @Value("${app.jwt.secret}")
    private String secreto;

    @BeforeEach
    void limpiar() {
        eventos.limpiar();
        when(ordenesClient.crearOrdenDesdeOferta(any(), any())).thenReturn(Mono.empty());
        ofertas.deleteAll().then(solicitudes.deleteAll()).block();
    }

    // ---------- Apoyo ----------

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

    /** Crea un token de técnico y simula el perfil que devolvería tecnicos-service. */
    private String tokenTecnico(long usuarioId, long perfilId, String estado,
                                String... especialidades) {
        String token = token(usuarioId, "TECNICO");
        when(tecnicosClient.miPerfil(token)).thenReturn(Mono.just(
                new TecnicoInfo(perfilId, estado, new BigDecimal("50"),
                        List.of(especialidades))));
        return token;
    }

    private Map<String, Object> datosSolicitud(String tipo, double presupuesto) {
        return Map.of(
                "tipoEquipo", tipo,
                "marcaModelo", "HP Pavilion",
                "descripcionFalla", "No enciende",
                "presupuestoRef", presupuesto);
    }

    private SolicitudResponse crearSolicitud(long clienteId, String tipo) {
        return cliente.post().uri("/api/solicitudes")
                .header(HttpHeaders.AUTHORIZATION, token(clienteId, "CLIENTE"))
                .bodyValue(datosSolicitud(tipo, 100))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SolicitudResponse.class)
                .returnResult().getResponseBody();
    }

    private Map<String, Object> datosOferta(double precio) {
        return Map.of("precio", precio, "plazoDias", 3, "mensaje", "Lo reviso mañana");
    }

    private OfertaResponse ofertar(Long solicitudId, String tokenTecnico, double precio) {
        return cliente.post().uri("/api/solicitudes/{id}/ofertas", solicitudId)
                .header(HttpHeaders.AUTHORIZATION, tokenTecnico)
                .bodyValue(datosOferta(precio))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OfertaResponse.class)
                .returnResult().getResponseBody();
    }

    private SolicitudResponse verSolicitud(Long id, long clienteId) {
        return cliente.get().uri("/api/solicitudes/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, token(clienteId, "CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(SolicitudResponse.class)
                .returnResult().getResponseBody();
    }

    private List<OfertaResponse> verOfertas(Long solicitudId, long clienteId) {
        return cliente.get().uri("/api/solicitudes/{id}/ofertas", solicitudId)
                .header(HttpHeaders.AUTHORIZATION, token(clienteId, "CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OfertaResponse.class)
                .returnResult().getResponseBody();
    }

    // ---------- Solicitudes ----------

    @Test
    void crearSolicitudDevuelve201YQuedaAbierta() {
        SolicitudResponse s = crearSolicitud(1, "PC");

        assertNotNull(s.id());
        assertEquals("ABIERTA", s.estado().name());
        assertEquals(1L, s.clienteId());
    }

    @Test
    void crearSolicitudSinTokenDevuelve401() {
        cliente.post().uri("/api/solicitudes")
                .bodyValue(datosSolicitud("PC", 100))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void unTecnicoNoPuedeCrearSolicitudes() {
        cliente.post().uri("/api/solicitudes")
                .header(HttpHeaders.AUTHORIZATION, token(5, "TECNICO"))
                .bodyValue(datosSolicitud("PC", 100))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void presupuestoCeroDevuelve400() {
        cliente.post().uri("/api/solicitudes")
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .bodyValue(datosSolicitud("PC", 0))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void elClienteSoloVeSusSolicitudes() {
        crearSolicitud(1, "PC");

        cliente.get().uri("/api/solicitudes")
                .header(HttpHeaders.AUTHORIZATION, token(2, "CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(0);

        cliente.get().uri("/api/solicitudes")
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void elTecnicoSoloVeSolicitudesDeSuEspecialidad() {
        crearSolicitud(1, "PC");
        crearSolicitud(1, "TV");
        String tecnico = tokenTecnico(10, 100, "ACTIVO", "PC");

        cliente.get().uri("/api/solicitudes")
                .header(HttpHeaders.AUTHORIZATION, tecnico)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].tipoEquipo").isEqualTo("PC");
    }

    @Test
    void soloSePuedeEditarUnaSolicitudAbierta() {
        SolicitudResponse s = crearSolicitud(1, "PC");

        cliente.put().uri("/api/solicitudes/{id}", s.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .bodyValue(datosSolicitud("PC", 250))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.presupuestoRef").isEqualTo(250.0);

        ofertar(s.id(), tokenTecnico(10, 100, "ACTIVO", "PC"), 80);

        cliente.put().uri("/api/solicitudes/{id}", s.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .bodyValue(datosSolicitud("PC", 300))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void cancelarSolicitudDevuelve204YRechazaLasOfertas() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        ofertar(s.id(), tokenTecnico(10, 100, "ACTIVO", "PC"), 80);

        cliente.delete().uri("/api/solicitudes/{id}", s.id())
                .header(HttpHeaders.AUTHORIZATION, token(2, "CLIENTE"))
                .exchange()
                .expectStatus().isForbidden();

        cliente.delete().uri("/api/solicitudes/{id}", s.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange()
                .expectStatus().isNoContent();

        assertEquals("CANCELADA", verSolicitud(s.id(), 1).estado().name());
        assertEquals("RECHAZADA", verOfertas(s.id(), 1).get(0).estado().name());
    }

    // ---------- Ofertas ----------

    @Test
    void unTecnicoActivoConLaEspecialidadPuedeOfertar() {
        SolicitudResponse s = crearSolicitud(1, "PC");

        OfertaResponse o = ofertar(s.id(), tokenTecnico(10, 100, "ACTIVO", "PC", "TV"), 80);

        assertEquals(100L, o.tecnicoId());
        assertEquals("PENDIENTE", o.estado().name());
        assertEquals("CON_OFERTAS", verSolicitud(s.id(), 1).estado().name());
    }

    @Test
    void unTecnicoSinLaEspecialidadNoPuedeOfertar() {
        SolicitudResponse s = crearSolicitud(1, "PC");

        cliente.post().uri("/api/solicitudes/{id}/ofertas", s.id())
                .header(HttpHeaders.AUTHORIZATION, tokenTecnico(10, 100, "ACTIVO", "TV"))
                .bodyValue(datosOferta(80))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void unTecnicoNoActivoNoPuedeOfertar() {
        SolicitudResponse s = crearSolicitud(1, "PC");

        cliente.post().uri("/api/solicitudes/{id}/ofertas", s.id())
                .header(HttpHeaders.AUTHORIZATION,
                        tokenTecnico(10, 100, "PENDIENTE_VALIDACION", "PC"))
                .bodyValue(datosOferta(80))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void unTecnicoSinPerfilRecibe400() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        String token = token(10, "TECNICO");
        when(tecnicosClient.miPerfil(token)).thenReturn(Mono.error(
                new ReglaNegocioException("Aún no has creado tu perfil de técnico")));

        cliente.post().uri("/api/solicitudes/{id}/ofertas", s.id())
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(datosOferta(80))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void unTecnicoNoPuedeOfertarDosVecesLaMismaSolicitud() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        String tecnico = tokenTecnico(10, 100, "ACTIVO", "PC");
        ofertar(s.id(), tecnico, 80);

        cliente.post().uri("/api/solicitudes/{id}/ofertas", s.id())
                .header(HttpHeaders.AUTHORIZATION, tecnico)
                .bodyValue(datosOferta(70))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void elPrecioCeroDevuelve400() {
        SolicitudResponse s = crearSolicitud(1, "PC");

        cliente.post().uri("/api/solicitudes/{id}/ofertas", s.id())
                .header(HttpHeaders.AUTHORIZATION, tokenTecnico(10, 100, "ACTIVO", "PC"))
                .bodyValue(datosOferta(0))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void soloElClienteDuenoVeLasOfertasOrdenadasPorPrecio() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        ofertar(s.id(), tokenTecnico(10, 100, "ACTIVO", "PC"), 150);
        ofertar(s.id(), tokenTecnico(11, 101, "ACTIVO", "PC"), 60);

        cliente.get().uri("/api/solicitudes/{id}/ofertas", s.id())
                .header(HttpHeaders.AUTHORIZATION, token(2, "CLIENTE"))
                .exchange()
                .expectStatus().isForbidden();

        List<OfertaResponse> lista = verOfertas(s.id(), 1);
        assertEquals(2, lista.size());
        assertEquals(101L, lista.get(0).tecnicoId());
    }

    @Test
    void elTecnicoModificaSuOfertaPeroNoLaDeOtro() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        String duenio = tokenTecnico(10, 100, "ACTIVO", "PC");
        String otro = tokenTecnico(11, 101, "ACTIVO", "PC");
        OfertaResponse o = ofertar(s.id(), duenio, 150);

        cliente.put().uri("/api/ofertas/{id}", o.id())
                .header(HttpHeaders.AUTHORIZATION, otro)
                .bodyValue(datosOferta(10))
                .exchange()
                .expectStatus().isForbidden();

        cliente.put().uri("/api/ofertas/{id}", o.id())
                .header(HttpHeaders.AUTHORIZATION, duenio)
                .bodyValue(datosOferta(90))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.precio").isEqualTo(90.0);
    }

    @Test
    void retirarLaUltimaOfertaDevuelveLaSolicitudAAbierta() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        String tecnico = tokenTecnico(10, 100, "ACTIVO", "PC");
        OfertaResponse o = ofertar(s.id(), tecnico, 80);

        cliente.delete().uri("/api/ofertas/{id}", o.id())
                .header(HttpHeaders.AUTHORIZATION, tecnico)
                .exchange()
                .expectStatus().isNoContent();

        assertEquals("ABIERTA", verSolicitud(s.id(), 1).estado().name());
        assertEquals(0, verOfertas(s.id(), 1).size());
    }

    // ---------- Aceptar ----------

    @Test
    void aceptarUnaOfertaAdjudicaLaSolicitudYRechazaLasDemas() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        OfertaResponse primera = ofertar(s.id(), tokenTecnico(10, 100, "ACTIVO", "PC"), 150);
        OfertaResponse segunda = ofertar(s.id(), tokenTecnico(11, 101, "ACTIVO", "PC"), 60);

        cliente.post().uri("/api/ofertas/{id}/aceptar", segunda.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.estado").isEqualTo("ACEPTADA");

        verify(ordenesClient, times(1)).crearOrdenDesdeOferta(any(), any());
        assertEquals(List.of("oferta.aceptada"), eventos.claves());
        assertEquals("ADJUDICADA", verSolicitud(s.id(), 1).estado().name());
        List<OfertaResponse> lista = verOfertas(s.id(), 1);
        assertEquals("ACEPTADA", lista.get(0).estado().name());
        assertEquals(primera.id(), lista.get(1).id());
        assertEquals("RECHAZADA", lista.get(1).estado().name());

        cliente.post().uri("/api/solicitudes/{id}/ofertas", s.id())
                .header(HttpHeaders.AUTHORIZATION, tokenTecnico(12, 102, "ACTIVO", "PC"))
                .bodyValue(datosOferta(50))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void soloElDuenoDeLaSolicitudAceptaOfertas() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        OfertaResponse o = ofertar(s.id(), tokenTecnico(10, 100, "ACTIVO", "PC"), 80);

        cliente.post().uri("/api/ofertas/{id}/aceptar", o.id())
                .header(HttpHeaders.AUTHORIZATION, token(2, "CLIENTE"))
                .exchange()
                .expectStatus().isForbidden();

        cliente.post().uri("/api/ofertas/{id}/aceptar", o.id())
                .header(HttpHeaders.AUTHORIZATION, tokenTecnico(10, 100, "ACTIVO", "PC"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void unaOfertaYaAceptadaNoSePuedeAceptarDeNuevo() {
        SolicitudResponse s = crearSolicitud(1, "PC");
        OfertaResponse o = ofertar(s.id(), tokenTecnico(10, 100, "ACTIVO", "PC"), 80);

        cliente.post().uri("/api/ofertas/{id}/aceptar", o.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange()
                .expectStatus().isOk();

        cliente.post().uri("/api/ofertas/{id}/aceptar", o.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
