package pe.tecniya.solicitudes.client;

import reactor.core.publisher.Mono;

/** Llamada síncrona a tecnicos-service. */
public interface TecnicosClient {

    /** Perfil del técnico dueño del token (GET /api/tecnicos/mio). */
    Mono<TecnicoInfo> miPerfil(String autorizacion);
}
