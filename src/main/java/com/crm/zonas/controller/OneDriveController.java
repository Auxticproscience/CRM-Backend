package com.crm.zonas.controller;

import com.crm.zonas.service.SchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoint para disparar el scheduler manualmente.
 * Útil para pruebas sin esperar las 6am.
 *
 * POST /api/onedrive/ejecutar
 */
@RestController
@RequestMapping("/api/onedrive")
public class OneDriveController {

    private final SchedulerService schedulerService;

    public OneDriveController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @PostMapping("/ejecutar")
    public ResponseEntity<Map<String, Object>> ejecutarManualmente() {
        schedulerService.ejecutarManualmente();
        return ResponseEntity.ok(Map.of(
                "mensaje", "Proceso de OneDrive ejecutado manualmente",
                "hora",    LocalDateTime.now().toString()
        ));
    }
}