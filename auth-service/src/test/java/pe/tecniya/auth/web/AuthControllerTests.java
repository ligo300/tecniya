package pe.tecniya.auth.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private String registro(String email, String password, String rol) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password
                + "\",\"rol\":\"" + rol + "\"}";
    }

    private String login(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String obtenerToken(String email, String password) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.token");
    }

    @Test
    void registrarClienteDevuelve201SinExponerElHash() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("cliente1@correo.com", "Clave12345",
                                "CLIENTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("cliente1@correo.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registrarCorreoRepetidoDevuelve409() throws Exception {
        String cuerpo = registro("cliente2@correo.com", "Clave12345", "CLIENTE");
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarConDatosInvalidosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("no-es-correo", "123", "CLIENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void registrarComoAdminDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("falso.admin@correo.com", "Clave12345",
                                "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginCorrectoDevuelveToken() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("cliente3@correo.com", "Clave12345",
                                "CLIENTE")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("cliente3@correo.com", "Clave12345")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("admin@tecniya.pe", "incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarUsuariosSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elAdminPuedeListarUsuarios() throws Exception {
        String token = obtenerToken("admin@tecniya.pe", "Admin12345");

        mockMvc.perform(get("/api/auth/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void unClienteNoPuedeListarUsuarios() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("cliente4@correo.com", "Clave12345",
                                "CLIENTE")))
                .andExpect(status().isCreated());
        String token = obtenerToken("cliente4@correo.com", "Clave12345");

        mockMvc.perform(get("/api/auth/usuarios")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
