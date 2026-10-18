package pe.tecniya.solicitudes.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import pe.tecniya.solicitudes.exception.AccesoDenegadoException;
import reactor.core.publisher.Mono;

public final class UsuarioActual {

    private UsuarioActual() {
    }

    /** Devuelve el usuario del token, o un error 403 si la petición es anónima. */
    public static Mono<JwtUser> obtener() {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(contexto -> Mono.justOrEmpty(contexto.getAuthentication()))
                .filter(auth -> auth.getPrincipal() instanceof JwtUser)
                .map(auth -> (JwtUser) auth.getPrincipal())
                .switchIfEmpty(Mono.error(new AccesoDenegadoException("No autenticado")));
    }
}
