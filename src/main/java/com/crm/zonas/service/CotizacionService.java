package com.crm.zonas.service;

import com.crm.zonas.entity.CargaExcel;
import com.crm.zonas.entity.Cotizacion;
import com.crm.zonas.repository.CargaExcelRepository;
import com.crm.zonas.repository.CotizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CotizacionRepository cotizacionRepo;
    private final CargaExcelRepository cargaRepo;

    public List<Cotizacion> listar(Integer propietarioId,
                                   Integer clienteId,
                                   Integer centroId,
                                   LocalDate desde,
                                   LocalDate hasta) {

        return cotizacionRepo
                .findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion"))
                .stream()
                .filter(c -> propietarioId == null ||
                        (c.getPropietario() != null &&
                                c.getPropietario().getId().equals(propietarioId)))
                .filter(c -> clienteId == null ||
                        (c.getCliente() != null &&
                                c.getCliente().getId().equals(clienteId)))
                .filter(c -> centroId == null ||
                        (c.getCentroOperacion() != null &&
                                c.getCentroOperacion().getId().equals(centroId)))
                .filter(c -> desde == null ||
                        !c.getFechaCreacion().toLocalDate().isBefore(desde))
                .filter(c -> hasta == null ||
                        !c.getFechaCreacion().toLocalDate().isAfter(hasta))
                .toList();
    }

    public Optional<Cotizacion> porId(Integer id) {
        return cotizacionRepo.findById(id);
    }

    public List<CargaExcel> historialCargas() {
        return cargaRepo
                .findTop10ByOrderByFechaCargaDesc()
                .stream()
                .filter(c -> c.getNombreArchivo() != null &&
                        c.getNombreArchivo().toLowerCase().startsWith("cot_"))
                .toList();
    }
}