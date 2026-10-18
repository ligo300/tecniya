package pe.tecniya.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.tecniya.auth.domain.Rol;
import pe.tecniya.auth.domain.Usuario;
import pe.tecniya.auth.dto.AuthResponse;
import pe.tecniya.auth.dto.LoginRequest;
import pe.tecniya.auth.dto.RegistroRequest;
import pe.tecniya.auth.dto.UsuarioResponse;
import pe.tecniya.auth.exception.CorreoDuplicadoException;
import pe.tecniya.auth.exception.CredencialesInvalidasException;
import pe.tecniya.auth.exception.ReglaNegocioException;
import pe.tecniya.auth.repository.UsuarioRepository;
import pe.tecniya.auth.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (request.rol() == Rol.ADMIN) {
            throw new ReglaNegocioException(
                    "No se puede registrar un usuario con rol ADMIN");
        }
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new CorreoDuplicadoException(email);
        }
        Usuario usuario = Usuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(true)
                .build();
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository
                .findByEmail(request.email().trim().toLowerCase())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(request.password(),
                        u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);
        return new AuthResponse(jwtService.generar(usuario), "Bearer",
                jwtService.getMinutosDeVida(), UsuarioResponse.from(usuario));
    }
}
