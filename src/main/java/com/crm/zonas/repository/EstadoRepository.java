package com.crm.zonas.repository;

import com.crm.zonas.entity.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EstadoRepository extends JpaRepository<Estado, Integer> {
    Optional<Estado> findByNombreIgnoreCase(String nombre);
}
