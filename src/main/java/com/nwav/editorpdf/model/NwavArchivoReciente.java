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

@Entity
@Table(name = "nwav_archivo_reciente")
@Getter
@Setter
@NoArgsConstructor
public class NwavArchivoReciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nwav_id")
    private Long nwavId;

    @Column(name = "nwav_ruta", nullable = false, unique = true, length = 2048)
    private String nwavRuta;

    @Column(name = "nwav_nombre", nullable = false)
    private String nwavNombre;

    @Column(name = "nwav_ultima_apertura", nullable = false)
    private LocalDateTime nwavUltimaApertura;

    @Column(name = "nwav_ultima_pagina", nullable = false)
    private int nwavUltimaPagina;

    @Column(name = "nwav_zoom", nullable = false)
    private double nwavZoom = 1.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "nwav_modo_zoom", nullable = false, length = 30)
    private NwavModoZoom nwavModoZoom = NwavModoZoom.NWAV_PORCENTAJE;

    public NwavZoomPreferido getNwavZoomPreferido() {
        return new NwavZoomPreferido(nwavModoZoom, nwavZoom);
    }
}
