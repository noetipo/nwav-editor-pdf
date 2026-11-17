@echo off
setlocal
set "NWAV_DIR=%~dp0"
start "" "%NWAV_DIR%runtime\bin\javaw.exe" -Xmx2g -Dfile.encoding=UTF-8 -Dsun.java2d.d3d=true -cp "%NWAV_DIR%app\lib\*" com.nwav.editorpdf.NwavLauncher %*
