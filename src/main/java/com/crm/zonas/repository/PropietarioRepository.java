package com.crm.zonas.repository;

import com.crm.zonas.entity.Propietario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PropietarioRepository extends JpaRepository<Propietario, Integer> {
    Optional<Propietario> findByNombreIgnoreCase(String nombre);
}
