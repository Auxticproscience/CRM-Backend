package com.crm.zonas.service;

import com.crm.zonas.entity.CargaExcel;
import com.crm.zonas.entity.Cotizacion;
import com.crm.zonas.repository.CargaExcelRepository;
import com.crm.zonas.repository.CotizacionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

        Specification<Cotizacion> filtros = (root, query, cb) -> {
            var predicados = new ArrayList<Predicate>();

            if (propietarioId != null) {
                predicados.add(cb.equal(root.get("propietario").get("id"), propietarioId));
            }
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (centroId != null) {
                predicados.add(cb.equal(root.get("centroOperacion").get("id"), centroId));
            }
            if (fechaDesde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicados.add(cb.lessThan(root.get("fechaCreacion"), fechaHasta));
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };

        return cotizacionRepo.findAll(filtros, pageable);
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
