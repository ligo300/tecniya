package pe.tecniya.auth.dto;

import pe.tecniya.auth.domain.Rol;
import pe.tecniya.auth.domain.Usuario;

public record UsuarioResponse(Long id, String email, Rol rol, boolean activo) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getRol(), u.isActivo());
    }
}
