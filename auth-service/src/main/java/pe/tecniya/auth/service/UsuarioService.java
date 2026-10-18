package pe.tecniya.auth.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pe.tecniya.auth.domain.Usuario;
import pe.tecniya.auth.dto.ActualizarUsuarioRequest;
import pe.tecniya.auth.dto.UsuarioResponse;
import pe.tecniya.auth.exception.CorreoDuplicadoException;
import pe.tecniya.auth.exception.RecursoNoEncontradoException;
import pe.tecniya.auth.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = buscar(id);
        if (request.email() != null && !request.email().isBlank()) {
            String nuevo = request.email().trim().toLowerCase();
            if (!nuevo.equals(usuario.getEmail())
                    && usuarioRepository.existsByEmail(nuevo)) {
                throw new CorreoDuplicadoException(nuevo);
            }
            usuario.setEmail(nuevo);
        }
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException("El usuario " + id + " no existe"));
    }
}
