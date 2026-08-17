@echo off
setlocal
cd /d "%~dp0"
set "ORUSHIO_SCRIPT=%~dp0scripts\start-server.ps1"
set "ORUSHIO_POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if not exist "%ORUSHIO_SCRIPT%" goto failure
if not exist "%ORUSHIO_POWERSHELL%" goto failure
"%ORUSHIO_POWERSHELL%" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%ORUSHIO_SCRIPT%"
set "ORUSHIO_EXIT=%ERRORLEVEL%"
if not "%ORUSHIO_EXIT%"=="0" goto failure
endlocal
exit /b 0

:failure
echo Launcher failed. Review the Japanese error shown above.
pause >nul
endlocal
exit /b 1
