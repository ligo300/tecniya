package pe.tecniya.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "eureka.client.enabled=false")
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca sin errores.
    }
}
