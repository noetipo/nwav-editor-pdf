package com.nwav.editorpdf.service;

import com.nwav.editorpdf.exception.NwavEditorException;
import com.nwav.editorpdf.exception.NwavTraductorErrores;
import com.nwav.editorpdf.model.NwavRectangulo;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Matrix;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NwavFirmaDigitalService {
    private final NwavPdfDocumentoService nwavDocumentos;

    public record NwavCertificado(String nwavTitular, String nwavEmisor, Date nwavDesde, Date nwavHasta,
                                  boolean nwavAutofirmado) {
        public String nwavResumen() {
            SimpleDateFormat nwavFormato = new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT);
            return nwavTitular + " · válido del " + nwavFormato.format(nwavDesde) + " al " + nwavFormato.format(nwavHasta)
                    + (nwavAutofirmado ? " · autofirmado" : " · emitido por " + nwavEmisor);
        }
    }

    public Path nwavCrearCertificado(String nwavNombre, String nwavOrganizacion, String nwavCorreo, char[] nwavClave,
                                     int nwavAnios, Path nwavDestino) {
        if (nwavNombre == null || nwavNombre.isBlank()) {
            throw new NwavEditorException("Escribe el nombre del titular del certificado.");
        }
        if (nwavClave == null || nwavClave.length < 6) {
            throw new NwavEditorException("La contraseña del certificado debe tener al menos 6 caracteres.");
        }
        try {
            KeyPairGenerator nwavGenerador = KeyPairGenerator.getInstance("RSA");
            nwavGenerador.initialize(2048, new SecureRandom());
            KeyPair nwavPar = nwavGenerador.generateKeyPair();
            X500NameBuilder nwavNombreX500 = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, nwavNombre.strip());
            if (nwavOrganizacion != null && !nwavOrganizacion.isBlank()) {
                nwavNombreX500.addRDN(BCStyle.O, nwavOrganizacion.strip());
            }
            if (nwavCorreo != null && !nwavCorreo.isBlank()) {
                nwavNombreX500.addRDN(BCStyle.EmailAddress, nwavCorreo.strip());
            }
            X500Name nwavSujeto = nwavNombreX500.build();
            Instant nwavAhora = Instant.now();
            JcaX509v3CertificateBuilder nwavConstructor = new JcaX509v3CertificateBuilder(nwavSujeto,
                    new BigInteger(64, new SecureRandom()).abs(), Date.from(nwavAhora.minus(1, ChronoUnit.DAYS)),
                    Date.from(nwavAhora.plus(365L * Math.max(1, Math.min(10, nwavAnios)), ChronoUnit.DAYS)), nwavSujeto,
                    nwavPar.getPublic());
            nwavConstructor.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            nwavConstructor.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
            ContentSigner nwavFirmante = new JcaContentSignerBuilder("SHA256withRSA").build(nwavPar.getPrivate());
            X509Certificate nwavCertificado = new JcaX509CertificateConverter()
                    .getCertificate(nwavConstructor.build(nwavFirmante));
            KeyStore nwavAlmacen = KeyStore.getInstance("PKCS12");
            nwavAlmacen.load(null, null);
            nwavAlmacen.setKeyEntry("nwav-firma", nwavPar.getPrivate(), nwavClave, new Certificate[]{nwavCertificado});
            return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal -> {
                try (OutputStream nwavSalida = Files.newOutputStream(nwavTemporal)) {
                    nwavAlmacen.store(nwavSalida, nwavClave);
                } catch (GeneralSecurityException nwavError) {
                    throw new IOException(nwavError);
                }
            });
        } catch (GeneralSecurityException | OperatorCreationException | IOException nwavError) {
            throw new NwavEditorException("No se pudo crear el certificado: " + nwavError.getMessage(), nwavError);
        }
    }

    record NwavIdentidad(PrivateKey nwavClave, Certificate[] nwavCadena) {
        X509Certificate nwavCertificado() {
            return (X509Certificate) nwavCadena[0];
        }
    }

    NwavIdentidad nwavCargar(Path nwavP12, char[] nwavClave) {
        if (nwavP12 == null || !Files.isRegularFile(nwavP12)) {
            throw new NwavEditorException("Elige el archivo del certificado (.p12 o .pfx).");
        }
        try (InputStream nwavEntrada = Files.newInputStream(nwavP12)) {
            KeyStore nwavAlmacen = KeyStore.getInstance("PKCS12");
            nwavAlmacen.load(nwavEntrada, nwavClave);
            Enumeration<String> nwavAlias = nwavAlmacen.aliases();
            while (nwavAlias.hasMoreElements()) {
                String nwavNombre = nwavAlias.nextElement();
                if (nwavAlmacen.isKeyEntry(nwavNombre)) {
                    Certificate[] nwavCadena = nwavAlmacen.getCertificateChain(nwavNombre);
                    PrivateKey nwavPrivada = (PrivateKey) nwavAlmacen.getKey(nwavNombre, nwavClave);
                    if (nwavCadena != null && nwavCadena.length > 0 && nwavCadena[0] instanceof X509Certificate) {
                        return new NwavIdentidad(nwavPrivada, nwavCadena);
                    }
                }
            }
            throw new NwavEditorException("El archivo no contiene ninguna clave para firmar.");
        } catch (IOException nwavError) {
            if (nwavError.getCause() instanceof java.security.UnrecoverableKeyException
                    || String.valueOf(nwavError.getMessage()).toLowerCase(Locale.ROOT).contains("password")) {
                throw new NwavEditorException("La contraseña del certificado no es correcta.", nwavError);
            }
            throw new NwavEditorException("No se pudo leer el certificado: " + nwavError.getMessage(), nwavError);
        } catch (GeneralSecurityException nwavError) {
            throw new NwavEditorException("No se pudo leer el certificado: " + nwavError.getMessage(), nwavError);
        }
    }

    public NwavCertificado nwavInfoCertificado(Path nwavP12, char[] nwavClave) {
        return nwavInfo(nwavCargar(nwavP12, nwavClave).nwavCertificado());
    }

    private static NwavCertificado nwavInfo(X509Certificate nwavCertificado) {
        return new NwavCertificado(nwavNombreComun(nwavCertificado.getSubjectX500Principal().getName()),
                nwavNombreComun(nwavCertificado.getIssuerX500Principal().getName()), nwavCertificado.getNotBefore(),
                nwavCertificado.getNotAfter(), nwavCertificado.getSubjectX500Principal().equals(
                nwavCertificado.getIssuerX500Principal()));
    }

    static String nwavNombreComun(String nwavDn) {
        for (String nwavParte : nwavDn.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String nwavLimpia = nwavParte.strip();
            if (nwavLimpia.toUpperCase(Locale.ROOT).startsWith("CN=")) {
                return nwavLimpia.substring(3).replace("\"", "");
            }
        }
        return nwavDn;
    }

    public record NwavOpcionesFirma(String nwavMotivo, String nwavLugar, String nwavContacto, int nwavPagina,
                                    NwavRectangulo nwavZona, byte[] nwavImagen) {
    }

    public Path nwavFirmar(Path nwavEntrada, String nwavContrasenaPdf, Path nwavP12, char[] nwavClaveP12,
                           NwavOpcionesFirma nwavOpciones, Path nwavDestino) {
        NwavIdentidad nwavIdentidad = nwavCargar(nwavP12, nwavClaveP12);
        X509Certificate nwavCertificado = nwavIdentidad.nwavCertificado();
        try {
            nwavCertificado.checkValidity();
        } catch (GeneralSecurityException nwavError) {
            throw new NwavEditorException("El certificado ha caducado o aún no es válido ("
                    + nwavInfo(nwavCertificado).nwavResumen() + ").");
        }
        try (PDDocument nwavPd = nwavDocumentos.nwavCargarDocumento(nwavEntrada, nwavContrasenaPdf)) {
            if (nwavPd.isEncrypted()) {
                throw new NwavEditorException("Quita antes la contraseña del PDF (Seguridad → Desbloquear) para firmarlo.");
            }
            PDSignature nwavFirma = new PDSignature();
            nwavFirma.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            nwavFirma.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            nwavFirma.setName(nwavInfo(nwavCertificado).nwavTitular());
            nwavFirma.setReason(nwavVacio(nwavOpciones.nwavMotivo()));
            nwavFirma.setLocation(nwavVacio(nwavOpciones.nwavLugar()));
            nwavFirma.setContactInfo(nwavVacio(nwavOpciones.nwavContacto()));
            Calendar nwavFecha = Calendar.getInstance();
            nwavFirma.setSignDate(nwavFecha);
            SignatureInterface nwavFirmador = nwavContenido -> nwavCms(nwavContenido, nwavIdentidad);
            SignatureOptions nwavOpcionesPdfBox = new SignatureOptions();
            nwavOpcionesPdfBox.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
            try {
                if (nwavOpciones.nwavZona() != null) {
                    int nwavPagina = nwavOpciones.nwavPagina() < 0 || nwavOpciones.nwavPagina() >= nwavPd.getNumberOfPages()
                            ? nwavPd.getNumberOfPages() - 1 : nwavOpciones.nwavPagina();
                    PDPage nwavHoja = nwavPd.getPage(nwavPagina);
                    PDRectangle nwavCaja = NwavGeometriaPagina.nwavDe(nwavHoja).nwavRectanguloPdf(nwavOpciones.nwavZona());
                    nwavOpcionesPdfBox.setVisualSignature(nwavPlantillaVisible(nwavPd, nwavPagina, nwavCaja,
                            nwavInfo(nwavCertificado).nwavTitular(), nwavFecha, nwavOpciones));
                    nwavOpcionesPdfBox.setPage(nwavPagina);
                }
                nwavPd.addSignature(nwavFirma, nwavFirmador, nwavOpcionesPdfBox);
                return NwavEscrituraSegura.nwavEscribir(nwavDestino, nwavTemporal -> {
                    try (OutputStream nwavSalida = Files.newOutputStream(nwavTemporal)) {
                        nwavPd.saveIncremental(nwavSalida);
                    }
                });
            } finally {
                nwavOpcionesPdfBox.close();
            }
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    private static String nwavVacio(String nwavTexto) {
        return nwavTexto == null || nwavTexto.isBlank() ? null : nwavTexto.strip();
    }

    private static byte[] nwavCms(InputStream nwavContenido, NwavIdentidad nwavIdentidad) throws IOException {
        try {
            CMSSignedDataGenerator nwavGenerador = new CMSSignedDataGenerator();
            ContentSigner nwavFirmante = new JcaContentSignerBuilder("SHA256withRSA").build(nwavIdentidad.nwavClave());
            nwavGenerador.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().build()).build(nwavFirmante, nwavIdentidad.nwavCertificado()));
            nwavGenerador.addCertificates(new JcaCertStore(List.of(nwavIdentidad.nwavCadena())));
            CMSTypedData nwavDatos = new NwavDatosCms(nwavContenido.readAllBytes());
            return nwavGenerador.generate(nwavDatos, false).getEncoded();
        } catch (GeneralSecurityException | OperatorCreationException | CMSException nwavError) {
            throw new IOException("No se pudo generar la firma: " + nwavError.getMessage(), nwavError);
        }
    }

    private record NwavDatosCms(byte[] nwavBytes) implements CMSTypedData {
        @Override
        public ASN1ObjectIdentifier getContentType() {
            return CMSObjectIdentifiers.data;
        }

        @Override
        public void write(OutputStream nwavSalida) throws IOException {
            nwavSalida.write(nwavBytes);
        }

        @Override
        public Object getContent() {
            return nwavBytes;
        }
    }

    private static InputStream nwavPlantillaVisible(PDDocument nwavOrigen, int nwavPagina, PDRectangle nwavCaja,
                                                    String nwavTitular, Calendar nwavFecha, NwavOpcionesFirma nwavOpciones)
            throws IOException {
        PDPage nwavHojaOrigen = nwavOrigen.getPage(nwavPagina);
        try (PDDocument nwavPlantilla = new PDDocument()) {
            PDPage nwavHoja = new PDPage(nwavHojaOrigen.getMediaBox());
            nwavHoja.setRotation(nwavHojaOrigen.getRotation());
            nwavPlantilla.addPage(nwavHoja);
            PDAcroForm nwavFormulario = new PDAcroForm(nwavPlantilla);
            nwavPlantilla.getDocumentCatalog().setAcroForm(nwavFormulario);
            PDSignatureField nwavCampo = new PDSignatureField(nwavFormulario);
            PDAnnotationWidget nwavWidget = nwavCampo.getWidgets().get(0);
            nwavFormulario.setSignaturesExist(true);
            nwavFormulario.setAppendOnly(true);
            nwavFormulario.getCOSObject().setDirect(true);
            nwavFormulario.getFields().add(nwavCampo);
            nwavWidget.setRectangle(nwavCaja);
            nwavWidget.setPage(nwavHoja);
            nwavWidget.setPrinted(true);

            int nwavRotacion = NwavDocumentoPdf.nwavRotacionNormalizada(nwavHojaOrigen);
            boolean nwavGirada = nwavRotacion == 90 || nwavRotacion == 270;
            float nwavAncho = nwavGirada ? nwavCaja.getHeight() : nwavCaja.getWidth();
            float nwavAlto = nwavGirada ? nwavCaja.getWidth() : nwavCaja.getHeight();
            PDStream nwavFlujoAp = new PDStream(nwavPlantilla);
            PDFormXObject nwavForma = new PDFormXObject(nwavFlujoAp);
            nwavForma.setResources(new PDResources());
            nwavForma.setFormType(1);
            nwavForma.setBBox(new PDRectangle(nwavCaja.getWidth(), nwavCaja.getHeight()));
            Matrix nwavMatriz = switch (nwavRotacion) {
                case 90 -> new Matrix(0, 1, -1, 0, nwavCaja.getWidth(), 0);
                case 180 -> new Matrix(-1, 0, 0, -1, nwavCaja.getWidth(), nwavCaja.getHeight());
                case 270 -> new Matrix(0, -1, 1, 0, 0, nwavCaja.getHeight());
                default -> new Matrix();
            };
            PDAppearanceStream nwavApariencia = new PDAppearanceStream(nwavForma.getCOSObject());
            nwavApariencia.setMatrix(nwavMatriz.createAffineTransform());
            PDAppearanceDictionary nwavDiccionario = new PDAppearanceDictionary();
            nwavDiccionario.getCOSObject().setDirect(true);
            nwavDiccionario.setNormalAppearance(nwavApariencia);
            nwavWidget.setAppearance(nwavDiccionario);

            try (PDPageContentStream nwavFlujo = new PDPageContentStream(nwavPlantilla, nwavApariencia)) {
                nwavFlujo.setNonStrokingColor(0.97f, 0.98f, 1f);
                nwavFlujo.addRect(0, 0, nwavAncho, nwavAlto);
                nwavFlujo.fill();
                nwavFlujo.setStrokingColor(0.2f, 0.35f, 0.65f);
                nwavFlujo.setLineWidth(0.8f);
                nwavFlujo.addRect(0.4f, 0.4f, nwavAncho - 0.8f, nwavAlto - 0.8f);
                nwavFlujo.stroke();
                float nwavTexto = nwavAncho;
                if (nwavOpciones.nwavImagen() != null && nwavOpciones.nwavImagen().length > 0) {
                    PDImageXObject nwavImagen = PDImageXObject.createFromByteArray(nwavPlantilla, nwavOpciones.nwavImagen(),
                            "firma");
                    float nwavMaxAncho = nwavAncho * 0.45f;
                    float nwavEscala = Math.min(nwavMaxAncho / nwavImagen.getWidth(), (nwavAlto - 6) / nwavImagen.getHeight());
                    float nwavAnchoImg = nwavImagen.getWidth() * nwavEscala;
                    float nwavAltoImg = nwavImagen.getHeight() * nwavEscala;
                    nwavFlujo.drawImage(nwavImagen, 4, (nwavAlto - nwavAltoImg) / 2, nwavAnchoImg, nwavAltoImg);
                    nwavTexto = nwavAncho - nwavAnchoImg - 10;
                }
                PDType1Font nwavFuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font nwavNegrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                List<String> nwavLineas = new ArrayList<>();
                nwavLineas.add("Firmado digitalmente por");
                nwavLineas.add(nwavTitular);
                nwavLineas.add("Fecha: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).format(nwavFecha.getTime()));
                if (nwavOpciones.nwavMotivo() != null && !nwavOpciones.nwavMotivo().isBlank()) {
                    nwavLineas.add("Motivo: " + nwavOpciones.nwavMotivo().strip());
                }
                if (nwavOpciones.nwavLugar() != null && !nwavOpciones.nwavLugar().isBlank()) {
                    nwavLineas.add("Lugar: " + nwavOpciones.nwavLugar().strip());
                }
                float nwavTamano = Math.max(4.5f, Math.min(9, (nwavAlto - 6) / (nwavLineas.size() * 1.25f)));
                float nwavX = nwavAncho - nwavTexto + 4;
                float nwavY = nwavAlto - 4 - nwavTamano;
                nwavFlujo.setNonStrokingColor(0.1f, 0.15f, 0.3f);
                for (int nwavIndice = 0; nwavIndice < nwavLineas.size(); nwavIndice++) {
                    PDType1Font nwavUsada = nwavIndice == 1 ? nwavNegrita : nwavFuente;
                    String nwavLinea = nwavSoloWinAnsi(nwavUsada, nwavLineas.get(nwavIndice));
                    while (nwavLinea.length() > 1 && nwavUsada.getStringWidth(nwavLinea) / 1000 * nwavTamano > nwavTexto - 6) {
                        nwavLinea = nwavLinea.substring(0, nwavLinea.length() - 1);
                    }
                    nwavFlujo.beginText();
                    nwavFlujo.setFont(nwavUsada, nwavTamano);
                    nwavFlujo.newLineAtOffset(nwavX, nwavY);
                    nwavFlujo.showText(nwavLinea);
                    nwavFlujo.endText();
                    nwavY -= nwavTamano * 1.25f;
                }
            }
            java.io.ByteArrayOutputStream nwavBytes = new java.io.ByteArrayOutputStream();
            nwavPlantilla.save(nwavBytes);
            return new ByteArrayInputStream(nwavBytes.toByteArray());
        }
    }

    private static String nwavSoloWinAnsi(PDType1Font nwavFuente, String nwavTexto) {
        StringBuilder nwavValido = new StringBuilder();
        for (char nwavCaracter : nwavTexto.toCharArray()) {
            try {
                nwavFuente.encode(String.valueOf(nwavCaracter));
                nwavValido.append(nwavCaracter);
            } catch (IOException | IllegalArgumentException nwavError) {
                nwavValido.append('?');
            }
        }
        return nwavValido.toString();
    }

    public record NwavFirmaVerificada(String nwavFirmante, Date nwavFecha, String nwavMotivo, boolean nwavIntegra,
                                      boolean nwavCubreTodo, boolean nwavCertificadoVigente, boolean nwavAutofirmado,
                                      String nwavDetalle) {
        public boolean nwavValida() {
            return nwavIntegra && nwavCertificadoVigente;
        }

        public String nwavResumen() {
            SimpleDateFormat nwavFormato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT);
            StringBuilder nwavTexto = new StringBuilder(nwavFirmante == null ? "Firmante desconocido" : nwavFirmante);
            if (nwavFecha != null) {
                nwavTexto.append(" (").append(nwavFormato.format(nwavFecha)).append(')');
            }
            nwavTexto.append(": ");
            if (!nwavIntegra) {
                nwavTexto.append("NO VÁLIDA, el documento se modificó después de firmarlo o la firma está dañada");
            } else {
                nwavTexto.append("íntegra");
                nwavTexto.append(nwavCubreTodo ? ", cubre todo el documento" : ", pero hay cambios añadidos después de firmar");
                nwavTexto.append(nwavCertificadoVigente ? "" : ", certificado no vigente en la fecha de firma");
                nwavTexto.append(nwavAutofirmado ? ", certificado autofirmado (no verificado por una autoridad)"
                        : ", certificado no comprobado contra autoridades de confianza");
            }
            return nwavTexto.toString();
        }
    }

    public List<NwavFirmaVerificada> nwavVerificar(Path nwavEntrada, String nwavContrasena) {
        byte[] nwavArchivo;
        try {
            nwavArchivo = Files.readAllBytes(nwavEntrada);
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
        try (PDDocument nwavPd = nwavContrasena == null ? Loader.loadPDF(nwavArchivo) : Loader.loadPDF(nwavArchivo, nwavContrasena)) {
            List<NwavFirmaVerificada> nwavResultado = new ArrayList<>();
            for (PDSignature nwavFirma : nwavPd.getSignatureDictionaries()) {
                nwavResultado.add(nwavVerificarUna(nwavFirma, nwavArchivo));
            }
            return nwavResultado;
        } catch (IOException nwavError) {
            throw NwavTraductorErrores.nwavTraducir(nwavError, String.valueOf(nwavEntrada.getFileName()));
        }
    }

    private static NwavFirmaVerificada nwavVerificarUna(PDSignature nwavFirma, byte[] nwavArchivo) {
        Date nwavFecha = nwavFirma.getSignDate() == null ? null : nwavFirma.getSignDate().getTime();
        int[] nwavRango = nwavFirma.getByteRange();
        boolean nwavCubreTodo = nwavRango != null && nwavRango.length == 4
                && (long) nwavRango[2] + nwavRango[3] == nwavArchivo.length;
        try {
            byte[] nwavContenido = nwavFirma.getContents(nwavArchivo);
            byte[] nwavFirmado = nwavFirma.getSignedContent(nwavArchivo);
            CMSSignedData nwavCms = new CMSSignedData(new CMSProcessableByteArray(nwavFirmado), nwavContenido);
            Store<X509CertificateHolder> nwavCertificados = nwavCms.getCertificates();
            Collection<SignerInformation> nwavFirmantes = nwavCms.getSignerInfos().getSigners();
            if (nwavFirmantes.isEmpty()) {
                return new NwavFirmaVerificada(nwavFirma.getName(), nwavFecha, nwavFirma.getReason(), false, nwavCubreTodo,
                        false, false, "La firma no contiene firmantes.");
            }
            SignerInformation nwavFirmante = nwavFirmantes.iterator().next();
            @SuppressWarnings("unchecked")
            Collection<X509CertificateHolder> nwavCoinciden = nwavCertificados.getMatches(nwavFirmante.getSID());
            if (nwavCoinciden.isEmpty()) {
                return new NwavFirmaVerificada(nwavFirma.getName(), nwavFecha, nwavFirma.getReason(), false, nwavCubreTodo,
                        false, false, "La firma no incluye el certificado del firmante.");
            }
            X509Certificate nwavCertificado = new JcaX509CertificateConverter().getCertificate(nwavCoinciden.iterator().next());
            boolean nwavIntegra = nwavFirmante.verify(new JcaSimpleSignerInfoVerifierBuilder().build(nwavCertificado));
            boolean nwavVigente;
            try {
                nwavCertificado.checkValidity(nwavFecha != null ? nwavFecha : new Date());
                nwavVigente = true;
            } catch (GeneralSecurityException nwavError) {
                nwavVigente = false;
            }
            NwavCertificado nwavInfo = nwavInfo(nwavCertificado);
            return new NwavFirmaVerificada(nwavInfo.nwavTitular(), nwavFecha, nwavFirma.getReason(), nwavIntegra,
                    nwavCubreTodo, nwavVigente, nwavInfo.nwavAutofirmado(), nwavInfo.nwavResumen());
        } catch (CMSException | GeneralSecurityException | OperatorCreationException | IOException
                 | RuntimeException nwavError) {
            return new NwavFirmaVerificada(nwavFirma.getName(), nwavFecha, nwavFirma.getReason(), false, nwavCubreTodo,
                    false, false, "No se pudo comprobar: " + nwavError.getMessage());
        }
    }
}
