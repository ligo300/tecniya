package pe.tecniya.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import pe.tecniya.auth.domain.Rol;
import pe.tecniya.auth.domain.Usuario;
import pe.tecniya.auth.repository.UsuarioRepository;

@Component
public class AdminInicial implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInicial(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.admin.email}") String email,
                        @Value("${app.admin.password}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByEmail(email)) {
            usuarioRepository.save(Usuario.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .build());
        }
    }
}
