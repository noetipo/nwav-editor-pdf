package com.nwav.editorpdf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "nwav_configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NwavConfiguracion {
    @Id
    @Column(name = "nwav_clave", length = 100)
    private String nwavClave;

    @Column(name = "nwav_valor", length = 2048)
    private String nwavValor;
}
