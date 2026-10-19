package pe.tecniya.ordenes.client;

import reactor.core.publisher.Mono;

/** Llamadas síncronas a tecnicos-service. */
public interface TecnicosClient {

    /** Perfil del técnico dueño del token (GET /api/tecnicos/mio). */
    Mono<TecnicoInfo> miPerfil(String autorizacion);

    /** Vista pública de un técnico (GET /api/tecnicos/{id}), sin token. */
    Mono<TecnicoInfo> perfilPublico(Long tecnicoId);
}
