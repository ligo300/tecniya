package pe.tecniya.solicitudes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SolicitudesServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca con R2DBC (H2 en memoria).
    }
}
