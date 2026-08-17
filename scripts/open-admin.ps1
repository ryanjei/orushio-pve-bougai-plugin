param([Parameter(Mandatory=$true)][string]$Handoff)
$logPath=Join-Path (Split-Path (Split-Path (Split-Path $Handoff -Parent) -Parent) -Parent) 'logs\launcher.log'
function Write-LauncherLog([string]$message){Add-Content -LiteralPath $logPath -Value "[$([DateTime]::Now.ToString('s'))] $message" -Encoding utf8}
function Show-AdminError([string]$message){Write-LauncherLog $message;try{Add-Type -AssemblyName PresentationFramework;[System.Windows.MessageBox]::Show($message,'OPBP 管理画面',[System.Windows.MessageBoxButton]::OK,[System.Windows.MessageBoxImage]::Error)|Out-Null}catch{}}
$deadline=[DateTime]::UtcNow.AddSeconds(120)
while([DateTime]::UtcNow -lt $deadline){
    if(Test-Path -LiteralPath $Handoff){
        $url=Get-Content -LiteralPath $Handoff -Raw
        Remove-Item -LiteralPath $Handoff -Force -ErrorAction SilentlyContinue
        if($url -like 'http://127.0.0.1:8765/auth/bootstrap?token=*'){try{Start-Process $url -ErrorAction Stop;Write-LauncherLog '認証済み管理画面を開きました。';exit 0}catch{Show-AdminError '管理画面をブラウザで開けませんでした。既定ブラウザの設定を確認してください。';exit 1}}
        Show-AdminError '管理画面の認証情報を安全に取得できませんでした。サーバーを停止し、再度START_SERVER.batを実行してください。'
        exit 1
    }
    Start-Sleep -Milliseconds 250
}
Show-AdminError '管理画面の準備が120秒以内に完了しませんでした。.runtime\paper\logs\latest.logを確認してください。'
exit 1
