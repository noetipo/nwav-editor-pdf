package com.nwav.editorpdf;

import javafx.fxml.FXML;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NwavVistasFxmlTest {
    private static final Path NWAV_CARPETA_VISTAS = Path.of("src/main/resources/view");

    @Test
    void nwavCadaFxmlEncajaConSuControlador() throws Exception {
        List<String> nwavProblemas = new ArrayList<>();
        List<Path> nwavVistas;
        try (Stream<Path> nwavArchivos = Files.list(NWAV_CARPETA_VISTAS)) {
            nwavVistas = nwavArchivos.filter(nwavRuta -> nwavRuta.toString().endsWith(".fxml")).sorted().toList();
        }
        assertThat(nwavVistas).hasSizeGreaterThanOrEqualTo(9);

        for (Path nwavVista : nwavVistas) {
            String nwavNombre = nwavVista.getFileName().toString();
            if (!nwavNombre.startsWith("nwav-")) {
                nwavProblemas.add(nwavNombre + ": el archivo no lleva prefijo nwav-");
            }
            Document nwavXml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(nwavVista.toFile());
            Element nwavRaiz = nwavXml.getDocumentElement();
            String nwavClase = nwavRaiz.getAttribute("fx:controller");
            if (nwavClase.isEmpty()) {
                nwavProblemas.add(nwavNombre + ": no declara fx:controller");
                continue;
            }
            Class<?> nwavControlador = Class.forName(nwavClase);
            nwavRevisar(nwavRaiz, nwavNombre, nwavControlador, nwavProblemas);
        }
        assertThat(nwavProblemas).isEmpty();
    }

    private static void nwavRevisar(Element nwavElemento, String nwavVista, Class<?> nwavControlador,
                                    List<String> nwavProblemas) {
        NamedNodeMap nwavAtributos = nwavElemento.getAttributes();
        for (int nwavIndice = 0; nwavIndice < nwavAtributos.getLength(); nwavIndice++) {
            Node nwavAtributo = nwavAtributos.item(nwavIndice);
            String nwavClave = nwavAtributo.getNodeName();
            String nwavValor = nwavAtributo.getNodeValue();
            if (nwavClave.equals("fx:id")) {
                if (!nwavValor.startsWith("nwav")) {
                    nwavProblemas.add(nwavVista + ": fx:id sin prefijo: " + nwavValor);
                }
                if (nwavCampo(nwavControlador, nwavValor) == null) {
                    nwavProblemas.add(nwavVista + ": falta el campo @FXML «" + nwavValor + "» en "
                            + nwavControlador.getSimpleName());
                }
            } else if (nwavClave.startsWith("on") && nwavValor.startsWith("#")) {
                String nwavMetodo = nwavValor.substring(1);
                if (!nwavMetodo.startsWith("nwav")) {
                    nwavProblemas.add(nwavVista + ": manejador sin prefijo: " + nwavMetodo);
                }
                if (!nwavTieneMetodo(nwavControlador, nwavMetodo)) {
                    nwavProblemas.add(nwavVista + ": falta el método «" + nwavMetodo + "» en "
                            + nwavControlador.getSimpleName());
                }
            }
        }
        NodeList nwavHijos = nwavElemento.getChildNodes();
        for (int nwavIndice = 0; nwavIndice < nwavHijos.getLength(); nwavIndice++) {
            if (nwavHijos.item(nwavIndice) instanceof Element nwavHijo) {
                nwavRevisar(nwavHijo, nwavVista, nwavControlador, nwavProblemas);
            }
        }
    }

    private static Field nwavCampo(Class<?> nwavClase, String nwavNombre) {
        for (Class<?> nwavActual = nwavClase; nwavActual != null; nwavActual = nwavActual.getSuperclass()) {
            for (Field nwavCampo : nwavActual.getDeclaredFields()) {
                if (nwavCampo.getName().equals(nwavNombre)
                        && (nwavCampo.isAnnotationPresent(FXML.class)
                        || java.lang.reflect.Modifier.isPublic(nwavCampo.getModifiers()))) {
                    return nwavCampo;
                }
            }
        }
        return null;
    }

    private static boolean nwavTieneMetodo(Class<?> nwavClase, String nwavNombre) {
        for (Class<?> nwavActual = nwavClase; nwavActual != null; nwavActual = nwavActual.getSuperclass()) {
            boolean nwavEncontrado = Arrays.stream(nwavActual.getDeclaredMethods())
                    .map(Method::getName).anyMatch(nwavNombre::equals);
            if (nwavEncontrado) {
                return true;
            }
        }
        return false;
    }
}
