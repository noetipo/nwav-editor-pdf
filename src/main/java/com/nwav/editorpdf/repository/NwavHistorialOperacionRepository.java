package com.nwav.editorpdf.repository;

import com.nwav.editorpdf.model.NwavHistorialOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NwavHistorialOperacionRepository extends JpaRepository<NwavHistorialOperacion, Long> {

    List<NwavHistorialOperacion> findAllByOrderByNwavFechaDescNwavIdDesc();
}
