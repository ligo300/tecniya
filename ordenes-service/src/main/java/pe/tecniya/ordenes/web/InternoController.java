package pe.tecniya.ordenes.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.tecniya.ordenes.dto.OrdenInternaRequest;
import pe.tecniya.ordenes.dto.OrdenResponse;
import pe.tecniya.ordenes.exception.AccesoDenegadoException;
import pe.tecniya.ordenes.service.OrdenService;
import reactor.core.publisher.Mono;

/**
 * Endpoint solo para otros servicios (no está en las rutas del Gateway).
 * Se protege con una clave compartida en la cabecera X-Internal-Key.
 */
@RestController
@RequestMapping("/internal/ordenes")
public class InternoController {

    private final OrdenService ordenes;
    private final byte[] clave;

    public InternoController(OrdenService ordenes, @Value("${app.internal.key}") String clave) {
        this.ordenes = ordenes;
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/desde-oferta")
    public Mono<ResponseEntity<OrdenResponse>> desdeOferta(
            @RequestHeader(value = "X-Internal-Key", required = false) String recibida,
            @Valid @RequestBody OrdenInternaRequest req) {
        if (recibida == null || !MessageDigest.isEqual(clave,
                recibida.getBytes(StandardCharsets.UTF_8))) {
            return Mono.error(new AccesoDenegadoException("Clave interna incorrecta"));
        }
        return ordenes.crearDesdeOferta(req)
                .map(o -> ResponseEntity.status(HttpStatus.CREATED).body(o));
    }
}
