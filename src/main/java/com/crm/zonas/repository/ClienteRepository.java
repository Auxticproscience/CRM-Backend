package com.crm.zonas.repository;

import com.crm.zonas.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByNombreIgnoreCase(String nombre);
}
