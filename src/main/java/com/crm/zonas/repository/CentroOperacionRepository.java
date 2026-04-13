package com.crm.zonas.repository;

import com.crm.zonas.entity.CentroOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CentroOperacionRepository extends JpaRepository<CentroOperacion, Integer> {
    Optional<CentroOperacion> findByNombreIgnoreCase(String nombre);
}