# nwav-editor-pdf

Editor de PDF de escritorio, local y sin conexión (Java 17, Spring Boot, JavaFX, PDFBox 3, SQLite).
Pensado sobre todo para Windows.

## Herramientas

- **Editor:** texto, imágenes, formas, notas, resaltados, firmas, formularios y deshacer/rehacer.
- **Organizar:** unir, dividir, eliminar, extraer, ordenar, rotar y recortar.
- **Optimizar:** comprimir, OCR (Tesseract) y reparar.
- **Convertir a PDF:** imágenes, escaneos, Office (con LibreOffice) y HTML o páginas web.
- **Convertir desde PDF:** imagen, extraer imágenes, Word, Excel, PowerPoint, Markdown/texto y PDF/A.
- **Editar:** crear formularios, marca de agua (también en mosaico), numerar y comparar PDF.
- **Seguridad:** proteger o quitar contraseña, firma digital (.p12) y verificación, redacción real y
  quitar metadatos.

Los PDF nunca se guardan en la base de datos, y el original no se sobrescribe sin confirmación.

## Windows

- **Portable:** descomprime `NWAV-Editor-PDF-<versión>-windows-x64.zip` y abre `NWAV Editor PDF.cmd`
  (lleva Java incluido). Se genera desde macOS o Linux con `empaquetado/construir-windows.sh`.
- **Instalador .exe:** en Windows, con JDK 21 y WiX 3.14:
  `powershell -ExecutionPolicy Bypass -File empaquetado\instalador-windows.ps1`
- **GitHub Actions** (`.github/workflows/windows.yml`) pasa las pruebas en Windows y publica el
  instalador como artefacto.

## Desarrollo

```bash
./mvnw test                 # pruebas
./mvnw javafx:run           # ejecutar
```

Opcionales: LibreOffice para «Office a PDF». En macOS y Linux, Tesseract (`brew install tesseract`)
para el OCR; en Windows el motor va incluido.
