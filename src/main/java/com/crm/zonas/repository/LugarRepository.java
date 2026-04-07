package com.crm.zonas.repository;

import com.crm.zonas.entity.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LugarRepository extends JpaRepository<Lugar, Integer> {
    Optional<Lugar> findByNombreIgnoreCase(String nombre);
}
