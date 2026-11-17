package com.nwav.editorpdf.repository;

import com.nwav.editorpdf.model.CompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {

    boolean existsByProductoId(Long productoId);
}
