@echo off
rem Igual que "NWAV Editor PDF.cmd" pero con consola: muestra los mensajes de error.
rem Usalo si la aplicacion no abre y comparte el texto que aparezca.
setlocal
set "NWAV_DIR=%~dp0"
chcp 65001 >nul
"%NWAV_DIR%runtime\bin\java.exe" -Xmx2g -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "%NWAV_DIR%app\lib\*" com.nwav.editorpdf.NwavLauncher %*
echo.
echo La aplicacion se ha cerrado. Pulsa una tecla para salir.
pause >nul
