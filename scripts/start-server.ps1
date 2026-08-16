param([switch]$PrepareOnly,[switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.UTF8Encoding]::new()
$repository = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtime = Join-Path $repository '.runtime\paper'
$paperJar = Join-Path $runtime 'paper.jar'
$plugins = Join-Path $runtime 'plugins'
$pluginJar = Join-Path $plugins 'orushio-pve-bougai-plugin.jar'
$handoff = Join-Path $plugins 'OrushioPveBougai\admin-bootstrap.url'
$paperUrl = 'https://fill-data.papermc.io/v1/objects/5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar'
$paperSha256 = '5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba'
$launcherLog = Join-Path $runtime 'logs\launcher.log'

function Stop-WithMessage([string]$message) { throw $message }
function Show-Step([string]$message) { Write-Host "[進捗] $message" -ForegroundColor Cyan }
function Get-Sha256([string]$path) { $stream=[IO.File]::OpenRead($path);try{$sha=[Security.Cryptography.SHA256]::Create();try{return ([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}}finally{$stream.Dispose()} }
function Find-Java21 {
    $candidates = [Collections.Generic.List[string]]::new()
    if ($env:JAVA_HOME) { $null=$candidates.Add((Join-Path $env:JAVA_HOME 'bin\java.exe')) }
    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command) { $null=$candidates.Add($command.Source) }
    @('C:\Program Files\Amazon Corretto','C:\Program Files\Eclipse Adoptium','C:\Program Files\Java',(Join-Path $env:USERPROFILE '.jdks')) | ForEach-Object {
        if (Test-Path -LiteralPath $_) { Get-ChildItem -LiteralPath $_ -Directory -ErrorAction SilentlyContinue | Where-Object Name -Match '21' | ForEach-Object { $null=$candidates.Add((Join-Path $_.FullName 'bin\java.exe')) } }
    }
    foreach ($candidate in $candidates | Select-Object -Unique) { if (Test-Path -LiteralPath $candidate) { $info=[Diagnostics.ProcessStartInfo]::new();$info.FileName=$candidate;$info.Arguments='-version';$info.UseShellExecute=$false;$info.RedirectStandardError=$true;$info.RedirectStandardOutput=$true;$info.CreateNoWindow=$true;$process=[Diagnostics.Process]::Start($info);$version=$process.StandardError.ReadToEnd()+$process.StandardOutput.ReadToEnd();$process.WaitForExit();if($process.ExitCode -eq 0 -and $version -match 'version "21(?:\.|\")'){return $candidate} } }
    return $null
}
function Test-Port([int]$port) {
    $client = [Net.Sockets.TcpClient]::new()
    try { $task=$client.ConnectAsync('127.0.0.1',$port); return $task.Wait(300) -and $client.Connected } catch { return $false } finally { $client.Dispose() }
}

try {
    New-Item -ItemType Directory -Path (Split-Path $launcherLog -Parent) -Force | Out-Null
    Start-Transcript -LiteralPath $launcherLog -Append | Out-Null
    Write-Host '=== Orushio PVE サーバー起動 ===' -ForegroundColor Cyan
    Show-Step 'Java 21を確認しています。'
    $javaExe = Find-Java21
    if (-not $javaExe) { Stop-WithMessage '64bit版Java 21を確認できません。Java 21をインストールし、Windowsを再起動してから再実行してください。' }
    $env:JAVA_HOME = Split-Path (Split-Path $javaExe -Parent) -Parent
    $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
    Write-Host 'Java 21: OK'

    Show-Step '使用ポートを確認しています。'
    if (Test-Port 25565) { Stop-WithMessage 'Minecraftポート25565が使用中です。既に起動しているサーバーを安全に停止してから再実行してください。' }
    if (Test-Port 8765) { Stop-WithMessage '管理画面ポート8765が使用中です。既に起動しているサーバーを安全に停止してから再実行してください。' }

    Show-Step '現在のソースからpluginをbuildしています。数分かかる場合があります。'
    & (Join-Path $repository 'gradlew.bat') build --no-daemon
    if ($LASTEXITCODE -ne 0) { Stop-WithMessage 'plugin buildに失敗しました。古いJARでは起動しません。表示されたGradleエラーを確認してください。' }
    $builtJar = Join-Path $repository 'build\libs\orushio-pve-bougai-plugin-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $builtJar)) { Stop-WithMessage 'build成果物が見つかりません。build\libsを確認してください。' }

    New-Item -ItemType Directory -Path $plugins -Force | Out-Null
    if (-not (Test-Path -LiteralPath $paperJar) -or (Get-Sha256 $paperJar) -ne $paperSha256) {
        Show-Step '公式Paper 1.21.11を取得・検証しています。'
        $download = "$paperJar.download"
        Remove-Item -LiteralPath $download -Force -ErrorAction SilentlyContinue
        try { Invoke-WebRequest -Headers @{'User-Agent'='orushio-windows-launcher/1.0'} -Uri $paperUrl -OutFile $download } catch { Remove-Item -LiteralPath $download -Force -ErrorAction SilentlyContinue; Stop-WithMessage 'Paper取得に失敗しました。インターネット接続、セキュリティソフト、PaperMCへの接続を確認してください。' }
        if ((Get-Sha256 $download) -ne $paperSha256) { Remove-Item -LiteralPath $download -Force; Stop-WithMessage '取得したPaperの検証に失敗しました。ファイルは配置していません。' }
        Move-Item -LiteralPath $download -Destination $paperJar -Force
    }

    $eula = Join-Path $runtime 'eula.txt'
    if (-not (Test-Path -LiteralPath $eula)) {
        Show-Step '初回のMinecraft EULA確認を行います。'
        Write-Host '初回のみMinecraft EULAへの同意が必要です: https://aka.ms/MinecraftEULA'
        $choice=$Host.UI.PromptForChoice('Minecraft EULA','内容を確認し、同意する場合だけ「同意する」を選択してください。',@('&同意する','&中止'),1)
        if ($choice -ne 0) { Stop-WithMessage 'EULAへ同意していないため起動を中止しました。' }
        Set-Content -LiteralPath $eula -Value 'eula=true' -Encoding ascii
    }
    $properties = Join-Path $runtime 'server.properties'
    if (-not (Test-Path -LiteralPath $properties)) { Set-Content -LiteralPath $properties -Encoding ascii -Value @('online-mode=true','server-ip=127.0.0.1','server-port=25565','level-name=world','view-distance=8','simulation-distance=6','spawn-protection=0') }

    Show-Step '最新plugin JARをPaperへ配置しています。'
    $stagedPlugin = "$pluginJar.new"
    Copy-Item -LiteralPath $builtJar -Destination $stagedPlugin -Force
    Move-Item -LiteralPath $stagedPlugin -Destination $pluginJar -Force
    if (-not (Test-Path -LiteralPath $pluginJar)) { Stop-WithMessage 'plugin配置に失敗しました。runtimeの書込み権限を確認してください。' }
    Remove-Item -LiteralPath $handoff -Force -ErrorAction SilentlyContinue
    if ($PrepareOnly) { Write-Host '起動準備確認が完了しました。'; Stop-Transcript | Out-Null; exit 0 }

    if (-not $NoBrowser) { Show-Step '認証済み管理画面の自動表示を準備しています。';$opener=Join-Path $PSScriptRoot 'open-admin.ps1';if(-not(Test-Path -LiteralPath $opener)){Stop-WithMessage '管理画面起動処理 scripts\open-admin.ps1 が見つかりません。Repositoryのファイル一式を確認してください。'};$arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$opener+'" -Handoff "'+$handoff+'"';try{Start-Process powershell.exe -WindowStyle Hidden -ArgumentList $arguments -ErrorAction Stop | Out-Null}catch{Stop-WithMessage '管理画面起動処理を開始できませんでした。Windows PowerShellが利用可能か確認してください。'} }

    Show-Step 'Paperを起動しています。plugin enable、HTTP bind、管理画面準備の完了を待ってください。'
    Write-Host 'Paperを起動します。管理画面は準備完了後に自動で開きます。' -ForegroundColor Green
    Write-Host '安全に終了するには、このウィンドウで Ctrl+C を1回押してください。'
    Push-Location $runtime
    try { & $javaExe '-Xms1G' '-Xmx2G' '-Dpaper.disableStartupVersionCheck=true' '-jar' $paperJar '--nogui'; $paperExit=$LASTEXITCODE } finally { Pop-Location }
    if ($paperExit -ne 0) { Stop-WithMessage "Paperが異常終了しました（終了コード: $paperExit）。.runtime\paper\logs\latest.logを確認してください。" }
    Write-Host 'サーバーを安全に停止しました。' -ForegroundColor Green
    Stop-Transcript | Out-Null
    exit 0
} catch {
    Write-Host ''
    Write-Host "エラー: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host '解決しない場合は .runtime\paper\logs\latest.log と、この画面のメッセージを確認してください。'
    try { Stop-Transcript | Out-Null } catch {}
    exit 1
}
