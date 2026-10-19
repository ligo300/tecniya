package pe.tecniya.ordenes.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.tecniya.ordenes.dto.CalificacionRequest;
import pe.tecniya.ordenes.dto.CalificacionResponse;
import pe.tecniya.ordenes.dto.CambioEstadoRequest;
import pe.tecniya.ordenes.dto.FotoResponse;
import pe.tecniya.ordenes.dto.OrdenDetalleResponse;
import pe.tecniya.ordenes.dto.OrdenDirectaRequest;
import pe.tecniya.ordenes.dto.OrdenResponse;
import pe.tecniya.ordenes.dto.PagoRequest;
import pe.tecniya.ordenes.dto.PagoResponse;
import pe.tecniya.ordenes.security.UsuarioActual;
import pe.tecniya.ordenes.service.CalificacionService;
import pe.tecniya.ordenes.service.FotoService;
import pe.tecniya.ordenes.service.OrdenService;
import pe.tecniya.ordenes.service.PagoService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService ordenes;
    private final FotoService fotos;
    private final PagoService pagos;
    private final CalificacionService calificaciones;

    public OrdenController(OrdenService ordenes, FotoService fotos, PagoService pagos,
                           CalificacionService calificaciones) {
        this.ordenes = ordenes;
        this.fotos = fotos;
        this.pagos = pagos;
        this.calificaciones = calificaciones;
    }

    @PostMapping("/directa")
    public Mono<ResponseEntity<OrdenResponse>> crearDirecta(
            @Valid @RequestBody OrdenDirectaRequest req) {
        return UsuarioActual.obtener()
                .flatMap(u -> ordenes.crearDirecta(req, u))
                .map(o -> ResponseEntity.status(HttpStatus.CREATED).body(o));
    }

    @GetMapping
    public Flux<OrdenResponse> listar(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMapMany(u -> ordenes.listar(u, autorizacion));
    }

    @GetMapping("/{id}")
    public Mono<OrdenDetalleResponse> detalle(@PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMap(u -> ordenes.detalle(id, u, autorizacion));
    }

    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<FotoResponse>> subirFoto(@PathVariable Long id,
            @RequestPart("archivo") FilePart archivo,
            @RequestPart("estado") String estado,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMap(u -> fotos.subir(id, archivo, estado, u, autorizacion))
                .map(f -> ResponseEntity.status(HttpStatus.CREATED).body(f));
    }

    @GetMapping("/{id}/fotos/{fotoId}")
    public Mono<ResponseEntity<byte[]>> verFoto(@PathVariable Long id,
            @PathVariable Long fotoId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMap(u -> fotos.descargar(id, fotoId, u, autorizacion))
                .map(a -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(a.tipo()))
                        .cacheControl(CacheControl.noStore())
                        .body(a.bytes()));
    }

    @PutMapping("/{id}/estado")
    public Mono<OrdenResponse> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoRequest req,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMap(u -> ordenes.cambiarEstado(id, req.estado(), u, autorizacion));
    }

    @PutMapping("/{id}/cancelar")
    public Mono<OrdenResponse> cancelar(@PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return UsuarioActual.obtener()
                .flatMap(u -> ordenes.cancelar(id, u, autorizacion));
    }

    @PostMapping("/{id}/pago")
    public Mono<ResponseEntity<PagoResponse>> pagar(@PathVariable Long id,
            @Valid @RequestBody PagoRequest req) {
        return UsuarioActual.obtener()
                .flatMap(u -> pagos.pagar(id, req, u))
                .map(p -> ResponseEntity.status(HttpStatus.CREATED).body(p));
    }

    @PostMapping("/{id}/calificacion")
    public Mono<ResponseEntity<CalificacionResponse>> calificar(@PathVariable Long id,
            @Valid @RequestBody CalificacionRequest req) {
        return UsuarioActual.obtener()
                .flatMap(u -> calificaciones.calificar(id, req, u))
                .map(c -> ResponseEntity.status(HttpStatus.CREATED).body(c));
    }
}
