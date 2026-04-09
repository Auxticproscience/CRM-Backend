package com.crm.zonas.service;

import com.auxticproscience.crm.service.OneDriveService;
import com.auxticproscience.crm.service.OneDriveService.ArchivoOneDrive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final OneDriveService oneDriveService;
    private final ExcelParserService gestionesParser;
    public SchedulerService(OneDriveService oneDriveService,
                            ExcelParserService gestionesParser) {
        this.oneDriveService = oneDriveService;
        this.gestionesParser = gestionesParser;
    }

    /**
     * Se ejecuta cada día a las 6:00 AM hora Colombia (UTC-5).
     * Cron: segundo minuto hora día mes día-semana
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "America/Bogota")
    public void procesarArchivosDelDia() {
        log.info("=== Scheduler iniciado: buscando archivos en OneDrive ===");
        try {
            String token = oneDriveService.obtenerToken();
            List<ArchivoOneDrive> archivos = oneDriveService.listarArchivosXlsx(token);

            if (archivos.isEmpty()) {
                log.info("No se encontraron archivos .xlsx en la carpeta de OneDrive.");
                return;
            }

            for (ArchivoOneDrive archivo : archivos) {
                procesarArchivo(token, archivo);
            }

            log.info("=== Scheduler finalizado: {} archivo(s) procesado(s) ===", archivos.size());

        } catch (Exception e) {
            log.error("Error en el scheduler: {}", e.getMessage(), e);
        }
    }

    /** Permite lanzar el proceso manualmente desde el endpoint REST. */
    public void ejecutarManualmente() {
        procesarArchivosDelDia();
    }

    // ─── Detección de prefijo y despacho al parser correcto ──────────────────

    private void procesarArchivo(String token, ArchivoOneDrive archivo) {
        String nombre = archivo.nombre().toLowerCase();
        log.info("Procesando: {}", archivo.nombre());

        try {
            byte[] contenido = oneDriveService.descargarArchivo(token, archivo.id());
            ByteArrayInputStream stream = new ByteArrayInputStream(contenido);

            if (nombre.startsWith("ges_")) {
                gestionesParser.parsearYGuardar(stream, archivo.nombre());
                log.info("  → GestionesParser OK");

            } else if (nombre.startsWith("ped_")) {
                // pedidosParser.parsearYGuardar(stream, archivo.nombre());
                log.warn("  → PedidosParser pendiente, archivo ignorado: {}", archivo.nombre());

            } else if (nombre.startsWith("cot_")) {
                // cotizacionesParser.parsearYGuardar(stream, archivo.nombre());
                log.warn("  → CotizacionesParser pendiente, archivo ignorado: {}", archivo.nombre());

            } else {
                log.warn("  → Prefijo desconocido en '{}', ignorado.", archivo.nombre());
            }

        } catch (Exception e) {
            log.error("  → Error procesando '{}': {}", archivo.nombre(), e.getMessage(), e);
        }
    }
}