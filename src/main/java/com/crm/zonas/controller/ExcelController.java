package com.crm.zonas.controller;

import com.crm.zonas.dto.CargaResultadoDTO;
import com.crm.zonas.exception.ArchivoInvalidoException;
import com.crm.zonas.service.ExcelParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelParserService parser;

    /**
     * POST /api/excel/cargar
     * Content-Type: multipart/form-data
     * Param:        file → archivo .xlsx
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

        return ResponseEntity.ok(parser.procesarExcel(file));
    }
}
