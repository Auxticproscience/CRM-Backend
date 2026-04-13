package com.crm.zonas.repository;

import com.crm.zonas.entity.ListaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, Integer> {
    Optional<ListaPrecio> findByNombreIgnoreCase(String nombre);
}