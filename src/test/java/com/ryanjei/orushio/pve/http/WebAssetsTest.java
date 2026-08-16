package com.ryanjei.orushio.pve.http;
import static org.junit.jupiter.api.Assertions.*;import java.nio.charset.StandardCharsets;import org.junit.jupiter.api.Test;
class WebAssetsTest{
    @Test void システム画面は診断APIとescapeを使う()throws Exception{String js=resource("/web/app.js");assertTrue(js.contains("/system/diagnostics"));assertTrue(js.contains("escapeHtml"));assertTrue(js.contains("loadDiagnostics"));}
    @Test void 未対応画面は無効で後続Phase表示を持つ()throws Exception{String html=resource("/web/index.html");assertTrue(html.contains("disabled>マップ"));assertTrue(html.contains("後続Phase"));assertTrue(html.contains("data-page=\"system\""));}
    @Test void ホーム画面は参加者とホワイトリスト操作を提供する()throws Exception{String html=resource("/web/index.html"),js=resource("/web/app.js");assertTrue(html.contains("現在の参加者"));assertTrue(html.contains("ホワイトリスト管理"));assertTrue(html.contains("id=\"whitelist-toggle\""));assertTrue(js.contains("/server/whitelist"));assertTrue(js.contains("confirm("));assertTrue(js.contains("現在参加しているプレイヤーはいません"));}
    @Test void status失敗時もplayersとwhitelistを個別処理する()throws Exception{String js=resource("/web/app.js");assertTrue(js.contains("Promise.allSettled"));assertTrue(js.contains("statusResult.status === 'fulfilled'"));assertTrue(js.contains("playersResult.status === 'fulfilled'"));assertTrue(js.contains("whitelistResult.status === 'fulfilled'"));assertTrue(js.contains("renderPlayers(playersResult.value)"));assertTrue(js.contains("renderWhitelist(whitelistResult.value)"));}
    @Test void mutation成功後の再取得失敗を区別する()throws Exception{String js=resource("/web/app.js");assertTrue(js.contains("refresh({ notifyResult: false })"));assertTrue(js.contains("ただし、最新状態を確認できていません"));assertTrue(js.contains("if (refreshed.complete)"));}
    private static String resource(String path)throws Exception{return new String(WebAssetsTest.class.getResourceAsStream(path).readAllBytes(),StandardCharsets.UTF_8);}
}
