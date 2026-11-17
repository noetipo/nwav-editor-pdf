package com.nwav.editorpdf.repository;

import com.nwav.editorpdf.model.NwavOpcionHerramienta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NwavOpcionHerramientaRepository extends JpaRepository<NwavOpcionHerramienta, Long> {

    List<NwavOpcionHerramienta> findByNwavHerramienta(String nwavHerramienta);
}
