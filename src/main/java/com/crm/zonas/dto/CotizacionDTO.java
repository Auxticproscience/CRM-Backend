package com.crm.zonas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CotizacionDTO(
        Integer        id,
        OffsetDateTime fechaCreacion,
        String         numeroCotizacion,
        String         propietario,
        String         creadoPor,
        String         vendedor,
        String         cliente,
        String         puntoEnvio,
        String         tipoCliente,
        String         centroOperacion,
        String         listaPrecio,
        String         condicionPago,
        LocalDate      fechaEntrega,
        BigDecimal     valorBruto,
        BigDecimal     valorSubtotal,
        BigDecimal     impuestos,
        BigDecimal     descuentos,
        BigDecimal     descuentoGlobalPct,
        BigDecimal     valorDescuentoGlobal,
        BigDecimal     valorTotal,
        String         pedidoErp,
        BigDecimal     rowidErp,
        String         notasPedido,
        OffsetDateTime createdAt
) {}