package com.crm.zonas.dto;

import java.time.OffsetDateTime;

/**
 * Respuesta de resumen luego de procesar un archivo Excel.
 */
public record CargaResultadoDTO(
    String nombreArchivo,
    int registrosCargados,
    int registrosOmitidos,
    OffsetDateTime fechaCarga,
    String notas
) {}
