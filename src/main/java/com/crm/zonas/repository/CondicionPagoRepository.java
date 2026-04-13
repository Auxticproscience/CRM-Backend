package com.crm.zonas.repository;

import com.crm.zonas.entity.CondicionPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CondicionPagoRepository extends JpaRepository<CondicionPago, Integer> {
    Optional<CondicionPago> findByNombreIgnoreCase(String nombre);
}