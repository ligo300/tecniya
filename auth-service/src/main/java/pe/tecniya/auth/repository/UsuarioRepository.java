package pe.tecniya.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.tecniya.auth.domain.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
