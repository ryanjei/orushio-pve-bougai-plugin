# Orushio PVE 妨害プラグイン

## このツールは何か

Minecraft Java版のPaperサーバーで、視聴者参加型の妨害＋PvE企画を運営するためのプラグインです。現在のPhase 3では、日本語Web管理画面、参加・ホワイトリスト管理、安全なマップZIP登録、マップセットアップまで利用できます。PvE本体と妨害は後続Phaseです。

## 必要環境

- Windows 10またはWindows 11（64bit）
- 64bit版Java 21
- Minecraft Java Edition 1.21.11
- 初回のPaper取得とGradle buildに使用するインターネット接続
- 約4 GB以上の空きメモリと、ワールドを保存できるディスク空き容量

Paper 1.21.11 build 132は初回起動時に公式配布元から取得・検証します。Paper本体や生成ワールドはGit管理されません。

## 初回起動

1. Repository最上位の `START_SERVER.bat` をダブルクリックします。
   ダブルクリック直後に「Orushio PVE サーバーを起動しています...」と表示されます。
2. 初回だけMinecraft EULAのURLと確認画面が表示されます。内容を確認し、同意する場合だけ「同意する」を選びます。
3. Java確認、現在checkout中のソースのbuild、Paper取得、最新plugin配置が自動で行われます。
4. Paperの起動が完了すると、認証済み管理画面が既定ブラウザで自動的に開きます。
5. Minecraftから接続し、管理画面のオンラインプレイヤー一覧で自分の「セットアップ管理者にする」を押します。
6. 管理画面の「マップ管理」からマップ登録・セットアップを行います。

PowerShell、Git Bash、コマンドプロンプト、Gradleコマンドを利用者が入力する必要はありません。

## 2回目以降の起動

毎回 `START_SERVER.bat` をダブルクリックしてください。現在checkoutされているソースを必ずbuildしてからJARを配置するため、以前の古いJARで起動しません。buildに失敗した場合、Paperは起動されません。

## 管理画面

- URL：`http://127.0.0.1:8765/`
- `START_SERVER.bat`から起動した場合、Paper準備完了後に自動で開きます。
- プラグインが短時間・1回限りのbootstrap tokenを一時ファイルで起動補助へ渡し、ブラウザで交換します。
- tokenの手入力、installation secretの入力、`secrets.yml`の閲覧は不要です。
- 管理画面はlocalhostだけで待ち受け、外部へ公開しません。

管理画面を閉じた後に再度認証が必要になった場合は、サーバーを安全に停止して `START_SERVER.bat` から再起動してください。

## Minecraft接続

- 接続先：`127.0.0.1:25565`
- 対応クライアント：Minecraft Java Edition 1.21.11
- 接続後、管理画面のオンラインプレイヤー一覧で自分を「セットアップ管理者にする」に設定してください。Paperコンソールでの`op`コマンド入力は不要です。
- 管理画面には「管理者利用可能」または「管理者権限なし」が表示され、セットアップ開始候補には利用可能なプレイヤーだけが表示されます。
- Phase 3ではサーバーをlocalhostへ限定しています。

## 終了方法

`START_SERVER.bat`で開いたサーバーウィンドウを選び、`Ctrl+C`を1回押してください。Paperの停止処理が完了し「サーバーを安全に停止しました」と表示されるまで、ウィンドウを強制終了したりPCをシャットダウンしたりしないでください。

## データ保存場所

実行データはRepository最上位の `.runtime/paper/` に保存され、Git管理対象から除外されます。

- Paper本体・通常ワールド：`.runtime/paper/`
- plugin設定・秘密情報：`.runtime/paper/plugins/OrushioPveBougai/`
- マップ原本：`.runtime/paper/plugins/OrushioPveBougai/maps/`
- active session：`.runtime/paper/plugins/OrushioPveBougai/sessions/`
- 監査ログ：`.runtime/paper/plugins/OrushioPveBougai/logs/`
- Paperログ：`.runtime/paper/logs/latest.log`

`.runtime`を削除するとワールド、設定、マップ等も失われます。バックアップなしで削除しないでください。

## トラブル時

- `START_SERVER.bat`は、失敗時に日本語の原因と確認先を表示したまま停止します。PowerShell、Git Bash、cmd、Gradle、Javaコマンドを手入力する必要はありません。
- 起動処理の記録は `.runtime/paper/logs/launcher.log`、Paperとpluginの記録は `.runtime/paper/logs/latest.log` に保存されます。
- 管理画面の自動表示に失敗した場合は日本語ダイアログが表示されます。既定ブラウザ設定と上記ログを確認してください。

- Javaエラー：`java -version`等の入力は不要です。64bit版Java 21をインストールし、Windowsを再起動してから再実行してください。
- build失敗：起動画面に表示されたGradleエラーを確認してください。古いpluginでは起動しません。
- Paper取得失敗：インターネット接続、セキュリティソフト、PaperMCへの接続を確認してください。
- ポート使用中：既に開いているサーバーを安全に停止してから再実行してください。
- 管理画面が開かない：Paper画面に起動完了が表示されているか確認し、セキュリティソフトがlocalhost通信を遮断していないか確認してください。
- Paper起動失敗：`.runtime/paper/logs/latest.log`を確認してください。

起動失敗時はウィンドウを自動で閉じず、日本語の原因と確認先を表示します。

## 開発者向け

実装担当者は最初に [AGENTS.md](AGENTS.md) と対象Phaseの正式依頼書を確認してください。

- [仕様書索引](docs/README.md)
- [固定技術・運用判断](docs/00_FIXED_DECISIONS.md)
- [製品仕様](docs/01_PRODUCT_SPEC.md)
- [管理UI仕様](docs/02_ADMIN_UI_SPEC.md)
- [アーキテクチャ](docs/03_ARCHITECTURE.md)
- [データ・API契約](docs/04_DATA_AND_API.md)
- [開発計画](docs/05_DEVELOPMENT_PLAN.md)
- [受入テスト](docs/06_ACCEPTANCE_TESTS.md)
- [Phase 3正式依頼書](docs/requests/PHASE_3_CODEX_REQUEST.md)
- [Phase 3 handoff](docs/handoffs/PHASE_3_MAP_MANAGEMENT_HANDOFF.md)
