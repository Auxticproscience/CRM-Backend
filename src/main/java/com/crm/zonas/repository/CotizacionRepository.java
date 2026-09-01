package com.crm.zonas.repository;

import com.crm.zonas.entity.Cotizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {

    boolean existsByNumeroCotizacion(String numeroCotizacion);

    Optional<Cotizacion> findByNumeroCotizacion(String numeroCotizacion);

    @EntityGraph(attributePaths = {
            "propietario",
            "creadoPor",
            "vendedor",
            "cliente",
            "tipoCliente",
            "centroOperacion",
            "listaPrecio",
            "condicionPago"
    })
    @Query("""
            SELECT c
            FROM Cotizacion c
            WHERE (:propietarioId IS NULL OR c.propietario.id = :propietarioId)
              AND (:clienteId IS NULL OR c.cliente.id = :clienteId)
              AND (:centroId IS NULL OR c.centroOperacion.id = :centroId)
              AND (:desde IS NULL OR c.fechaCreacion >= :desde)
              AND (:hasta IS NULL OR c.fechaCreacion < :hasta)
            ORDER BY c.fechaCreacion DESC
            """)
    Page<Cotizacion> buscarConFiltros(
            @Param("propietarioId") Integer propietarioId,
            @Param("clienteId") Integer clienteId,
            @Param("centroId") Integer centroId,
            @Param("desde") java.time.OffsetDateTime desde,
            @Param("hasta") java.time.OffsetDateTime hasta,
            Pageable pageable
    );
}