package pe.tecniya.solicitudes.client;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import pe.tecniya.solicitudes.domain.Oferta;
import pe.tecniya.solicitudes.domain.Solicitud;
import pe.tecniya.solicitudes.exception.ServicioNoDisponibleException;
import reactor.core.publisher.Mono;

/** Crea la orden real en ordenes-service (llamada interna con clave compartida). */
@Component
public class OrdenesWebClient implements OrdenesClient {

    private final WebClient web;
    private final String claveInterna;

    public OrdenesWebClient(WebClient.Builder builder,
                            @Value("${app.ordenes.url}") String url,
                            @Value("${app.internal.key}") String claveInterna) {
        this.web = builder.baseUrl(url).build();
        this.claveInterna = claveInterna;
    }

    @Override
    public Mono<Void> crearOrdenDesdeOferta(Solicitud solicitud, Oferta oferta) {
        String descripcion = solicitud.getMarcaModelo() + " — " + solicitud.getDescripcionFalla();
        if (descripcion.length() > 700) {
            descripcion = descripcion.substring(0, 700);
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("clienteId", solicitud.getClienteId());
        cuerpo.put("tecnicoId", oferta.getTecnicoId());
        cuerpo.put("solicitudId", solicitud.getId());
        cuerpo.put("ofertaId", oferta.getId());
        cuerpo.put("tipoEquipo", solicitud.getTipoEquipo().name());
        cuerpo.put("descripcion", descripcion);
        cuerpo.put("precioAcordado", oferta.getPrecio());

        return web.post()
                .uri("/internal/ordenes/desde-oferta")
                .header("X-Internal-Key", claveInterna)
                .bodyValue(cuerpo)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorMap(WebClientResponseException.class,
                        e -> new ServicioNoDisponibleException(
                                "No se pudo crear la orden en ordenes-service"))
                .onErrorMap(WebClientRequestException.class,
                        e -> new ServicioNoDisponibleException(
                                "No se pudo conectar con ordenes-service"));
    }
}
