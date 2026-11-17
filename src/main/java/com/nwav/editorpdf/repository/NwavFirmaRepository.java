package com.nwav.editorpdf.repository;

import com.nwav.editorpdf.model.NwavFirma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NwavFirmaRepository extends JpaRepository<NwavFirma, Long> {
    List<NwavFirma> findAllByOrderByNwavFechaCreacionDesc();
}
