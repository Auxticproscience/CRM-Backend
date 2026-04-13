package com.crm.zonas.controller;

import com.crm.zonas.dto.ActividadDTO;
import com.crm.zonas.dto.CargaHistorialDTO;
import com.crm.zonas.entity.Actividad;
import com.crm.zonas.entity.CargaExcel;
import com.crm.zonas.service.ActividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actividades")
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService actividadService;

    @GetMapping
    public ResponseEntity<List<ActividadDTO>> listar(
            @RequestParam(required = false) Integer propietarioId) {

        List<ActividadDTO> result = actividadService.listar(propietarioId)
            .stream()
            .map(this::toDTO)
            .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/historial-cargas")
    public ResponseEntity<List<CargaHistorialDTO>> historial() {
        List<CargaHistorialDTO> result = actividadService.historialCargas()
            .stream()
            .map(this::toCargaDTO)
            .toList();
        return ResponseEntity.ok(result);
    }

    private ActividadDTO toDTO(Actividad a) {
        return new ActividadDTO(
            a.getId(),
            a.getFechaCreacion(),
            a.getNombre(),
            a.getDescripcion(),
            a.getEstado()      != null ? a.getEstado().getNombre()      : null,
            a.getTipo()        != null ? a.getTipo().getNombre()        : null,
            a.getPropietario() != null ? a.getPropietario().getNombre() : null,
            a.getCliente()     != null ? a.getCliente().getNombre()     : null,
            a.getLugar()       != null ? a.getLugar().getNombre()       : null,
            a.getCreatedAt()
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
