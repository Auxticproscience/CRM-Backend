package com.crm.zonas.repository;

import com.crm.zonas.entity.CargaExcel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CargaExcelRepository extends JpaRepository<CargaExcel, Integer> {
    List<CargaExcel> findTop10ByOrderByFechaCargaDesc();
}
