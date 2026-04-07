package com.crm.zonas.repository;

import com.crm.zonas.entity.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TipoRepository extends JpaRepository<Tipo, Integer> {
    Optional<Tipo> findByNombreIgnoreCase(String nombre);
}
