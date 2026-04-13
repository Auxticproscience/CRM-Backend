package com.crm.zonas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "centros_operacion")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CentroOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;
}