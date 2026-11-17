package com.nwav.editorpdf.service;

import com.nwav.editorpdf.model.NwavInfoDocumento;
import com.nwav.editorpdf.model.NwavMetadatos;
import com.nwav.editorpdf.model.NwavTipoOperacion;
import com.nwav.editorpdf.service.comando.NwavComando;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;

@Service
public class NwavMetadatosService {
    public NwavInfoDocumento nwavLeer(NwavDocumentoPdf nwavDocumento) {
        synchronized (nwavDocumento) {
            nwavDocumento.nwavVerificarAbierto();
            PDDocument nwavPd = nwavDocumento.nwavPdDocument();
            PDDocumentInformation nwavInfo = nwavPd.getDocumentInformation();
            NwavMetadatos nwavMetadatos = new NwavMetadatos(nwavInfo.getTitle(), nwavInfo.getAuthor(),
                    nwavInfo.getSubject(), nwavInfo.getKeywords(), nwavInfo.getCreator(), nwavInfo.getProducer(),
                    nwavFecha(nwavInfo.getCreationDate()), nwavFecha(nwavInfo.getModificationDate()));
            long nwavTamano;
            try {
                nwavTamano = Files.size(nwavDocumento.getNwavRuta());
            } catch (IOException nwavError) {
                nwavTamano = -1;
            }
            PDAcroForm nwavFormulario = nwavPd.getDocumentCatalog().getAcroForm();
            int nwavCampos = 0;
            if (nwavFormulario != null) {
                for (var nwavCampo : nwavFormulario.getFieldTree()) {
                    nwavCampos += nwavCampo instanceof org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField ? 1 : 0;
                }
            }
            NwavCambioProteccion nwavCambio = nwavDocumento.getNwavProteccionPendiente();
            return new NwavInfoDocumento(nwavMetadatos, nwavDocumento.getNwavNumeroPaginas(),
                    String.valueOf(nwavPd.getVersion()), nwavTamano, nwavDocumento.isNwavCifrado(),
                    nwavCambio == null ? null : nwavCambio.nwavDescripcion(), nwavCampos);
        }
    }

    public NwavComando nwavComandoEditar(NwavDocumentoPdf nwavDocumento, NwavMetadatos nwavNuevos) {
        return new NwavComando() {
            private COSDictionary nwavAntes;
            private COSDictionary nwavDespues;

            @Override
            public void nwavEjecutar() {
                synchronized (nwavDocumento) {
                    nwavDocumento.nwavVerificarAbierto();
                    PDDocument nwavPd = nwavDocumento.nwavPdDocument();
                    if (nwavDespues != null) {
                        nwavPd.setDocumentInformation(new PDDocumentInformation(new COSDictionary(nwavDespues)));
                        return;
                    }
                    nwavAntes = new COSDictionary(nwavPd.getDocumentInformation().getCOSObject());
                    PDDocumentInformation nwavInfo = new PDDocumentInformation(new COSDictionary(nwavAntes));
                    nwavInfo.setTitle(nwavVacioANulo(nwavNuevos.nwavTitulo()));
                    nwavInfo.setAuthor(nwavVacioANulo(nwavNuevos.nwavAutor()));
                    nwavInfo.setSubject(nwavVacioANulo(nwavNuevos.nwavAsunto()));
                    nwavInfo.setKeywords(nwavVacioANulo(nwavNuevos.nwavPalabrasClave()));
                    nwavInfo.setModificationDate(new GregorianCalendar());
                    if (nwavInfo.getCOSObject().getItem(COSName.PRODUCER) == null) {
                        nwavInfo.setProducer("NWAV Editor PDF");
                    }
                    nwavPd.setDocumentInformation(nwavInfo);
                    nwavDespues = new COSDictionary(nwavInfo.getCOSObject());
                }
            }

            @Override
            public void nwavDeshacer() {
                synchronized (nwavDocumento) {
                    nwavDocumento.nwavPdDocument().setDocumentInformation(
                            new PDDocumentInformation(new COSDictionary(nwavAntes)));
                }
            }

            @Override
            public NwavTipoOperacion nwavTipo() {
                return NwavTipoOperacion.NWAV_METADATOS;
            }

            @Override
            public String nwavDescripcion() {
                return "Editar propiedades del documento";
            }

            @Override
            public boolean nwavCambiaEstructura() {
                return false;
            }
        };
    }

    private static String nwavVacioANulo(String nwavTexto) {
        return nwavTexto == null || nwavTexto.isBlank() ? null : nwavTexto.strip();
    }

    private static LocalDateTime nwavFecha(Calendar nwavCalendario) {
        return nwavCalendario == null ? null
                : LocalDateTime.ofInstant(nwavCalendario.toInstant(), ZoneId.systemDefault());
    }
}
