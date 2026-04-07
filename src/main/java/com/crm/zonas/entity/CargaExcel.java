package com.crm.zonas.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cargas_excel")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CargaExcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "fecha_carga", nullable = false)
    @Builder.Default
    private OffsetDateTime fechaCarga = OffsetDateTime.now();

    @Column(name = "registros_cargados", nullable = false)
    @Builder.Default
    private Integer registrosCargados = 0;

    @Column(name = "registros_omitidos", nullable = false)
    @Builder.Default
    private Integer registrosOmitidos = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id")
    private Propietario propietario;

    @Column(columnDefinition = "TEXT")
    private String notas;
}
