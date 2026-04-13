package com.crm.zonas.controller;

import com.crm.zonas.dto.CargaResultadoDTO;
import com.crm.zonas.exception.ArchivoInvalidoException;
import com.crm.zonas.service.CotizacionesParserService;
import com.crm.zonas.service.GestionesExcelParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final GestionesExcelParserService gestionesParser;
    private final CotizacionesParserService   cotizacionesParser;

    /**
     * POST /api/excel/cargar
     * Detecta el tipo de archivo por el prefijo del nombre:
     *   ges_*.xlsx  → GestionesExcelParserService
     *   cot_*.xlsx  → CotizacionesParserService
     */
    @PostMapping("/cargar")
    public ResponseEntity<CargaResultadoDTO> cargar(
            @RequestParam("file") MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            throw new ArchivoInvalidoException("El archivo está vacío.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new ArchivoInvalidoException("Solo se aceptan archivos .xlsx");
        }

        String nombre = filename.toLowerCase();

        if (nombre.startsWith("ges_")) {
            return ResponseEntity.ok(gestionesParser.procesarExcel(file));
        }

        if (nombre.startsWith("cot_")) {
            return ResponseEntity.ok(
                    cotizacionesParser.procesarExcel(file.getInputStream(), filename));
        }

        throw new ArchivoInvalidoException(
                "Prefijo de archivo no reconocido. Use ges_*.xlsx o cot_*.xlsx");
    }
}