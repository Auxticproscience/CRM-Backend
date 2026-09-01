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
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private static final int BATCH_SIZE       = 500;

    @Transactional
    public CargaResultadoDTO parsearYGuardar(ByteArrayInputStream stream, String nombreArchivo) throws IOException {
        return procesarStream(stream, nombreArchivo);
    }

    @Transactional
    public CargaResultadoDTO procesarExcel(MultipartFile file) throws IOException {
        return procesarStream(file.getInputStream(), file.getOriginalFilename());
    }


    public CargaResultadoDTO procesarStream(InputStream inputStream, String nombreArchivo) throws IOException {

        int insertados = 0;
        int duplicados = 0;
        int invalidos = 0;
        int erroresDb = 0;

        List<String> logs = new ArrayList<>();
        List<Actividad> lote = new ArrayList<>(BATCH_SIZE);
        Set<String> actividadesExistentes = new HashSet<>();
        Set<String> actividadesProcesadas = new HashSet<>();

        try (Workbook wb = new XSSFWorkbook(inputStream)) {

            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            Map<String, Propietario> propietarios = cargarPorNombre(propietarioRepo.findAll(), Propietario::getNombre);
            Map<String, Estado> estados = cargarPorNombre(estadoRepo.findAll(), Estado::getNombre);
            Map<String, Tipo> tipos = cargarPorNombre(tipoRepo.findAll(), Tipo::getNombre);
            Map<String, Cliente> clientes = cargarPorNombre(clienteRepo.findAll(), Cliente::getNombre);
            Map<String, Lugar> lugares = cargarPorNombre(lugarRepo.findAll(), Lugar::getNombre);

            for (Actividad actividad : actividadRepo.findAll()) {
                if (actividad.getFechaCreacion() != null
                        && actividad.getNombre() != null
                        && actividad.getPropietario() != null
                        && actividad.getPropietario().getId() != null) {

                    actividadesExistentes.add(
                            claveActividad(
                                    actividad.getFechaCreacion(),
                                    actividad.getNombre(),
                                    actividad.getPropietario().getId()
                            )
                    );
                }
            }

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
                    if(!lote.isEmpty()){
                        actividadRepo.saveAll(lote);
                        actividadRepo.flush();
                        lote.clear();
                    }

                    Propietario propietario = resolverPropietario(propNombre, propietarios);

                    String clave = claveActividad(
                            fecha,
                            nombre,
                            propietario.getId()
                    );

                    if (!actividadesProcesadas.add(clave)) {
                        duplicados++;
                        logs.add("Fila " + (i + 1) + " duplicada dentro del archivo");
                        continue;
                    }

                    if (actividadesExistentes.contains(clave)) {
                        duplicados++;
                        logs.add("Fila " + (i + 1) + " duplicada en la base de datos");
                        continue;
                    }

                    Actividad a = Actividad.builder()
                            .fechaCreacion(fecha)
                            .nombre(nombre)
                            .descripcion(texto(row.getCell(COL_DESCRIPCION), fmt))
                            .estado(resolverEstado(texto(row.getCell(COL_ESTADO), fmt), estados))
                            .tipo(resolverTipo(texto(row.getCell(COL_TIPO), fmt), tipos))
                            .propietario(propietario)
                            .cliente(resolverCliente(texto(row.getCell(COL_CLIENTE), fmt), clientes))
                            .lugar(resolverLugar(texto(row.getCell(COL_LUGAR), fmt), lugares))
                            .build();

                    lote.add(a);
                    actividadesExistentes.add(clave);
                    insertados++;

                    if (lote.size() >= BATCH_SIZE) {
                        actividadRepo.saveAll(lote);
                        actividadRepo.flush();
                        lote.clear();
                    }

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

    private <T> Map<String, T> cargarPorNombre(List<T> entidades, Function<T, String> extractor) {
        return entidades.stream()
                .filter(Objects::nonNull)
                .filter(e -> extractor.apply(e) != null && !extractor.apply(e).isBlank())
                .collect(Collectors.toMap(
                        e -> normalizar(extractor.apply(e)),
                        Function.identity(),
                        (existente, ignorado) -> existente,
                        HashMap::new
                ));
    }

    private Propietario resolverPropietario(String nombre, Map<String, Propietario> cache) {
        String key = normalizar(nombre);
        Propietario existente = cache.get(key);
        if (existente != null) return existente;

        Propietario nuevo = propietarioRepo.save(
                Propietario.builder().nombre(nombre.trim().toUpperCase()).build());
        cache.put(key, nuevo);
        return nuevo;
    }

    private Estado resolverEstado(String nombre, Map<String, Estado> cache) {
        if (nombre.isBlank()) return null;
        return cache.computeIfAbsent(normalizar(nombre), key ->
                estadoRepo.save(Estado.builder().nombre(nombre.trim()).build()));
    }

    private Tipo resolverTipo(String nombre, Map<String, Tipo> cache) {
        if (nombre.isBlank()) return null;
        return cache.computeIfAbsent(normalizar(nombre), key ->
                tipoRepo.save(Tipo.builder().nombre(nombre.trim()).build()));
    }

    private Cliente resolverCliente(String nombre, Map<String, Cliente> cache) {
        if (nombre.isBlank()) return null;
        return cache.computeIfAbsent(normalizar(nombre), key ->
                clienteRepo.save(Cliente.builder().nombre(nombre.trim()).build()));
    }

    private Lugar resolverLugar(String nombre, Map<String, Lugar> cache) {
        if (nombre.isBlank()) return null;
        return cache.computeIfAbsent(normalizar(nombre), key ->
                lugarRepo.save(Lugar.builder().nombre(nombre.trim()).build()));
    }

    private String claveActividad( OffsetDateTime fecha,
                                   String nombre, Integer propietarioId){
        return fecha + "|" + normalizar(nombre) + "|" + propietarioId;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}