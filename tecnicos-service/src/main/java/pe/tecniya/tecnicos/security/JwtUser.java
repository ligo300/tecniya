package pe.tecniya.tecnicos.security;

/** Datos del usuario que vienen dentro del token JWT emitido por auth-service. */
public record JwtUser(Long id, String email, String rol) {
}
