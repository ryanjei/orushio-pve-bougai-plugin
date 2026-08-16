@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-server.ps1"
if errorlevel 1 (
  echo.
  echo 起動に失敗しました。上に表示された日本語メッセージを確認してください。
  echo このウィンドウは確認のため閉じません。
  pause
)
endlocal
