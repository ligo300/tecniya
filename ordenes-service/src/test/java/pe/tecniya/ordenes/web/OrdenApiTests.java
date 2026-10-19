package pe.tecniya.ordenes.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import pe.tecniya.ordenes.client.TecnicoInfo;
import pe.tecniya.ordenes.client.TecnicosClient;
import pe.tecniya.ordenes.dto.FotoResponse;
import pe.tecniya.ordenes.dto.OrdenDetalleResponse;
import pe.tecniya.ordenes.dto.OrdenResponse;
import pe.tecniya.ordenes.exception.RecursoNoEncontradoException;
import pe.tecniya.ordenes.repository.CalificacionRepository;
import pe.tecniya.ordenes.repository.FotoRepository;
import pe.tecniya.ordenes.repository.HistorialRepository;
import pe.tecniya.ordenes.repository.OrdenRepository;
import pe.tecniya.ordenes.repository.PagoRepository;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrdenApiTests {

    @Autowired
    private WebTestClient cliente;
    @Autowired
    private OrdenRepository ordenes;
    @Autowired
    private HistorialRepository historial;
    @Autowired
    private FotoRepository fotos;
    @Autowired
    private PagoRepository pagos;
    @Autowired
    private CalificacionRepository calificaciones;

    /** tecnicos-service se simula: aquí no se llama a ningún otro servicio. */
    @MockitoBean
    private TecnicosClient tecnicosClient;

    @Value("${app.jwt.secret}")
    private String secreto;
    @Value("${app.internal.key}")
    private String claveInterna;

    private long contadorOferta = 1000;

    @BeforeEach
    void limpiar() {
        calificaciones.deleteAll()
                .then(pagos.deleteAll())
                .then(fotos.deleteAll())
                .then(historial.deleteAll())
                .then(ordenes.deleteAll())
                .block();
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

    /** Token de técnico + el perfil que devolvería tecnicos-service para ese token. */
    private String tokenTecnico(long usuarioId, long perfilId) {
        String token = token(usuarioId, "TECNICO");
        when(tecnicosClient.miPerfil(token)).thenReturn(Mono.just(
                new TecnicoInfo(perfilId, "ACTIVO", new BigDecimal("50"), List.of("PC"))));
        return token;
    }

    private OrdenResponse crearOrden(long clienteId, long perfilTecnicoId, double precio) {
        Map<String, Object> cuerpo = Map.of(
                "clienteId", clienteId,
                "tecnicoId", perfilTecnicoId,
                "solicitudId", 5,
                "ofertaId", ++contadorOferta,
                "tipoEquipo", "PC",
                "descripcion", "HP Pavilion — No enciende",
                "precioAcordado", precio);
        return cliente.post().uri("/internal/ordenes/desde-oferta")
                .header("X-Internal-Key", claveInterna)
                .bodyValue(cuerpo)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrdenResponse.class)
                .returnResult().getResponseBody();
    }

    private byte[] jpg(int tamano) {
        byte[] b = new byte[tamano];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        b[3] = (byte) 0xE0;
        return b;
    }

    private WebTestClient.ResponseSpec subirFoto(long ordenId, String token, String estado,
                                                 byte[] bytes) {
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("archivo", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "foto.jpg";
            }
        }).contentType(MediaType.IMAGE_JPEG);
        mb.part("estado", estado);
        return cliente.post().uri("/api/ordenes/{id}/fotos", ordenId)
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(mb.build()))
                .exchange();
    }

    private WebTestClient.ResponseSpec cambiarEstado(long id, String token, String estado) {
        return cliente.put().uri("/api/ordenes/{id}/estado", id)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(Map.of("estado", estado))
                .exchange();
    }

    private void avanzar(long id, String tokenTec, String estado) {
        subirFoto(id, tokenTec, estado, jpg(2048)).expectStatus().isCreated();
        cambiarEstado(id, tokenTec, estado).expectStatus().isOk();
    }

    private void llevarAListo(long id, String tokenTec) {
        for (String e : List.of("RECIBIDO", "EN_DIAGNOSTICO", "EN_REPARACION", "LISTO")) {
            avanzar(id, tokenTec, e);
        }
    }

    private WebTestClient.ResponseSpec pagar(long id, String token, double monto, String nro) {
        return cliente.post().uri("/api/ordenes/{id}/pago", id)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(Map.of("monto", monto, "medio", "YAPE", "nroOperacion", nro))
                .exchange();
    }

    private WebTestClient.ResponseSpec calificar(long id, String token, int puntaje) {
        return cliente.post().uri("/api/ordenes/{id}/calificacion", id)
                .header(HttpHeaders.AUTHORIZATION, token)
                .bodyValue(Map.of("puntaje", puntaje, "comentario", "Buen trabajo"))
                .exchange();
    }

    private WebTestClient.ResponseSpec cancelar(long id, String token) {
        return cliente.put().uri("/api/ordenes/{id}/cancelar", id)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange();
    }

    private OrdenDetalleResponse detalle(long id, String token) {
        return cliente.get().uri("/api/ordenes/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrdenDetalleResponse.class)
                .returnResult().getResponseBody();
    }

    // ---------- Creación ----------

    @Test
    void solicitudesCreaLaOrdenYEsIdempotente() {
        Map<String, Object> cuerpo = Map.of("clienteId", 1, "tecnicoId", 100,
                "solicitudId", 5, "ofertaId", 77, "tipoEquipo", "PC",
                "descripcion", "No enciende", "precioAcordado", 90);
        OrdenResponse primera = null;
        for (int i = 0; i < 2; i++) {
            OrdenResponse r = cliente.post().uri("/internal/ordenes/desde-oferta")
                    .header("X-Internal-Key", claveInterna)
                    .bodyValue(cuerpo)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(OrdenResponse.class)
                    .returnResult().getResponseBody();
            if (primera == null) {
                primera = r;
            } else {
                assertEquals(primera.id(), r.id());
            }
        }
        assertEquals("CONTRATADO", primera.estado().name());
        assertEquals("OFERTA", primera.origen().name());
        assertEquals(1L, ordenes.count().block());
    }

    @Test
    void elEndpointInternoExigeLaClave() {
        Map<String, Object> cuerpo = Map.of("clienteId", 1, "tecnicoId", 100,
                "solicitudId", 5, "ofertaId", 78, "tipoEquipo", "PC",
                "descripcion", "No enciende", "precioAcordado", 90);
        cliente.post().uri("/internal/ordenes/desde-oferta")
                .header("X-Internal-Key", "otra-clave")
                .bodyValue(cuerpo).exchange().expectStatus().isForbidden();
        cliente.post().uri("/internal/ordenes/desde-oferta")
                .bodyValue(cuerpo).exchange().expectStatus().isForbidden();
        assertEquals(0L, ordenes.count().block());
    }

    @Test
    void contratacionDirectaUsaLaTarifaBase() {
        when(tecnicosClient.perfilPublico(100L)).thenReturn(Mono.just(
                new TecnicoInfo(100L, "ACTIVO", new BigDecimal("50"), List.of("PC", "TV"))));

        cliente.post().uri("/api/ordenes/directa")
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .bodyValue(Map.of("tecnicoId", 100, "tipoEquipo", "TV",
                        "descripcion", "Sin imagen"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.origen").isEqualTo("DIRECTA")
                .jsonPath("$.estado").isEqualTo("CONTRATADO")
                .jsonPath("$.precioAcordado").isEqualTo(50)
                .jsonPath("$.clienteId").isEqualTo(1)
                .jsonPath("$.tecnicoId").isEqualTo(100);
    }

    @Test
    void contratacionDirectaValidaTecnicoYPermisos() {
        when(tecnicosClient.perfilPublico(101L)).thenReturn(Mono.just(
                new TecnicoInfo(101L, "SUSPENDIDO", new BigDecimal("50"), List.of("PC"))));
        when(tecnicosClient.perfilPublico(102L)).thenReturn(Mono.just(
                new TecnicoInfo(102L, "ACTIVO", new BigDecimal("50"), List.of("TV"))));
        when(tecnicosClient.perfilPublico(103L)).thenReturn(Mono.error(
                new RecursoNoEncontradoException("Técnico no encontrado")));
        Map<String, Object> base = Map.of("tipoEquipo", "PC", "descripcion", "No enciende");

        for (long id : new long[] {101, 102}) {
            cliente.post().uri("/api/ordenes/directa")
                    .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                    .bodyValue(Map.of("tecnicoId", id, "tipoEquipo", "PC",
                            "descripcion", "No enciende"))
                    .exchange().expectStatus().isBadRequest();
        }
        cliente.post().uri("/api/ordenes/directa")
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .bodyValue(Map.of("tecnicoId", 103, "tipoEquipo", "PC",
                        "descripcion", "No enciende"))
                .exchange().expectStatus().isNotFound();
        cliente.post().uri("/api/ordenes/directa")
                .header(HttpHeaders.AUTHORIZATION, token(10, "TECNICO"))
                .bodyValue(Map.of("tecnicoId", 100, "tipoEquipo", base.get("tipoEquipo"),
                        "descripcion", "No enciende"))
                .exchange().expectStatus().isForbidden();
        cliente.post().uri("/api/ordenes/directa")
                .bodyValue(Map.of("tecnicoId", 100, "tipoEquipo", "PC",
                        "descripcion", "No enciende"))
                .exchange().expectStatus().isUnauthorized();
        cliente.post().uri("/api/ordenes/directa")
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .bodyValue(Map.of("tecnicoId", 100, "tipoEquipo", "PC"))
                .exchange().expectStatus().isBadRequest();
    }

    // ---------- Flujo completo ----------

    @Test
    void flujoCompletoConFotosPagoYCalificacion() {
        OrdenResponse o = crearOrden(1, 100, 100);
        String tec = tokenTecnico(10, 100);
        String cli = token(1, "CLIENTE");

        llevarAListo(o.id(), tec);
        pagar(o.id(), cli, 100, "OP-001").expectStatus().isCreated()
                .expectBody().jsonPath("$.estado").isEqualTo("PAGADO");
        cambiarEstado(o.id(), tec, "ENTREGADO").expectStatus().isOk()
                .expectBody().jsonPath("$.estado").isEqualTo("ENTREGADO");
        calificar(o.id(), cli, 5).expectStatus().isCreated();

        OrdenDetalleResponse d = detalle(o.id(), cli);
        assertEquals("ENTREGADO", d.orden().estado().name());
        assertEquals(6, d.historial().size());
        assertNull(d.historial().get(0).estadoAnterior());
        assertEquals("CONTRATADO", d.historial().get(0).estadoNuevo().name());
        assertEquals("ENTREGADO", d.historial().get(5).estadoNuevo().name());
        assertEquals(4, d.fotos().size());
        assertEquals(1, d.pagos().size());
        assertNotNull(d.calificacion());
        assertEquals(5, d.calificacion().puntaje());
    }

    // ---------- Reglas de estado y fotos ----------

    @Test
    void noSePuedeCambiarDeEstadoSinFoto() {
        OrdenResponse o = crearOrden(1, 100, 100);
        cambiarEstado(o.id(), tokenTecnico(10, 100), "RECIBIDO")
                .expectStatus().isBadRequest();
        assertEquals("CONTRATADO", detalle(o.id(), token(1, "CLIENTE")).orden().estado().name());
    }

    @Test
    void noSePuedeSaltarUnEstado() {
        OrdenResponse o = crearOrden(1, 100, 100);
        cambiarEstado(o.id(), tokenTecnico(10, 100), "LISTO")
                .expectStatus().isBadRequest();
    }

    @Test
    void laFotoDebeSerDelSiguienteEstado() {
        OrdenResponse o = crearOrden(1, 100, 100);
        String tec = tokenTecnico(10, 100);
        subirFoto(o.id(), tec, "LISTO", jpg(1024)).expectStatus().isBadRequest();
        subirFoto(o.id(), tec, "ENTREGADO", jpg(1024)).expectStatus().isBadRequest();
        subirFoto(o.id(), tec, "XYZ", jpg(1024)).expectStatus().isBadRequest();
    }

    @Test
    void soloSeAceptanImagenesJpgOPng() {
        OrdenResponse o = crearOrden(1, 100, 100);
        byte[] texto = "esto no es una imagen".getBytes(StandardCharsets.UTF_8);
        subirFoto(o.id(), tokenTecnico(10, 100), "RECIBIDO", texto)
                .expectStatus().isBadRequest();
    }

    @Test
    void unaFotoDeMasDe5MbSeRechazaConError413() {
        OrdenResponse o = crearOrden(1, 100, 100);
        subirFoto(o.id(), tokenTecnico(10, 100), "RECIBIDO", jpg(5 * 1024 * 1024 + 1))
                .expectStatus().isEqualTo(413);
    }

    @Test
    void soloElTecnicoAsignadoPuedeSubirFotosYCambiarEstados() {
        OrdenResponse o = crearOrden(1, 100, 100);
        String otro = tokenTecnico(11, 101);
        subirFoto(o.id(), otro, "RECIBIDO", jpg(1024)).expectStatus().isForbidden();
        cambiarEstado(o.id(), otro, "RECIBIDO").expectStatus().isForbidden();
        cambiarEstado(o.id(), token(1, "CLIENTE"), "RECIBIDO").expectStatus().isForbidden();
    }

    @Test
    void lasFotosSeDescarganSoloLosParticipantes() {
        OrdenResponse o = crearOrden(1, 100, 100);
        String tec = tokenTecnico(10, 100);
        byte[] original = jpg(3000);
        FotoResponse f = subirFoto(o.id(), tec, "RECIBIDO", original)
                .expectStatus().isCreated()
                .expectBody(FotoResponse.class).returnResult().getResponseBody();

        byte[] bajada = cliente.get().uri("/api/ordenes/{id}/fotos/{f}", o.id(), f.id())
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectBody(byte[].class).returnResult().getResponseBody();
        assertArrayEquals(original, bajada);

        cliente.get().uri("/api/ordenes/{id}/fotos/{f}", o.id(), f.id())
                .header(HttpHeaders.AUTHORIZATION, token(2, "CLIENTE"))
                .exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/ordenes/{id}/fotos/{f}", o.id(), 99999)
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange().expectStatus().isNotFound();
    }

    // ---------- Cancelación ----------

    @Test
    void elClienteCancelaSoloSiSigueContratada() {
        OrdenResponse a = crearOrden(1, 100, 100);
        cancelar(a.id(), token(1, "CLIENTE")).expectStatus().isOk()
                .expectBody().jsonPath("$.estado").isEqualTo("CANCELADO");
        cambiarEstado(a.id(), tokenTecnico(10, 100), "RECIBIDO")
                .expectStatus().isBadRequest();

        OrdenResponse b = crearOrden(1, 100, 100);
        avanzar(b.id(), tokenTecnico(10, 100), "RECIBIDO");
        cancelar(b.id(), token(1, "CLIENTE")).expectStatus().isBadRequest();
        cancelar(b.id(), token(2, "CLIENTE")).expectStatus().isForbidden();
    }

    @Test
    void elTecnicoCancelaHastaEnReparacion() {
        String tec = tokenTecnico(10, 100);
        OrdenResponse a = crearOrden(1, 100, 100);
        avanzar(a.id(), tec, "RECIBIDO");
        cancelar(a.id(), tec).expectStatus().isOk();

        OrdenResponse b = crearOrden(1, 100, 100);
        llevarAListo(b.id(), tec);
        cancelar(b.id(), tec).expectStatus().isBadRequest();
        cancelar(b.id(), tokenTecnico(11, 101)).expectStatus().isForbidden();
    }

    // ---------- Pago ----------

    @Test
    void elPagoSoloSePermiteCuandoLaOrdenEstaListo() {
        OrdenResponse o = crearOrden(1, 100, 100);
        pagar(o.id(), token(1, "CLIENTE"), 100, "OP-A").expectStatus().isBadRequest();
    }

    @Test
    void reglasDelPago() {
        OrdenResponse o = crearOrden(1, 100, 100);
        llevarAListo(o.id(), tokenTecnico(10, 100));
        String cli = token(1, "CLIENTE");

        pagar(o.id(), cli, 150, "OP-B").expectStatus().isBadRequest();
        pagar(o.id(), token(2, "CLIENTE"), 100, "OP-B").expectStatus().isForbidden();
        pagar(o.id(), token(10, "TECNICO"), 100, "OP-B").expectStatus().isForbidden();
        pagar(o.id(), cli, 0, "OP-B").expectStatus().isBadRequest();

        pagar(o.id(), cli, 40, "OP-B").expectStatus().isCreated();
        pagar(o.id(), cli, 60, "OP-C").expectStatus().isEqualTo(409);
    }

    @Test
    void numeroDeOperacionUnicoYReintentoTrasRechazo() {
        OrdenResponse a = crearOrden(1, 100, 100);
        OrdenResponse b = crearOrden(1, 100, 100);
        String tec = tokenTecnico(10, 100);
        String cli = token(1, "CLIENTE");
        llevarAListo(a.id(), tec);
        llevarAListo(b.id(), tec);

        pagar(a.id(), cli, 100, "RECHAZO-1").expectStatus().isCreated()
                .expectBody().jsonPath("$.estado").isEqualTo("RECHAZADO");
        cambiarEstado(a.id(), tec, "ENTREGADO").expectStatus().isBadRequest();
        pagar(a.id(), cli, 100, "OP-D").expectStatus().isCreated()
                .expectBody().jsonPath("$.estado").isEqualTo("PAGADO");
        cambiarEstado(a.id(), tec, "ENTREGADO").expectStatus().isOk();

        pagar(b.id(), cli, 100, "OP-D").expectStatus().isEqualTo(409);
    }

    @Test
    void noSeEntregaSinPago() {
        OrdenResponse o = crearOrden(1, 100, 100);
        String tec = tokenTecnico(10, 100);
        llevarAListo(o.id(), tec);
        cambiarEstado(o.id(), tec, "ENTREGADO").expectStatus().isBadRequest();
    }

    // ---------- Calificación ----------

    @Test
    void reglasDeLaCalificacion() {
        OrdenResponse o = crearOrden(1, 100, 100);
        String tec = tokenTecnico(10, 100);
        String cli = token(1, "CLIENTE");

        calificar(o.id(), cli, 5).expectStatus().isBadRequest();

        llevarAListo(o.id(), tec);
        pagar(o.id(), cli, 100, "OP-E").expectStatus().isCreated();
        cambiarEstado(o.id(), tec, "ENTREGADO").expectStatus().isOk();

        calificar(o.id(), cli, 6).expectStatus().isBadRequest();
        calificar(o.id(), cli, 0).expectStatus().isBadRequest();
        calificar(o.id(), token(2, "CLIENTE"), 4).expectStatus().isForbidden();
        calificar(o.id(), tec, 4).expectStatus().isForbidden();
        calificar(o.id(), cli, 4).expectStatus().isCreated();
        calificar(o.id(), cli, 5).expectStatus().isEqualTo(409);
    }

    // ---------- Consultas ----------

    @Test
    void cadaRolVeSoloSusOrdenes() {
        crearOrden(1, 100, 100);
        crearOrden(1, 101, 100);
        crearOrden(2, 100, 100);

        assertEquals(2, listar(token(1, "CLIENTE")).size());
        assertEquals(1, listar(token(2, "CLIENTE")).size());
        assertEquals(2, listar(tokenTecnico(10, 100)).size());
        assertEquals(1, listar(tokenTecnico(11, 101)).size());
        assertEquals(3, listar(token(99, "ADMIN")).size());
    }

    private List<OrdenResponse> listar(String token) {
        return cliente.get().uri("/api/ordenes")
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OrdenResponse.class)
                .returnResult().getResponseBody();
    }

    @Test
    void elDetalleSoloLoVenLosParticipantes() {
        OrdenResponse o = crearOrden(1, 100, 100);

        cliente.get().uri("/api/ordenes/{id}", o.id())
                .header(HttpHeaders.AUTHORIZATION, token(2, "CLIENTE"))
                .exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/ordenes/{id}", o.id())
                .header(HttpHeaders.AUTHORIZATION, tokenTecnico(11, 101))
                .exchange().expectStatus().isForbidden();
        cliente.get().uri("/api/ordenes/{id}", 99999)
                .header(HttpHeaders.AUTHORIZATION, token(1, "CLIENTE"))
                .exchange().expectStatus().isNotFound();
        cliente.get().uri("/api/ordenes/{id}", o.id())
                .exchange().expectStatus().isUnauthorized();

        detalle(o.id(), tokenTecnico(10, 100));
        detalle(o.id(), token(99, "ADMIN"));
    }
}
