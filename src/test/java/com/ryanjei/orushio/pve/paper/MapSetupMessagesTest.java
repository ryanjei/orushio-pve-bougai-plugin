package com.ryanjei.orushio.pve.paper;
import static org.junit.jupiter.api.Assertions.*;import java.util.List;import org.junit.jupiter.api.Test;
class MapSetupMessagesTest{@Test void 単一点は日本語項目名で完了を伝える(){assertEquals(List.of("ファーム開始地点を設定しました。"),MapSetupMessages.completed("farmSpawn",false,true));}@Test void 範囲は一点目二点目完了を日本語で伝える(){assertEquals(List.of("ファーム範囲：1点目を設定しました。"),MapSetupMessages.completed("farmRegion",true,false));assertEquals(List.of("ファーム範囲：2点目を設定しました。","ファーム範囲の設定が完了しました。"),MapSetupMessages.completed("farmRegion",true,true));}}
