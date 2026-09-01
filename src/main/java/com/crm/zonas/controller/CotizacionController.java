package com.crm.zonas.controller;

import com.crm.zonas.dto.CargaHistorialDTO;
import com.crm.zonas.dto.CotizacionDTO;
import com.crm.zonas.entity.CargaExcel;
import com.crm.zonas.entity.Cotizacion;
import com.crm.zonas.service.CotizacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;

    /**
     * GET /api/cotizaciones
     *
     * Parámetros opcionales:
     *   ?propietarioId=1
     *   ?clienteId=5
     *   ?centroId=3
     *   ?desde=2026-04-01
     *   ?hasta=2026-04-13
     *   ?page=0
     *   ?size=50
     */
    @GetMapping
    public ResponseEntity<Page<CotizacionDTO>> listar(
            @RequestParam(required = false) Integer propietarioId,
            @RequestParam(required = false) Integer clienteId,
            @RequestParam(required = false) Integer centroId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Protección para evitar peticiones gigantes.
        int pageSize = Math.min(Math.max(size, 1), 200);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                pageSize,
                Sort.by(Sort.Direction.DESC, "fechaCreacion")
        );

        Page<CotizacionDTO> result = cotizacionService
                .listar(
                        propietarioId,
                        clienteId,
                        centroId,
                        desde,
                        hasta,
                        pageable
                )
                .map(this::toDTO);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/cotizaciones/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CotizacionDTO> porId(@PathVariable Integer id) {
        return cotizacionService.porId(id)
                .map(c -> ResponseEntity.ok(toDTO(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/cotizaciones/historial-cargas
     */
    @GetMapping("/historial-cargas")
    public ResponseEntity<List<CargaHistorialDTO>> historial() {
        List<CargaHistorialDTO> result = cotizacionService.historialCargas()
                .stream()
                .map(this::toCargaDTO)
                .toList();

        return ResponseEntity.ok(result);
    }

    private CotizacionDTO toDTO(Cotizacion c) {
        return new CotizacionDTO(
                c.getId(),
                c.getFechaCreacion(),
                c.getNumeroCotizacion(),
                c.getPropietario()     != null ? c.getPropietario().getNombre()     : null,
                c.getCreadoPor()       != null ? c.getCreadoPor().getNombre()       : null,
                c.getVendedor()        != null ? c.getVendedor().getNombre()        : null,
                c.getCliente()         != null ? c.getCliente().getNombre()         : null,
                c.getPuntoEnvio(),
                c.getTipoCliente()     != null ? c.getTipoCliente().getNombre()     : null,
                c.getCentroOperacion() != null ? c.getCentroOperacion().getNombre() : null,
                c.getListaPrecio()     != null ? c.getListaPrecio().getNombre()     : null,
                c.getCondicionPago()   != null ? c.getCondicionPago().getNombre()   : null,
                c.getFechaEntrega(),
                c.getValorBruto(),
                c.getValorSubtotal(),
                c.getImpuestos(),
                c.getDescuentos(),
                c.getDescuentoGlobalPct(),
                c.getValorDescuentoGlobal(),
                c.getValorTotal(),
                c.getPedidoErp(),
                c.getRowidErp(),
                c.getNotasPedido(),
                c.getCreatedAt()
        );
    }

    private CargaHistorialDTO toCargaDTO(CargaExcel c) {
        return new CargaHistorialDTO(
                c.getId(),
                c.getNombreArchivo(),
                c.getFechaCarga(),
                c.getRegistrosCargados(),
                c.getRegistrosOmitidos(),
                c.getNotas()
        );
    }
}