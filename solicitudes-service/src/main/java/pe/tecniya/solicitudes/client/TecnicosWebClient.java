package pe.tecniya.solicitudes.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import pe.tecniya.solicitudes.exception.ReglaNegocioException;
import pe.tecniya.solicitudes.exception.ServicioNoDisponibleException;
import reactor.core.publisher.Mono;

@Component
public class TecnicosWebClient implements TecnicosClient {

    private final WebClient web;

    public TecnicosWebClient(WebClient.Builder builder,
                             @Value("${app.tecnicos.url}") String url) {
        this.web = builder.baseUrl(url).build();
    }

    @Override
    public Mono<TecnicoInfo> miPerfil(String autorizacion) {
        return web.get()
                .uri("/api/tecnicos/mio")
                .header(HttpHeaders.AUTHORIZATION, autorizacion)
                .retrieve()
                .bodyToMono(TecnicoInfo.class)
                .onErrorMap(WebClientResponseException.NotFound.class,
                        e -> new ReglaNegocioException(
                                "Aún no has creado tu perfil de técnico"))
                .onErrorMap(WebClientResponseException.class,
                        e -> new ServicioNoDisponibleException(
                                "No se pudo consultar tecnicos-service"))
                .onErrorMap(WebClientRequestException.class,
                        e -> new ServicioNoDisponibleException(
                                "No se pudo conectar con tecnicos-service"));
    }
}
