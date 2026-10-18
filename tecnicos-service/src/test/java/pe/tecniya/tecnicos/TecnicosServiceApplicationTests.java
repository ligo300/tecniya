package pe.tecniya.tecnicos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "eureka.client.enabled=false")
class TecnicosServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca sin errores.
    }
}
