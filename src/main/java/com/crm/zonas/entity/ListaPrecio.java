package com.crm.zonas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "listas_precio")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListaPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}