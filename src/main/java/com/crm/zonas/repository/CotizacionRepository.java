package com.crm.zonas.repository;

import com.crm.zonas.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {

    boolean existsByNumeroCotizacion(String numeroCotizacion);

    Optional<Cotizacion> findByNumeroCotizacion(String numeroCotizacion);
}