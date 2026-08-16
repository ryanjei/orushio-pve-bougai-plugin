@echo off
chcp 65001 >nul
setlocal
title Orushio PVE サーバー起動
echo ==================================================
echo Orushio PVE サーバーを起動しています...
echo ==================================================
echo [起動準備] 起動ファイルを確認しています。
cd /d "%~dp0"

set "ORUSHIO_SCRIPT=%~dp0scripts\start-server.ps1"
set "ORUSHIO_POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"

if not exist "%ORUSHIO_SCRIPT%" (
  echo.
  echo エラー: 起動処理ファイル scripts\start-server.ps1 が見つかりません。
  echo RepositoryをZIPから展開し直すか、ファイル一式が揃っているか確認してください。
  goto :failure
)

if not exist "%ORUSHIO_POWERSHELL%" (
  echo.
  echo エラー: Windows PowerShellを確認できません。
  echo WindowsのシステムファイルとPowerShellが利用可能か確認してください。
  goto :failure
)

echo [起動準備] Windows PowerShellを開始します。
"%ORUSHIO_POWERSHELL%" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%ORUSHIO_SCRIPT%"
set "ORUSHIO_EXIT=%ERRORLEVEL%"
if not "%ORUSHIO_EXIT%"=="0" (
  echo.
  echo エラー: サーバー起動処理が失敗しました。終了コード: %ORUSHIO_EXIT%
  echo 上に表示された日本語メッセージと .runtime\paper\logs\launcher.log を確認してください。
  goto :failure
)

echo.
echo Orushio PVEサーバーの処理が正常に完了しました。
echo 何かキーを押すとこのウィンドウを閉じます。
pause >nul
endlocal
exit /b 0

:failure
echo.
echo このウィンドウは確認のため閉じません。何かキーを押すと閉じます。
pause >nul
endlocal
exit /b 1
