package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.model.NwavColorRgb;
import com.nwav.editorpdf.model.NwavFuente;
import com.nwav.editorpdf.model.NwavMarcaAgua;
import com.nwav.editorpdf.model.NwavOpcionesMarcaAgua;
import com.nwav.editorpdf.model.NwavTipoMarcaAgua;
import com.nwav.editorpdf.repository.NwavMarcaAguaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NwavMarcaAguaService {
    private final NwavMarcaAguaRepository nwavRepositorio;

    @Transactional(readOnly = true)
    public List<NwavMarcaAgua> nwavListar() {
        java.text.Collator nwavOrden = java.text.Collator.getInstance(Locale.forLanguageTag("es"));
        nwavOrden.setStrength(java.text.Collator.SECONDARY);
        return nwavRepositorio.findAllByOrderByNwavNombreAsc().stream()
                .sorted(java.util.Comparator.comparing(NwavMarcaAgua::getNwavNombre, nwavOrden))
                .toList();
    }

    @Transactional
    public NwavMarcaAgua nwavGuardar(String nwavNombre, NwavOpcionesMarcaAgua nwavOpciones) {
        String nwavLimpio = nwavNombre == null ? "" : nwavNombre.strip();
        if (nwavLimpio.isEmpty()) {
            throw new NwavEditorException("Ponle un nombre a la plantilla.");
        }
        if (nwavOpciones.nwavTipo() == NwavTipoMarcaAgua.NWAV_IMAGEN
                && (nwavOpciones.nwavImagen() == null || nwavOpciones.nwavImagen().length == 0)) {
            throw new NwavEditorException("Elige la imagen antes de guardar la plantilla.");
        }
        Optional<NwavMarcaAgua> nwavExistente = nwavRepositorio.findAllByOrderByNwavNombreAsc().stream()
                .filter(nwavPlantilla -> nwavPlantilla.getNwavNombre().equalsIgnoreCase(nwavLimpio))
                .findFirst();
        NwavMarcaAgua nwavPlantilla = nwavExistente.orElseGet(NwavMarcaAgua::new);
        nwavPlantilla.setNwavNombre(nwavLimpio);
        nwavPlantilla.setNwavTipo(nwavOpciones.nwavTipo());
        nwavPlantilla.setNwavTexto(nwavOpciones.nwavTexto());
        nwavPlantilla.setNwavImagen(nwavOpciones.nwavTipo() == NwavTipoMarcaAgua.NWAV_IMAGEN
                ? nwavOpciones.nwavImagen() : null);
        nwavPlantilla.setNwavFuente(nwavOpciones.nwavFuente().name());
        nwavPlantilla.setNwavTamanoFuente(nwavOpciones.nwavTamano());
        nwavPlantilla.setNwavColor(nwavOpciones.nwavColor().nwavHex());
        nwavPlantilla.setNwavOpacidad(nwavOpciones.nwavOpacidad());
        nwavPlantilla.setNwavRotacion(nwavOpciones.nwavRotacion());
        nwavPlantilla.setNwavEscala(nwavOpciones.nwavEscala());
        nwavPlantilla.setNwavPosicion(nwavOpciones.nwavPosicion());
        nwavPlantilla.setNwavFechaCreacion(LocalDateTime.now());
        return nwavRepositorio.save(nwavPlantilla);
    }

    @Transactional
    public void nwavEliminar(Long nwavId) {
        nwavRepositorio.deleteById(nwavId);
    }

    public static NwavOpcionesMarcaAgua nwavAOpciones(NwavMarcaAgua nwavPlantilla) {
        NwavFuente nwavFuente;
        try {
            nwavFuente = NwavFuente.valueOf(nwavPlantilla.getNwavFuente());
        } catch (RuntimeException nwavError) {
            nwavFuente = NwavFuente.NWAV_HELVETICA;
        }
        return new NwavOpcionesMarcaAgua(nwavPlantilla.getNwavTipo(), nwavPlantilla.getNwavTexto(),
                nwavPlantilla.getNwavImagen(), nwavFuente,
                nwavPlantilla.getNwavTamanoFuente() == null ? 60 : nwavPlantilla.getNwavTamanoFuente(),
                nwavDeHex(nwavPlantilla.getNwavColor()), nwavPlantilla.getNwavOpacidad(),
                nwavPlantilla.getNwavRotacion(), nwavPlantilla.getNwavEscala(), "", nwavPlantilla.getNwavPosicion());
    }

    static NwavColorRgb nwavDeHex(String nwavHex) {
        if (nwavHex == null || !nwavHex.matches("#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            return new NwavColorRgb(150, 150, 150);
        }
        String nwavDigitos = nwavHex.replace("#", "").toUpperCase(Locale.ROOT);
        return new NwavColorRgb(Integer.parseInt(nwavDigitos.substring(0, 2), 16),
                Integer.parseInt(nwavDigitos.substring(2, 4), 16), Integer.parseInt(nwavDigitos.substring(4, 6), 16));
    }
}
