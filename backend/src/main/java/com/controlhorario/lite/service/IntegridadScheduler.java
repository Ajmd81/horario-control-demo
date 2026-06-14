package com.controlhorario.lite.service;

import com.controlhorario.lite.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntegridadScheduler {

    private final EmpresaRepository  empresaRepo;
    private final IntegridadService  integridadService;

    /** Verificación automática a las 03:00 todos los días. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Madrid")
    public void verificarTodasLasEmpresas() {
        log.info("⏰ Iniciando verificación automática de integridad");
        empresaRepo.findAll().forEach(empresa -> {
            try {
                var resultado = integridadService.verificarEmpresa(empresa.getId(), true);
                if (resultado.totalCorruptos() > 0) {
                    log.warn("⚠️ Empresa {}: {} fichajes corruptos",
                            empresa.getSlug(), resultado.totalCorruptos());
                }
            } catch (Exception e) {
                log.error("Error verificando empresa {}", empresa.getSlug(), e);
            }
        });
        log.info("✓ Verificación automática completada");
    }
}