package com.nwav.editorpdf.repository;

import com.nwav.editorpdf.model.NwavMarcaAgua;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NwavMarcaAguaRepository extends JpaRepository<NwavMarcaAgua, Long> {
    List<NwavMarcaAgua> findAllByOrderByNwavNombreAsc();
}
