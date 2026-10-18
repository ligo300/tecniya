package pe.tecniya.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @Email @Size(max = 120) String email,
        @Size(min = 8, max = 72) String password) {
}
