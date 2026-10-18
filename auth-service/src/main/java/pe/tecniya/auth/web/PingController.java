package pe.tecniya.auth.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PingController {

    @Value("${spring.application.name}")
    private String nombre;

    @Value("${server.port}")
    private String puerto;

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("servicio", nombre, "puerto", puerto, "estado", "OK");
    }
}
