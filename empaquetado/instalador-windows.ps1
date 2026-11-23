param([switch]$SinPruebas)
$ErrorActionPreference = 'Stop'
$Raiz = Split-Path -Parent $PSScriptRoot
Set-Location $Raiz

if (-not $env:JAVA_HOME) { throw 'Configura JAVA_HOME con un JDK 21 (necesario para jpackage).' }
$JPackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (-not (Test-Path $JPackage)) { throw "No se encontro jpackage en $JPackage. Instala un JDK 21 completo." }

$Version = (& .\mvnw.cmd -q -B help:evaluate '-Dexpression=project.version' -DforceStdout).Trim()
$VersionApp = ($Version -replace '[^0-9.].*$', '')

if (-not $SinPruebas) {
  Write-Host '==> Pruebas'
  & .\mvnw.cmd -B test
  if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas.' }
}

Write-Host '==> Compilando'
& .\mvnw.cmd -q -B -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion.' }
$Entrada = Join-Path $Raiz 'target\jpackage\lib'
if (Test-Path (Join-Path $Raiz 'target\jpackage')) { Remove-Item -Recurse -Force (Join-Path $Raiz 'target\jpackage') }
New-Item -ItemType Directory -Force -Path $Entrada | Out-Null
& .\mvnw.cmd -q -B dependency:copy-dependencies -DincludeScope=runtime -DexcludeArtifactIds=lombok "-DoutputDirectory=$Entrada"
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron copiar las dependencias.' }
Copy-Item "target\nwav-editor-pdf-$Version.jar" $Entrada

$Destino = Join-Path $Raiz 'target\instalador'
if (Test-Path $Destino) { Remove-Item -Recurse -Force $Destino }
$Comunes = @(
  '--name', 'Editor PDF',
  '--app-version', $VersionApp,
  '--vendor', 'Editor PDF',
  '--description', 'Editor de PDF local y sin conexion',
  '--input', $Entrada,
  '--main-jar', "nwav-editor-pdf-$Version.jar",
  '--main-class', 'com.nwav.editorpdf.NwavLauncher',
  '--icon', 'empaquetado\nwav-icono.ico',
  '--java-options', '-Xmx2g',
  '--java-options', '-Dfile.encoding=UTF-8',
  '--dest', $Destino
)

$TieneWix = (Get-Command candle.exe -ErrorAction SilentlyContinue) -or (Get-Command wix.exe -ErrorAction SilentlyContinue)
if ($TieneWix) {
  Write-Host '==> Creando el instalador .exe'
  & $JPackage --type exe @Comunes `
    --win-per-user-install --win-dir-chooser --win-menu --win-menu-group 'Editor PDF' --win-shortcut `
    --win-shortcut-prompt --win-upgrade-uuid '6b1f6f0e-2f7b-4e5b-9a43-1d2c8c0f7a51' `
    --file-associations 'empaquetado\windows\asociacion-pdf.properties'
} else {
  Write-Host '==> WiX no esta instalado: se crea la carpeta de la aplicacion (app-image)'
  & $JPackage --type app-image @Comunes
  Compress-Archive -Path (Join-Path $Destino 'Editor PDF') -DestinationPath (Join-Path $Destino "Editor-PDF-$VersionApp-app.zip")
}
if ($LASTEXITCODE -ne 0) { throw 'jpackage fallo.' }
Write-Host "Listo: $Destino"
Get-ChildItem $Destino
