package com.nwav.editorpdf.service;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;

public final class NwavInstantaneaPagina {
    private final PDPage nwavPagina;
    private final COSBase nwavContenido;
    private final COSArray nwavAnotaciones;

    NwavInstantaneaPagina(PDPage nwavPagina) {
        this.nwavPagina = nwavPagina;
        COSDictionary nwavDiccionario = nwavPagina.getCOSObject();
        COSBase nwavContenidoActual = nwavDiccionario.getDictionaryObject(COSName.CONTENTS);
        this.nwavContenido = nwavContenidoActual instanceof COSArray nwavArreglo
                ? nwavCopiar(nwavArreglo) : nwavDiccionario.getItem(COSName.CONTENTS);
        COSArray nwavAnotacionesActuales = nwavDiccionario.getCOSArray(COSName.ANNOTS);
        this.nwavAnotaciones = nwavAnotacionesActuales == null ? null : nwavCopiar(nwavAnotacionesActuales);
    }

    PDPage nwavPagina() {
        return nwavPagina;
    }

    void nwavRestaurar() {
        COSDictionary nwavDiccionario = nwavPagina.getCOSObject();
        if (nwavContenido == null) {
            nwavDiccionario.removeItem(COSName.CONTENTS);
        } else {
            nwavDiccionario.setItem(COSName.CONTENTS,
                    nwavContenido instanceof COSArray nwavArreglo ? nwavCopiar(nwavArreglo) : nwavContenido);
        }
        if (nwavAnotaciones == null) {
            nwavDiccionario.removeItem(COSName.ANNOTS);
        } else {
            nwavDiccionario.setItem(COSName.ANNOTS, nwavCopiar(nwavAnotaciones));
        }
    }

    private static COSArray nwavCopiar(COSArray nwavOriginal) {
        COSArray nwavCopia = new COSArray();
        for (int nwavIndice = 0; nwavIndice < nwavOriginal.size(); nwavIndice++) {
            nwavCopia.add(nwavOriginal.get(nwavIndice));
        }
        return nwavCopia;
    }
}
