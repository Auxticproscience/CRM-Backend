package com.crm.zonas.dto;

import java.time.OffsetDateTime;

public record ActividadDTO(
    Integer id,
    OffsetDateTime fechaCreacion,
    String nombre,
    String descripcion,
    String estado,
    String tipo,
    String propietario,
    String cliente,
    String lugar,
    OffsetDateTime createdAt
) {}
