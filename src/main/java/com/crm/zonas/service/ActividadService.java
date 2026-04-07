package com.crm.zonas.service;

import com.crm.zonas.entity.Actividad;
import com.crm.zonas.repository.ActividadRepository;
import com.crm.zonas.repository.CargaExcelRepository;
import com.crm.zonas.entity.CargaExcel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActividadService {

    private final ActividadRepository actividadRepo;
    private final CargaExcelRepository cargaRepo;

    @Transactional(readOnly = true)
    public List<Actividad> listar(Integer propietarioId) {
        return actividadRepo.findAllWithRelations(propietarioId);
    }

    @Transactional(readOnly = true)
    public List<CargaExcel> historialCargas() {
        return cargaRepo.findTop10ByOrderByFechaCargaDesc();
    }
}
