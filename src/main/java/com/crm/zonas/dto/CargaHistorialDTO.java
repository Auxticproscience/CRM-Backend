package com.crm.zonas.dto;

import java.time.OffsetDateTime;

public record CargaHistorialDTO(
    Integer id,
    String nombreArchivo,
    OffsetDateTime fechaCarga,
    int registrosCargados,
    int registrosOmitidos,
    String notas
) {}
