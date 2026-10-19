package pe.tecniya.ordenes.service;

import org.springframework.stereotype.Component;

import pe.tecniya.ordenes.client.TecnicosClient;
import pe.tecniya.ordenes.domain.Orden;
import pe.tecniya.ordenes.exception.AccesoDenegadoException;
import pe.tecniya.ordenes.exception.RecursoNoEncontradoException;
import pe.tecniya.ordenes.repository.OrdenRepository;
import pe.tecniya.ordenes.security.JwtUser;
import reactor.core.publisher.Mono;

/** Reglas de "quién puede tocar esta orden", compartidas por todos los servicios. */
@Component
public class AccesoOrden {

    private final OrdenRepository ordenes;
    private final TecnicosClient tecnicosClient;

    public AccesoOrden(OrdenRepository ordenes, TecnicosClient tecnicosClient) {
        this.ordenes = ordenes;
        this.tecnicosClient = tecnicosClient;
    }

    public Mono<Orden> cargar(Long id) {
        return ordenes.findById(id).switchIfEmpty(Mono.error(
                new RecursoNoEncontradoException("Orden no encontrada")));
    }

    /** Cliente de la orden, técnico asignado o administrador. */
    public Mono<Orden> comoParticipante(Long id, JwtUser usuario, String autorizacion) {
        return cargar(id).flatMap(o -> {
            switch (usuario.rol()) {
                case "ADMIN":
                    return Mono.just(o);
                case "CLIENTE":
                    return o.getClienteId().equals(usuario.id())
                            ? Mono.just(o)
                            : Mono.<Orden>error(new AccesoDenegadoException(
                                    "Esta orden no es tuya"));
                default:
                    return esTecnicoAsignado(o, autorizacion);
            }
        });
    }

    public Mono<Orden> comoCliente(Long id, JwtUser usuario) {
        return cargar(id).flatMap(o -> o.getClienteId().equals(usuario.id())
                ? Mono.just(o)
                : Mono.<Orden>error(new AccesoDenegadoException(
                        "Solo el cliente de la orden puede hacer esto")));
    }

    public Mono<Orden> comoTecnicoAsignado(Long id, String autorizacion) {
        return cargar(id).flatMap(o -> esTecnicoAsignado(o, autorizacion));
    }

    public Mono<Orden> esTecnicoAsignado(Orden orden, String autorizacion) {
        return tecnicosClient.miPerfil(autorizacion).flatMap(perfil ->
                perfil.id().equals(orden.getTecnicoId())
                        ? Mono.just(orden)
                        : Mono.<Orden>error(new AccesoDenegadoException(
                                "Esta orden está asignada a otro técnico")));
    }
}
