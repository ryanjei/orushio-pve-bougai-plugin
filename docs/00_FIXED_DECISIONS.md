# 固定技術・運用判断

この文書は、Codexが実装時に独自選定しないための固定値をまとめる。変更する場合は、機能実装とは別の仕様変更として先に本文書を更新する。

| 項目 | 決定 |
|---|---|
| Minecraft/Paper | Paper 1.21.11 |
| Java | Java 21でコンパイル・実行（より新しいJDKでの実行は互換範囲） |
| ビルド | Gradle Wrapper、Kotlin DSL |
| artifactId | `orushio-pve-bougai-plugin` |
| Paper plugin名 | `OrushioPveBougai` |
| Java package | `com.ryanjei.orushio.pve` |
| Main class | `com.ryanjei.orushio.pve.OrushioPvePlugin` |
| 管理権限 | `orushio.pve.admin`、既定OP |
| 管理画面 | JAR同梱のHTML/CSS/Vanilla JavaScript |
| HTTP実装 | JDK標準HTTPサーバーを第一選択。要件を満たせない根拠がある場合だけ小規模依存を提案 |
| listen | `127.0.0.1:8765`（ポートは設定変更後の再起動で変更可能） |
| 通常URL | `http://127.0.0.1:8765/` |
| 通常認証 | ランダムなインストール秘密＋ブラウザセッション。Phase 4ランチャーがワンタイムbootstrap URLを開く |
| 外部公開 | v1では禁止 |
| データ | 設定・MapProfile・Session・Snapshot・ResultはYAML。監査ログは日次JSON Lines |
| DB | v1では使用しない |
| Node/npm | v1のビルド・実行に使用しない |
| NMS/Folia | v1対象外 |
| 同時ゲーム | 1セッション |
| 参加人数 | 1～4人 |
| マップ | ZIP登録した原本を毎ゲーム一時複製 |
| ゲーム開始 | 管理者が管理画面から開始 |
| 通常プレイヤー操作 | ロビーの参加ブロック右クリック。コマンド不要 |
| 管理者セットアップ | 管理画面でモード選択後、Minecraft内で専用ツールをクリック。コマンド不要 |
| YouTube | Phase 6。v1には接続しない |
| 妨害 | 効果本体はv1、入力は管理画面手動 |

## データ配置

Paper標準のプラグインデータフォルダー `plugins/OrushioPveBougai/` 配下を使用する。

```text
config.yml
game-config.yml
secrets.yml
maps/<mapId>/profile.yml
maps/<mapId>/template/
sessions/active.yml
snapshots/<playerUuid>.yml
results/<sessionId>.yml
logs/audit-YYYY-MM-DD.jsonl
operations/<operationId>.yml
backups/
```

一時ゲームワールドはPaperのワールドコンテナ直下に作る。名前は `orushio_run_<ランダムID>`、セットアップ用は`orushio_setup_<ランダムID>`とし、フォルダー内部にも所有メタデータを置く。接頭辞だけを削除根拠にしてはならない。

## 認証bootstrap

- 初回enable時に十分な長さのランダムなインストール秘密を生成する。
- 秘密を標準ログやAPI応答へ出さない。
- Phase 1ではテストからbootstrap交換処理を検証可能にする。
- Phase 4のローカルランチャーは、短時間・1回限りのbootstrap tokenを安全なローカル連携で取得してブラウザを開く。
- bootstrap tokenをURLからセッションCookieへ交換した後、同じtokenを再利用できない。
- ユーザーへパスワード入力や秘密ファイルの手動コピーを要求しない。

