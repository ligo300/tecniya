package pe.tecniya.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import pe.tecniya.auth.domain.Rol;
import pe.tecniya.auth.domain.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryTests {

    @Autowired
    private UsuarioRepository repositorio;

    private Usuario nuevo(String email) {
        return Usuario.builder()
                .email(email)
                .passwordHash("$2a$10$hashDePrueba")
                .rol(Rol.CLIENTE)
                .activo(true)
                .build();
    }

    @Test
    void insertar() {
        Usuario guardado = repositorio.save(nuevo("ana@correo.com"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getCreadoEn()).isNotNull();
        assertThat(repositorio.findById(guardado.getId())).isPresent();
    }

    @Test
    void actualizar() {
        Usuario guardado = repositorio.save(nuevo("ana@correo.com"));

        guardado.setEmail("ana.nueva@correo.com");
        guardado.setActivo(false);
        repositorio.saveAndFlush(guardado);

        Usuario leido = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getEmail()).isEqualTo("ana.nueva@correo.com");
        assertThat(leido.isActivo()).isFalse();
    }

    @Test
    void eliminar() {
        Usuario guardado = repositorio.save(nuevo("ana@correo.com"));

        repositorio.deleteById(guardado.getId());

        assertThat(repositorio.findById(guardado.getId())).isEmpty();
    }

    @Test
    void listar() {
        repositorio.save(nuevo("ana@correo.com"));
        repositorio.save(nuevo("luis@correo.com"));

        assertThat(repositorio.findAll()).hasSize(2);
    }

    @Test
    void buscarPorCorreo() {
        repositorio.save(nuevo("ana@correo.com"));

        assertThat(repositorio.findByEmail("ana@correo.com")).isPresent();
        assertThat(repositorio.existsByEmail("ana@correo.com")).isTrue();
        assertThat(repositorio.existsByEmail("otro@correo.com")).isFalse();
    }

    @Test
    void noPermiteCorreosRepetidos() {
        repositorio.saveAndFlush(nuevo("ana@correo.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repositorio.saveAndFlush(nuevo("ana@correo.com")));
    }
}
