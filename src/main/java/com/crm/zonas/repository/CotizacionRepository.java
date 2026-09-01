package com.crm.zonas.repository;

import com.crm.zonas.entity.Cotizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer>,
        JpaSpecificationExecutor<Cotizacion> {

    boolean existsByNumeroCotizacion(String numeroCotizacion);

    Optional<Cotizacion> findByNumeroCotizacion(String numeroCotizacion);

    @Override
    @EntityGraph(attributePaths = {
            "propietario", "creadoPor", "vendedor", "cliente",
            "tipoCliente", "centroOperacion", "listaPrecio", "condicionPago"
    })
    Page<Cotizacion> findAll(Specification<Cotizacion> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "propietario", "creadoPor", "vendedor", "cliente",
            "tipoCliente", "centroOperacion", "listaPrecio", "condicionPago"
    })
    Optional<Cotizacion> findById(Integer id);
}
