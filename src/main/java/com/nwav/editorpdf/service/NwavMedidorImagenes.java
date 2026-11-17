package com.nwav.editorpdf.service;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recorre el contenido de las paginas y anota, para cada imagen, el mayor tamano (en
 * puntos) con el que se dibuja. Con eso se calcula su resolucion efectiva (DPI).
 */
class NwavMedidorImagenes extends PDFStreamEngine {

    /** Ancho y alto maximos (puntos) por imagen; clave = flujo de la imagen. */
    private final Map<COSBase, float[]> nwavTamanos = new HashMap<>();

    NwavMedidorImagenes() {
        addOperator(new Concatenate(this));
        addOperator(new DrawObject(this));
        addOperator(new SetGraphicsStateParameters(this));
        addOperator(new Save(this));
        addOperator(new Restore(this));
        addOperator(new SetMatrix(this));
    }

    @Override
    protected void processOperator(Operator nwavOperador, List<COSBase> nwavOperandos) throws IOException {
        if ("Do".equals(nwavOperador.getName()) && !nwavOperandos.isEmpty()
                && nwavOperandos.get(0) instanceof COSName nwavNombre && getResources() != null) {
            PDXObject nwavObjeto = getResources().getXObject(nwavNombre);
            if (nwavObjeto instanceof PDImageXObject nwavImagen) {
                Matrix nwavCtm = getGraphicsState().getCurrentTransformationMatrix();
                float nwavAncho = Math.abs(nwavCtm.getScalingFactorX());
                float nwavAlto = Math.abs(nwavCtm.getScalingFactorY());
                nwavTamanos.merge(nwavImagen.getCOSObject(), new float[]{nwavAncho, nwavAlto},
                        (nwavA, nwavB) -> new float[]{Math.max(nwavA[0], nwavB[0]), Math.max(nwavA[1], nwavB[1])});
                return;
            }
        }
        super.processOperator(nwavOperador, nwavOperandos);
    }

    /** Tamano maximo dibujado (puntos) o null si la imagen no se dibuja en el contenido. */
    float[] nwavTamano(COSBase nwavImagen) {
        return nwavTamanos.get(nwavImagen);
    }
}
