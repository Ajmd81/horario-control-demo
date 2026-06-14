package com.controlhorario.lite;

import com.controlhorario.lite.entity.Usuario;
import com.controlhorario.lite.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteApplication.class, args);
    }

    @Bean
    CommandLineRunner init(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.findByUsername("superadmin").isEmpty()) {
                Usuario sa = Usuario.builder()
                        .username("superadmin")
                        .password(encoder.encode("superadmin123"))
                        .role(Usuario.Role.SUPERADMIN)
                        .activo(true)
                        .build();
                repo.save(sa);
                System.out.println("✅ Superadmin creado — cambia la contraseña en producción!");
            }
        };
    }
}
