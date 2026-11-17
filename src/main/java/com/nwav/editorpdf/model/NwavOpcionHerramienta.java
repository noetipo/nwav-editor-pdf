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

/** Ultimo valor usado de una opcion de una herramienta. */
@Entity
@Table(name = "nwav_opcion_herramienta")
@Getter
@Setter
@NoArgsConstructor
public class NwavOpcionHerramienta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nwav_id")
    private Long nwavId;

    @Column(name = "nwav_herramienta", nullable = false, length = 60)
    private String nwavHerramienta;

    @Column(name = "nwav_clave", nullable = false, length = 80)
    private String nwavClave;

    @Column(name = "nwav_valor", length = 2048)
    private String nwavValor;

    @Column(name = "nwav_actualizado", nullable = false)
    private LocalDateTime nwavActualizado;
}
