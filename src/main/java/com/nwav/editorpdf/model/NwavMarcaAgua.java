package com.nwav.editorpdf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Plantilla reutilizable de marca de agua. Se usa en la Fase 4. */
@Entity
@Table(name = "nwav_marca_agua")
@Getter
@Setter
@NoArgsConstructor
public class NwavMarcaAgua {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nwav_id")
    private Long nwavId;

    @Column(name = "nwav_nombre", nullable = false, length = 120)
    private String nwavNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "nwav_tipo", nullable = false, length = 20)
    private NwavTipoMarcaAgua nwavTipo;

    @Column(name = "nwav_texto", length = 500)
    private String nwavTexto;

    @Column(name = "nwav_imagen")
    private byte[] nwavImagen;

    @Column(name = "nwav_fuente", length = 60)
    private String nwavFuente;

    @Column(name = "nwav_tamano_fuente")
    private Double nwavTamanoFuente;

    /** Color en formato #RRGGBB o #RRGGBBAA. */
    @Column(name = "nwav_color", length = 9)
    private String nwavColor;

    @Column(name = "nwav_opacidad", nullable = false)
    private double nwavOpacidad = 0.3;

    @Column(name = "nwav_rotacion", nullable = false)
    private double nwavRotacion = 45;

    @Column(name = "nwav_escala", nullable = false)
    private double nwavEscala = 1.0;

    @Column(name = "nwav_fecha_creacion", nullable = false)
    private LocalDateTime nwavFechaCreacion;
}
