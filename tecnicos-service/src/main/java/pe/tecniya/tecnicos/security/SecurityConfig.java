package pe.tecniya.tecnicos.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain cadenaDeSeguridad(ServerHttpSecurity http, JwtService jwtService) {
        ReactiveAuthenticationManager gestor = autenticacion -> Mono.just(autenticacion);
        AuthenticationWebFilter filtroJwt = new AuthenticationWebFilter(gestor);
        filtroJwt.setServerAuthenticationConverter(
                exchange -> convertir(exchange, jwtService));
        filtroJwt.setSecurityContextRepository(
                NoOpServerSecurityContextRepository.getInstance());

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(
                        NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(a -> a
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/tecnicos/mio")
                                .hasRole("TECNICO")
                        .pathMatchers(HttpMethod.GET, "/api/tecnicos/gestion")
                                .hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/tecnicos",
                                "/api/tecnicos/{id}").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/tecnicos")
                                .hasRole("TECNICO")
                        .pathMatchers(HttpMethod.PUT, "/api/tecnicos/{id}/estado")
                                .hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/tecnicos/{id}")
                                .hasAnyRole("TECNICO", "ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/tecnicos/{id}")
                                .hasRole("ADMIN")
                        .anyExchange().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterAt(filtroJwt, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    private Mono<Authentication> convertir(ServerWebExchange exchange, JwtService jwtService) {
        String cabecera = exchange.getRequest().getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            return Mono.empty();
        }
        try {
            JwtUser usuario = jwtService.leer(cabecera.substring(7));
            Authentication autenticacion = new UsernamePasswordAuthenticationToken(
                    usuario, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol())));
            return Mono.just(autenticacion);
        } catch (JwtException | IllegalArgumentException e) {
            return Mono.empty();
        }
    }
}
