package com.crm.zonas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lugares")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lugar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 100)
    private String ciudad;
}
