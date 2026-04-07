package com.crm.zonas.repository;

import com.crm.zonas.entity.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;

public interface ActividadRepository extends JpaRepository<Actividad, Integer> {

    boolean existsByFechaCreacionAndNombreAndPropietarioId(
        OffsetDateTime fechaCreacion, String nombre, Integer propietarioId
    );

    @Query("""
        SELECT a FROM Actividad a
        JOIN FETCH a.propietario p
        LEFT JOIN FETCH a.estado
        LEFT JOIN FETCH a.tipo
        LEFT JOIN FETCH a.cliente
        LEFT JOIN FETCH a.lugar
        WHERE (:propietarioId IS NULL OR p.id = :propietarioId)
        ORDER BY a.fechaCreacion DESC
    """)
    List<Actividad> findAllWithRelations(@Param("propietarioId") Integer propietarioId);
}
