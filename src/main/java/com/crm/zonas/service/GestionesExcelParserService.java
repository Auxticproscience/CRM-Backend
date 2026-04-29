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
public class GestionesExcelParserService {

    private final ActividadRepository    actividadRepo;
    private final EstadoRepository       estadoRepo;
    private final TipoRepository         tipoRepo;
    private final PropietarioRepository  propietarioRepo;
    private final ClienteRepository      clienteRepo;
    private final LugarRepository        lugarRepo;
    private final CargaExcelRepository   cargaRepo;

    private static final int COL_FECHA        = 0;
    private static final int COL_DESCRIPCION  = 1;
    private static final int COL_NOMBRE       = 2;
    private static final int COL_PROPIETARIO  = 3;
    private static final int COL_LUGAR        = 4;
    private static final int COL_CLIENTE      = 5;
    private static final int COL_ESTADO       = 7;
    private static final int COL_TIPO         = 8;

    public CargaResultadoDTO procesarStream(InputStream inputStream, String nombreArchivo) throws IOException {

        int insertados = 0;
        int duplicados = 0;
        int invalidos = 0;
        int erroresDb = 0;

        List<String> logs = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(inputStream)) {

            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    OffsetDateTime fecha = parseFecha(row.getCell(COL_FECHA), evaluator);
                    String nombre = texto(row.getCell(COL_NOMBRE), fmt);
                    String propNombre = texto(row.getCell(COL_PROPIETARIO), fmt);

                    if (fecha == null || nombre.isBlank() || propNombre.isBlank()) {
                        invalidos++;
                        logs.add("Fila " + (i + 1) + " inválida (campos obligatorios)");
                        continue;
                    }

                    Propietario propietario = propietarioRepo
                            .findByNombreIgnoreCase(propNombre)
                            .orElseGet(() -> propietarioRepo.save(
                                    Propietario.builder()
                                            .nombre(propNombre.toUpperCase())
                                            .build()));

                    boolean existe = actividadRepo
                            .existsByFechaCreacionAndNombreAndPropietarioId(
                                    fecha, nombre, propietario.getId());

                    if (existe) {
                        duplicados++;
                        logs.add("Fila " + (i + 1) + " duplicada");
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
                    insertados++;

                } catch (Exception e) {
                    erroresDb++;
                    logs.add("Fila " + (i + 1) + " error DB: " + e.getMessage());
                    log.warn("Error fila {}: {}", i + 1, e.getMessage());
                }
            }
        }

        CargaExcel registro = CargaExcel.builder()
                .nombreArchivo(nombreArchivo)
                .registrosCargados(insertados)
                .registrosOmitidos(invalidos + duplicados + erroresDb)
                .notas(String.join(" | ", logs))
                .build();

        cargaRepo.save(registro);

        log.info("""
            Carga finalizada:
            archivo: {}
            insertados: {}
            duplicados: {}
            invalidos: {}
            erroresDB: {}
        """, nombreArchivo, insertados, duplicados, invalidos, erroresDb);

        return new CargaResultadoDTO(
                nombreArchivo,
                insertados,
                invalidos + duplicados + erroresDb,
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

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().atOffset(ZoneOffset.UTC);
            }

            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim();

                java.time.format.DateTimeFormatter dtf =
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                return java.time.LocalDateTime.parse(s, dtf).atOffset(ZoneOffset.UTC);
            }
        } catch (Exception ignored) {}

        return null;
    }

    private Estado resolverEstado(String nombre) {
        if (nombre.isBlank()) return null;
        return estadoRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> estadoRepo.save(
                        Estado.builder().nombre(nombre).build()));
    }

    private Tipo resolverTipo(String nombre) {
        if (nombre.isBlank()) return null;
        return tipoRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> tipoRepo.save(
                        Tipo.builder().nombre(nombre).build()));
    }

    private Cliente resolverCliente(String nombre) {
        if (nombre.isBlank()) return null;
        return clienteRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> clienteRepo.save(
                        Cliente.builder().nombre(nombre).build()));
    }

    private Lugar resolverLugar(String nombre) {
        if (nombre.isBlank()) return null;
        return lugarRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> lugarRepo.save(
                        Lugar.builder().nombre(nombre).build()));
    }
}