package com.crm.zonas.service;

import com.crm.zonas.dto.CargaResultadoDTO;
import com.crm.zonas.entity.*;
import com.crm.zonas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelParserService {

    private final ActividadRepository    actividadRepo;
    private final EstadoRepository       estadoRepo;
    private final TipoRepository         tipoRepo;
    private final PropietarioRepository  propietarioRepo;
    private final ClienteRepository      clienteRepo;
    private final LugarRepository        lugarRepo;
    private final CargaExcelRepository   cargaRepo;

    // ── Índices de columna ────────────────────────────────────────
    private static final int COL_FECHA        = 0;
    private static final int COL_DESCRIPCION  = 1;
    private static final int COL_NOMBRE       = 2;
    private static final int COL_PROPIETARIO  = 3;
    private static final int COL_LUGAR        = 4;
    private static final int COL_CLIENTE      = 5;
    private static final int COL_ESTADO       = 7;
    private static final int COL_TIPO         = 8;

    // ─────────────────────────────────────────────────────────────
    // Entrada desde el scheduler (OneDrive → InputStream)
    // ─────────────────────────────────────────────────────────────

    /**
     * Llamado por el SchedulerService al descargar un archivo de OneDrive.
     * Delega en el núcleo de procesamiento común.
     */
    @Transactional
    public CargaResultadoDTO parsearYGuardar(ByteArrayInputStream stream, String nombreArchivo) throws IOException {
        return procesarStream(stream, nombreArchivo);
    }

    // ─────────────────────────────────────────────────────────────
    // Entrada desde el endpoint REST (carga manual desde el front)
    // ─────────────────────────────────────────────────────────────

    /**
     * Llamado por el controller REST con un MultipartFile subido manualmente.
     * Delega en el núcleo de procesamiento común.
     */
    @Transactional
    public CargaResultadoDTO procesarExcel(MultipartFile file) throws IOException {
        return procesarStream(file.getInputStream(), file.getOriginalFilename());
    }


    private CargaResultadoDTO procesarStream(InputStream inputStream, String nombreArchivo) throws IOException {

        int cargados = 0, omitidos = 0;
        List<String> errores = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(inputStream)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            // Fila 0 = encabezados → empezar en fila 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    OffsetDateTime fecha  = parseFecha(row.getCell(COL_FECHA), evaluator);
                    String nombre         = texto(row.getCell(COL_NOMBRE), fmt);
                    String propNombre     = texto(row.getCell(COL_PROPIETARIO), fmt);

                    if (fecha == null || nombre.isBlank() || propNombre.isBlank()) {
                        omitidos++;
                        errores.add("Fila " + (i + 1) + ": fecha/nombre/propietario vacío");
                        continue;
                    }

                    Propietario propietario = propietarioRepo
                            .findByNombreIgnoreCase(propNombre)
                            .orElseGet(() -> propietarioRepo.save(
                                    Propietario.builder()
                                            .nombre(propNombre.toUpperCase())
                                            .build()));

                    // Deduplicación
                    if (actividadRepo.existsByFechaCreacionAndNombreAndPropietarioId(
                            fecha, nombre, propietario.getId())) {
                        omitidos++;
                        continue;
                    }

                    Actividad a = Actividad.builder()
                            .fechaCreacion(fecha)
                            .nombre(nombre)
                            .descripcion(texto(row.getCell(COL_DESCRIPCION), fmt))
                            .estado(resolverEstado(texto(row.getCell(COL_ESTADO), fmt)))
                            .tipo(resolverTipo(texto(row.getCell(COL_TIPO), fmt)))
                            .propietario(propietario)
                            .cliente(resolverCliente(texto(row.getCell(COL_CLIENTE), fmt)))
                            .lugar(resolverLugar(texto(row.getCell(COL_LUGAR), fmt)))
                            .build();

                    actividadRepo.save(a);
                    cargados++;

                } catch (Exception e) {
                    log.warn("Fila {} omitida por error: {}", i + 1, e.getMessage());
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                    omitidos++;
                }
            }
        }

        // Auditoría
        CargaExcel registro = CargaExcel.builder()
                .nombreArchivo(nombreArchivo)
                .registrosCargados(cargados)
                .registrosOmitidos(omitidos)
                .notas(errores.isEmpty() ? null : String.join(" | ", errores))
                .build();
        cargaRepo.save(registro);

        log.info("Carga finalizada — archivo: {}, insertados: {}, omitidos: {}",
                nombreArchivo, cargados, omitidos);

        return new CargaResultadoDTO(
                nombreArchivo,
                cargados,
                omitidos,
                registro.getFechaCarga(),
                registro.getNotas()
        );
    }

    private String texto(Cell cell, DataFormatter fmt) {
        if (cell == null) return "";
        return fmt.formatCellValue(cell).trim().replaceAll("\\s+", " ");
    }

    private OffsetDateTime parseFecha(Cell cell, FormulaEvaluator ev) {
        if (cell == null) return null;

        CellType type = cell.getCellType() == CellType.FORMULA
                ? ev.evaluateFormulaCell(cell)
                : cell.getCellType();

        if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().atOffset(ZoneOffset.UTC);
        }
        if (type == CellType.STRING) {
            String s = cell.getStringCellValue().trim().replaceAll("\\s+", " ");
            for (String pattern : List.of(
                    "dd/MM/yyyy HH:mm", "dd/MM/yyyy H:mm",
                    "yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm",
                    "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy",
                    "dd-MM-yyyy", "yyyy/MM/dd")) {
                try {
                    java.time.format.DateTimeFormatter dtf =
                            java.time.format.DateTimeFormatter.ofPattern(pattern);
                    try {
                        return java.time.LocalDateTime.parse(s, dtf).atOffset(ZoneOffset.UTC);
                    } catch (Exception ignored) {
                        return LocalDate.parse(s, dtf).atStartOfDay().atOffset(ZoneOffset.UTC);
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private Estado resolverEstado(String nombre) {
        if (nombre.isBlank()) return null;
        return estadoRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> estadoRepo.save(
                        Estado.builder().nombre(capitalize(nombre)).build()));
    }

    private Tipo resolverTipo(String nombre) {
        if (nombre.isBlank()) return null;
        return tipoRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> tipoRepo.save(
                        Tipo.builder().nombre(capitalize(nombre)).build()));
    }

    private Cliente resolverCliente(String nombre) {
        if (nombre.isBlank()) return null;
        return clienteRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> clienteRepo.save(
                        Cliente.builder().nombre(capitalize(nombre)).build()));
    }

    private Lugar resolverLugar(String nombre) {
        if (nombre.isBlank()) return null;
        return lugarRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> lugarRepo.save(
                        Lugar.builder().nombre(capitalize(nombre)).build()));
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}