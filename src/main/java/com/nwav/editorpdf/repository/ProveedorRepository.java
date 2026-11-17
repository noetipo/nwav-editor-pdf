package com.nwav.editorpdf.repository;

import com.nwav.editorpdf.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long>, JpaSpecificationExecutor<Proveedor> {

    Optional<Proveedor> findByRuc(String ruc);
}
