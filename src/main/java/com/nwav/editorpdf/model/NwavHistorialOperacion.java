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

/** Registro de una operacion sobre archivos PDF. Se llena a partir de la Fase 2. */
@Entity
@Table(name = "nwav_historial_operacion")
@Getter
@Setter
@NoArgsConstructor
public class NwavHistorialOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nwav_id")
    private Long nwavId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nwav_tipo", nullable = false, length = 40)
    private NwavTipoOperacion nwavTipo;

    @Column(name = "nwav_archivo_origen", length = 4096)
    private String nwavArchivoOrigen;

    @Column(name = "nwav_archivo_destino", length = 2048)
    private String nwavArchivoDestino;

    @Column(name = "nwav_fecha", nullable = false)
    private LocalDateTime nwavFecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "nwav_resultado", nullable = false, length = 20)
    private NwavResultadoOperacion nwavResultado;

    @Column(name = "nwav_detalle", length = 2048)
    private String nwavDetalle;
}
