package com.crm.zonas.repository;

import com.crm.zonas.entity.TipoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoClienteRepository extends JpaRepository<TipoCliente, Integer> {
    Optional<TipoCliente> findByNombreIgnoreCase(String nombre);
}