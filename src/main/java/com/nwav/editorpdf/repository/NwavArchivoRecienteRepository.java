package com.nwav.editorpdf.repository;

import com.nwav.editorpdf.model.NwavArchivoReciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NwavArchivoRecienteRepository extends JpaRepository<NwavArchivoReciente, Long> {

    Optional<NwavArchivoReciente> findByNwavRuta(String nwavRuta);

    List<NwavArchivoReciente> findAllByOrderByNwavUltimaAperturaDesc();
}
