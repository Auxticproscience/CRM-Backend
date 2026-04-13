package com.crm.zonas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "condiciones_pago")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CondicionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}