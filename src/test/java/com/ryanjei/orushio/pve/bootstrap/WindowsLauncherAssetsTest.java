package com.ryanjei.orushio.pve.bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WindowsLauncherAssetsTest {
    @TempDir Path temp;

    @Test void repositoryTopLevelHasVisibleOneClickLauncherAndRuntimeIsIgnored() throws Exception {
        byte[] bytes=Files.readAllBytes(Path.of("START_SERVER.bat"));
        String bat=new String(bytes,StandardCharsets.UTF_8),ignore=Files.readString(Path.of(".gitignore")),attributes=Files.readString(Path.of(".gitattributes"));
        assertTrue(bat.contains("OPBP サーバーを起動しています"));
        assertTrue(bat.contains("scripts\\start-server.ps1"));
        assertTrue(bat.contains("if not exist \"%ORUSHIO_SCRIPT%\""));
        assertTrue(bat.contains("if not exist \"%ORUSHIO_POWERSHELL%\""));
        assertTrue(bat.contains("set \"ORUSHIO_EXIT=%ERRORLEVEL%\""));
        assertTrue(bat.contains("pause >nul"));assertTrue(ignore.lines().anyMatch(line->line.equals(".runtime/")));assertTrue(attributes.lines().anyMatch(line->line.equals("*.bat text eol=crlf")));
        for(int index=0;index<bytes.length;index++)if(bytes[index]=='\n')assertTrue(index>0&&bytes[index-1]=='\r',"BATはCRLFで保存する");
    }

    @Test void missingPowerShellScriptProducesReadableErrorAndNonZeroExit() throws Exception {
        Path bat=temp.resolve("START_SERVER.bat");Files.copy(Path.of("START_SERVER.bat"),bat);
        Process process=new ProcessBuilder("cmd.exe","/d","/c",bat.toString()).redirectErrorStream(true).start();
        process.getOutputStream().write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));process.getOutputStream().close();
        assertTrue(process.waitFor(10,TimeUnit.SECONDS));String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        assertNotEquals(0,process.exitValue());assertTrue(output.contains("OPBP"));assertTrue(output.contains("start-server.ps1"));
    }

    @Test void nonZeroPowerShellExitProducesReadableErrorAndPausePath() throws Exception {
        Path bat=temp.resolve("START_SERVER.bat"),scripts=Files.createDirectories(temp.resolve("scripts"));Files.copy(Path.of("START_SERVER.bat"),bat);Files.writeString(scripts.resolve("start-server.ps1"),"Write-Host 'POWERSHELL_FAILURE'; exit 7",StandardCharsets.US_ASCII);
        Process process=new ProcessBuilder("cmd.exe","/d","/c",bat.toString()).redirectErrorStream(true).start();process.getOutputStream().write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));process.getOutputStream().close();
        assertTrue(process.waitFor(15,TimeUnit.SECONDS));String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        assertNotEquals(0,process.exitValue());assertTrue(output.contains("POWERSHELL_FAILURE"));assertTrue(output.contains("7"));assertTrue(output.contains("ウィンドウ"));
    }

    @Test void launcherBuildsBeforeDeployAndExplainsAllRequiredFailures() throws Exception {
        String script=Files.readString(Path.of("scripts/start-server.ps1"));int build=script.indexOf("build --no-daemon"),failure=script.indexOf("plugin buildに失敗"),deploy=script.indexOf("Copy-Item -LiteralPath $builtJar"),paper=script.indexOf("$paperInfo.Arguments");
        assertTrue(build>=0&&build<failure&&failure<deploy&&deploy<paper);assertTrue(script.contains("if ($LASTEXITCODE -ne 0)"));
        for(String message:new String[]{"Java 21を確認できません","ポート25565が使用中","管理画面ポート8765が使用中","Paper取得に失敗","検証に失敗","EULAへ同意","plugin配置に失敗","Paperが異常終了","open-admin.ps1 が見つかりません"})assertTrue(script.contains(message),message);
        assertTrue(script.contains("launcher.log"));assertTrue(script.contains("Show-Step"));
    }

    @Test void launcherPinsPaperAndAdminOpenerReportsBootstrapAndBrowserFailures() throws Exception {
        String script=Files.readString(Path.of("scripts/start-server.ps1")),opener=Files.readString(Path.of("scripts/open-admin.ps1"));
        assertTrue(script.contains("paper-1.21.11-132.jar"));assertTrue(script.contains("5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"));assertTrue(script.contains("Security.Cryptography.SHA256"));assertTrue(opener.contains("http://127.0.0.1:8765/auth/bootstrap?token=*"));assertTrue(opener.indexOf("Remove-Item -LiteralPath $Handoff")<opener.indexOf("Start-Process $url"));assertTrue(opener.contains("管理画面の準備が120秒以内"));assertTrue(opener.contains("既定ブラウザの設定"));assertFalse(script.contains("secrets.yml"));
    }
    @Test void launcherUsesAuthenticatedPluginShutdownWithoutStdinOrKill()throws Exception{String script=Files.readString(Path.of("scripts/start-server.ps1"));assertTrue(script.contains("Y キーを押してください"));assertTrue(script.contains("/launcher/shutdown"));assertTrue(script.contains("X-OPBP-Shutdown-Token"));assertTrue(script.contains("StatusCode -ne 202"));assertTrue(script.contains("安全停止を要求できませんでした。サーバーはまだ稼働しています"));assertTrue(script.contains("データ保護のため強制終了せず待機します"));assertFalse(script.contains("RedirectStandardInput"));assertFalse(script.contains("StandardInput."));assertFalse(script.contains("stopBytes"));assertFalse(script.contains("Stop-Process"));assertFalse(script.contains("Write-Host $shutdownToken"));assertTrue(script.contains("$paperProcess.WaitForExit"));assertTrue(script.contains("Paper終了コード=$paperExit"));assertTrue(script.contains("OPBPサーバーを安全に停止しました"));assertTrue(script.contains("Paperが異常終了しました"));assertTrue(script.contains("CtrlPressed")||script.contains("ConsoleModifiers]::Control"));assertFalse(script.contains("bootstrap?token="));assertFalse(script.contains("installationSecret"));}
}
