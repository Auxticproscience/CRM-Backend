package com.crm.zonas.service;

import com.crm.zonas.entity.CargaExcel;
import com.crm.zonas.entity.Cotizacion;
import com.crm.zonas.repository.CargaExcelRepository;
import com.crm.zonas.repository.CotizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepo;
    private final CargaExcelRepository cargaRepo;

    public Page<Cotizacion> listar(
            Integer propietarioId,
            Integer clienteId,
            Integer centroId,
            LocalDate desde,
            LocalDate hasta,
            Pageable pageable) {

        OffsetDateTime fechaDesde = desde != null
                ? desde.atStartOfDay().atOffset(ZoneOffset.UTC)
                : null;

        OffsetDateTime fechaHasta = hasta != null
                ? hasta.plusDays(1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC)
                : null;

        return cotizacionRepo.buscarConFiltros(
                propietarioId,
                clienteId,
                centroId,
                fechaDesde,
                fechaHasta,
                pageable
        );
    }

    public Optional<Cotizacion> porId(Integer id) {
        return cotizacionRepo.findById(id);
    }

    public java.util.List<CargaExcel> historialCargas() {
        return cargaRepo
                .findTop10ByOrderByFechaCargaDesc()
                .stream()
                .filter(c -> c.getNombreArchivo() != null &&
                        c.getNombreArchivo().toLowerCase().startsWith("cot_"))
                .toList();
    }
}