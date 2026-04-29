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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CotizacionesParserService {

    private final CotizacionRepository    cotizacionRepo;
    private final PropietarioRepository   propietarioRepo;
    private final ClienteRepository       clienteRepo;
    private final TipoClienteRepository   tipoClienteRepo;
    private final CentroOperacionRepository centroOperacionRepo;
    private final ListaPrecioRepository   listaPrecioRepo;
    private final CondicionPagoRepository condicionPagoRepo;
    private final CargaExcelRepository    cargaRepo;

    private static final int COL_FECHA_CREACION       = 0;
    private static final int COL_NOTAS_PEDIDO         = 1;
    private static final int COL_NUMERO_COTIZACION    = 2;
    private static final int COL_PROPIETARIO          = 3;
    private static final int COL_CREADO_POR           = 4;
    private static final int COL_VALOR_TOTAL          = 5;
    private static final int COL_TIPO_CLIENTE         = 6;
    private static final int COL_FACTURAR_A           = 7;
    private static final int COL_CENTRO_OPERACION     = 8;
    private static final int COL_FECHA_ENTREGA        = 9;
    private static final int COL_LISTA_PRECIOS        = 10;
    private static final int COL_PUNTO_ENVIO          = 11;
    private static final int COL_VALOR_SUBTOTAL       = 12;
    private static final int COL_VALOR_BRUTO          = 13;
    private static final int COL_IMPUESTOS            = 14;
    private static final int COL_DESCUENTOS           = 15;
    private static final int COL_CONDICION_PAGO       = 16;
    private static final int COL_PEDIDO_ERP           = 17;
    private static final int COL_ROWID_ERP            = 18;
    private static final int COL_VENDEDOR             = 19;
    private static final int COL_DESCUENTO_GLOBAL_PCT = 20;
    private static final int COL_VALOR_DESC_GLOBAL    = 21;


    @Transactional
    public CargaResultadoDTO parsearYGuardar(ByteArrayInputStream stream,
                                             String nombreArchivo) throws IOException {
        return procesarStream(stream, nombreArchivo);
    }


    @Transactional
    public CargaResultadoDTO procesarExcel(InputStream inputStream,
                                           String nombreArchivo) throws IOException {
        return procesarStream(inputStream, nombreArchivo);
    }

    private CargaResultadoDTO procesarStream(InputStream inputStream,
                                             String nombreArchivo) throws IOException {
        int cargados = 0, omitidos = 0;
        List<String> errores = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(inputStream)) {
            Sheet sheet     = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            // Fila 0 = encabezados → empezar en fila 1
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String numeroCot = texto(row.getCell(COL_NUMERO_COTIZACION), fmt);
                    String propNombre = texto(row.getCell(COL_PROPIETARIO), fmt);
                    String creadoPorNombre = texto(row.getCell(COL_CREADO_POR), fmt);
                    OffsetDateTime fechaCreacion = parseFechaHora(
                            row.getCell(COL_FECHA_CREACION), evaluator);

                    BigDecimal valorTotal = parseDecimal(
                            row.getCell(COL_VALOR_TOTAL), fmt);

                    if (numeroCot.isBlank() || propNombre.isBlank()
                            || creadoPorNombre.isBlank() || fechaCreacion == null || valorTotal == null) {
                        omitidos++;
                        errores.add("Fila " + (i + 1) + ": campos obligatorios vacíos");
                        continue;
                    }

                    // ── Deduplicación por número de cotización ────
                    if (cotizacionRepo.existsByNumeroCotizacion(numeroCot)) {
                        omitidos++;
                        continue;
                    }

                    // ── Resolver catálogos (auto-create) ──────────
                    Propietario propietario = resolverPropietario(propNombre);
                    Propietario creadoPor   = resolverPropietario(creadoPorNombre);
                    Propietario vendedor    = resolverPropietarioOpcional(
                            texto(row.getCell(COL_VENDEDOR), fmt));

                    Cliente cliente = resolverCliente(
                            texto(row.getCell(COL_FACTURAR_A), fmt));
                    TipoCliente tipoCliente = resolverTipoCliente(
                            texto(row.getCell(COL_TIPO_CLIENTE), fmt));
                    CentroOperacion centroOp = resolverCentroOperacion(
                            texto(row.getCell(COL_CENTRO_OPERACION), fmt));
                    ListaPrecio listaPrecio = resolverListaPrecio(
                            texto(row.getCell(COL_LISTA_PRECIOS), fmt));
                    CondicionPago condicionPago = resolverCondicionPago(
                            texto(row.getCell(COL_CONDICION_PAGO), fmt));

                    // ── Construir y guardar ───────────────────────
                    Cotizacion c = Cotizacion.builder()
                            .fechaCreacion(fechaCreacion)
                            .numeroCotizacion(numeroCot)
                            .propietario(propietario)
                            .creadoPor(creadoPor)
                            .vendedor(vendedor)
                            .cliente(cliente)
                            .puntoEnvio(texto(row.getCell(COL_PUNTO_ENVIO), fmt))
                            .tipoCliente(tipoCliente)
                            .centroOperacion(centroOp)
                            .listaPrecio(listaPrecio)
                            .condicionPago(condicionPago)
                            .fechaEntrega(parseFechaSolo(
                                    row.getCell(COL_FECHA_ENTREGA), evaluator))
                            .valorBruto(parseDecimal(
                                    row.getCell(COL_VALOR_BRUTO), fmt))
                            .valorSubtotal(parseDecimal(
                                    row.getCell(COL_VALOR_SUBTOTAL), fmt))
                            .impuestos(parseDecimal(
                                    row.getCell(COL_IMPUESTOS), fmt))
                            .descuentos(parseDecimal(
                                    row.getCell(COL_DESCUENTOS), fmt))
                            .descuentoGlobalPct(parseDecimal(
                                    row.getCell(COL_DESCUENTO_GLOBAL_PCT), fmt))
                            .valorDescuentoGlobal(parseDecimal(
                                    row.getCell(COL_VALOR_DESC_GLOBAL), fmt))
                            .valorTotal(parseDecimal(
                                    row.getCell(COL_VALOR_TOTAL), fmt))
                            .pedidoErp(texto(row.getCell(COL_PEDIDO_ERP), fmt))
                            .rowidErp(parseDecimal(
                                    row.getCell(COL_ROWID_ERP), fmt))
                            .notasPedido(texto(row.getCell(COL_NOTAS_PEDIDO), fmt))
                            .build();

                    cotizacionRepo.save(c);
                    cargados++;

                } catch (Exception e) {
                    log.warn("Fila {} omitida: {}", i + 1, e.getMessage());
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                    omitidos++;
                }
            }
        }

        CargaExcel registro = CargaExcel.builder()
                .nombreArchivo(nombreArchivo)
                .registrosCargados(cargados)
                .registrosOmitidos(omitidos)
                .notas(errores.isEmpty() ? null : String.join(" | ", errores))
                .build();
        cargaRepo.save(registro);

        log.info("Cotizaciones — archivo: {}, insertadas: {}, omitidas: {}",
                nombreArchivo, cargados, omitidos);

        return new CargaResultadoDTO(
                nombreArchivo,
                cargados,
                omitidos,
                registro.getFechaCarga(),
                registro.getNotas()
        );
    }


    private Propietario resolverPropietario(String nombre) {
        return propietarioRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> propietarioRepo.save(
                        Propietario.builder().nombre(nombre.toUpperCase()).build()));
    }

    /** Igual que resolverPropietario pero retorna null si el nombre está vacío. */
    private Propietario resolverPropietarioOpcional(String nombre) {
        if (nombre.isBlank()) return null;
        return resolverPropietario(nombre);
    }

    private Cliente resolverCliente(String nombre) {
        if (nombre.isBlank()) return null;
        return clienteRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> clienteRepo.save(
                        Cliente.builder().nombre(nombre).build()));
    }

    private TipoCliente resolverTipoCliente(String nombre) {
        if (nombre.isBlank()) return null;
        return tipoClienteRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> tipoClienteRepo.save(
                        TipoCliente.builder().nombre(nombre.toUpperCase()).build()));
    }

    private CentroOperacion resolverCentroOperacion(String nombre) {
        if (nombre.isBlank()) return null;
        return centroOperacionRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> centroOperacionRepo.save(
                        CentroOperacion.builder().nombre(nombre.toUpperCase()).build()));
    }

    private ListaPrecio resolverListaPrecio(String nombre) {
        if (nombre.isBlank()) return null;
        return listaPrecioRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> listaPrecioRepo.save(
                        ListaPrecio.builder().nombre(nombre.toUpperCase()).build()));
    }

    private CondicionPago resolverCondicionPago(String nombre) {
        if (nombre.isBlank()) return null;
        return condicionPagoRepo.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> condicionPagoRepo.save(
                        CondicionPago.builder().nombre(nombre.toUpperCase()).build()));
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers de parseo
    // ─────────────────────────────────────────────────────────────

    private String texto(Cell cell, DataFormatter fmt) {
        if (cell == null) return "";
        return fmt.formatCellValue(cell).trim().replaceAll("\\s+", " ");
    }

    private OffsetDateTime parseFechaHora(Cell cell, FormulaEvaluator ev) {
        if (cell == null) return null;

        CellType type = cell.getCellType() == CellType.FORMULA
                ? ev.evaluateFormulaCell(cell) : cell.getCellType();

        if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().atOffset(ZoneOffset.UTC);
        }

        if (type == CellType.STRING) {
            String s = cell.getStringCellValue().trim().replaceAll("\\s+", " ");
            for (String pattern : List.of(
                    "dd/MM/yyyy HH:mm", "dd/MM/yyyy H:mm",
                    "yyyy-MM-dd HH:mm", "yyyy-MM-dd H:mm",
                    "yyyy-MM-dd", "dd/MM/yyyy")) {
                try {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
                    try {
                        return LocalDateTime.parse(s, dtf).atOffset(ZoneOffset.UTC);
                    } catch (Exception ignored) {
                        return LocalDate.parse(s, dtf).atStartOfDay().atOffset(ZoneOffset.UTC);
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private LocalDate parseFechaSolo(Cell cell, FormulaEvaluator ev) {
        if (cell == null) return null;

        CellType type = cell.getCellType() == CellType.FORMULA
                ? ev.evaluateFormulaCell(cell) : cell.getCellType();

        if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }

        if (type == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            for (String pattern : List.of(
                    "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy")) {
                try {
                    return LocalDate.parse(s, DateTimeFormatter.ofPattern(pattern));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private BigDecimal parseDecimal(Cell cell, DataFormatter fmt) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }

        String s = fmt.formatCellValue(cell).trim().replace(",", "");
        if (s.isBlank()) return null;

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            log.warn("No se pudo parsear valor decimal: '{}'", s);
            return null;
        }
    }
}