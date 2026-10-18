package pe.tecniya.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UsuarioRepository repositorio;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(repositorio, encoder, jwtService);
    }

    @Test
    void registrarGuardaLaPasswordCifradaConBCrypt() {
        when(repositorio.existsByEmail("ana@correo.com")).thenReturn(false);
        when(repositorio.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse respuesta = servicio.registrar(
                new RegistroRequest("Ana@Correo.com", "Clave12345", Rol.CLIENTE));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio).save(captor.capture());
        String hash = captor.getValue().getPasswordHash();
        assertThat(hash).isNotEqualTo("Clave12345").startsWith("$2");
        assertThat(encoder.matches("Clave12345", hash)).isTrue();
        assertThat(respuesta.email()).isEqualTo("ana@correo.com");
    }

    @Test
    void registrarRechazaElRolAdmin() {
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(
                new RegistroRequest("x@correo.com", "Clave12345", Rol.ADMIN)));
    }

    @Test
    void registrarRechazaCorreoRepetido() {
        when(repositorio.existsByEmail("ana@correo.com")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> servicio.registrar(
                new RegistroRequest("ana@correo.com", "Clave12345", Rol.CLIENTE)));
    }

    @Test
    void loginConPasswordIncorrectaFalla() {
        Usuario usuario = usuarioActivo();
        when(repositorio.findByEmail("ana@correo.com"))
                .thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class, () -> servicio.login(
                new LoginRequest("ana@correo.com", "otra-clave")));
    }

    @Test
    void loginCorrectoDevuelveToken() {
        Usuario usuario = usuarioActivo();
        when(repositorio.findByEmail("ana@correo.com"))
                .thenReturn(Optional.of(usuario));
        when(jwtService.generar(usuario)).thenReturn("token-de-prueba");
        when(jwtService.getMinutosDeVida()).thenReturn(120L);

        AuthResponse respuesta = servicio.login(
                new LoginRequest("ana@correo.com", "Clave12345"));

        assertThat(respuesta.token()).isEqualTo("token-de-prueba");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .id(1L)
                .email("ana@correo.com")
                .passwordHash(encoder.encode("Clave12345"))
                .rol(Rol.CLIENTE)
                .activo(true)
                .build();
    }
}
