package com.nwav.editorpdf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "nwav_firma")
@Getter
@Setter
@NoArgsConstructor
public class NwavFirma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nwav_id")
    private Long nwavId;

    @Column(name = "nwav_nombre", nullable = false, length = 120)
    private String nwavNombre;

    @Column(name = "nwav_imagen", nullable = false)
    private byte[] nwavImagen;

    @Column(name = "nwav_fecha_creacion", nullable = false)
    private LocalDateTime nwavFechaCreacion;
}
