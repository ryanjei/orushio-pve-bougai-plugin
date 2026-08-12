# Phase 1 Codex正式依頼書：基盤・管理画面

## 依頼の目的

`orushio-pve-bougai-plugin` をゼロから立ち上げ、後続Phaseでゲーム機能を二重実装せず追加できるPaperプラグイン基盤を作成してください。

このPhaseでは、Paperプラグイン、状態機械、Application Service境界、安全な永続化、ローカルWeb管理画面の骨格、認証、診断、テスト/CIまでを実装します。ゲーム本体やマップコピーはまだ実装しません。

## 作業前に読む文書

1. `/AGENTS.md`
2. `/docs/00_FIXED_DECISIONS.md`
3. `/docs/01_PRODUCT_SPEC.md`
4. `/docs/02_ADMIN_UI_SPEC.md`
5. `/docs/03_ARCHITECTURE.md`
6. `/docs/04_DATA_AND_API.md`
7. `/docs/05_DEVELOPMENT_PLAN.md`
8. `/docs/06_ACCEPTANCE_TESTS.md`

文書間の優先順位は`AGENTS.md`に従ってください。

## 作業開始時に行うこと

- リポジトリが空であることを確認する。
- `docs/00_FIXED_DECISIONS.md`どおり、Paper 1.21.11、Java 21、Gradle Wrapper、Kotlin DSLを採用する。
- 固定バージョンを独自判断で更新しない。
- 変更予定ファイルと、このPhaseで触らない範囲を短く宣言してから実装する。
- 仕様の重大な矛盾を発見した場合だけ確認する。確定部分は先に進める。

## 実装対象

### 1. Paperプロジェクト

- Java 21
- Paper 1.21.11
- Gradle Wrapper / `build.gradle.kts`
- Paper API
- 固定されたplugin名、package、main classを使う`plugin.yml`
- プラグインのenable/disable
- NMS不使用
- ユーザー向けメッセージは日本語

### 2. Domain Core

- `GameState`：IDLE, RECRUITING, PREPARING, ACTIVE, PAUSED, CLEAR, ABORTING, RECOVERING, MAP_SETUP
- 許可された状態遷移
- 不正遷移の明示的なDomain Error
- `GameSession`、`Participant`、最低限のMapProfile識別子
- Bukkit、HTTP、ファイルI/Oに依存しない純粋Java

### 3. Application境界

- Game/Map/Setup/Disruption/RecoveryのApplication Serviceインターフェースまたは将来追加可能な責務分離
- Phase 1で必要な状態読取り、受付開始/終了の最小Use Case
- operationId、sessionId、状態競合の扱い
- 管理APIがDomainを直接書き換えない構成

### 4. 永続化

- system-config、game-config、active-sessionのRepository
- `schemaVersion`
- 一時ファイルへの書込み後に安全置換
- 直前の正常版バックアップ
- 未知の新しいschemaVersionは上書きしない
- Phase 1では外部DBを導入しない

### 5. 組込み管理HTTP

- プラグイン内で起動・停止
- 既定で`127.0.0.1:8765`だけにbind
- 静的Web UI配信
- JSON API `/api/v1`
- 認証トークンからHttpOnly/SameSite Cookieへの交換
- 状態変更時のOrigin/Host/CSRF検証
- CORS無効
- 任意コマンド・任意ファイル操作API禁止
- 入力エラー、状態競合、内部エラーの共通応答

### 6. Phase 1 API

- `GET /api/v1/status`
- `GET /api/v1/players`
- `GET /api/v1/game/current`
- `POST /api/v1/game/recruiting/start`
- `POST /api/v1/game/recruiting/close`
- `GET /api/v1/system/diagnostics`

ゲーム実装前のため、受付以外のmutationは未実装でよい。未実装操作を成功扱いしないでください。

### 7. 管理画面骨格

- 日本語UI
- 左メニュー：ダッシュボード、ゲーム操作、プレイヤー、マップ、マップ設定、バランス、装備・ショップ、履歴・ログ、システム
- Phase 1で動くのはダッシュボード、受付開始/終了、オンラインプレイヤー読取り、診断
- 後続画面は「Phase 2以降」と明示し、偽の保存や成功表示をしない
- Paper状態、プラグイン版、ゲーム状態、参加人数、警告を表示
- Node/npmビルドを必須にせず、JARへ静的資産を同梱

### 8. Paperスレッド境界

- HTTPスレッドからPaper APIを直接呼ばない
- メインスレッド実行Gatewayを作る
- API応答はPaper側の必要処理完了後に返す
- タイムアウトとプラグイン停止中の拒否を扱う

### 9. ログ・診断

- 操作ログと安全なtraceId
- 認証情報をログへ出さない
- HTTP bind状態、保存領域、設定読込み、ゲーム状態を診断APIへ出す
- 重大エラーでも可能なら診断画面を開ける設計にする

### 10. テスト・CI

- JUnit 5
- Domain状態遷移テスト
- 不正遷移テスト
- Repositoryの保存、バックアップ、破損、schemaVersionテスト
- APIの認証、CSRF/Origin、入力、409状態競合テスト
- メインスレッドGatewayをモック/フェイク化したApplicationテスト
- GitHub Actionsでテストとビルド

## 明示的な対象外

- ワールドZIP取込、コピー、ロード、削除
- マップセットアップのゲーム内クリック
- 敵、コア、ファーム、ショップ、装備配布
- プレイヤースナップショットとテレポート
- 妨害効果
- YouTube/Twitch/TikTok連携
- 外部DB
- Node.jsプロセス
- Windowsランチャー/ショートカット
- インターネット公開
- Folia/NMS

対象外機能の空実装を大量に作らず、責務境界と最小DTOだけにしてください。

## 必須受入条件

`docs/06_ACCEPTANCE_TESTS.md`の「Phase 1受入」をすべて満たすこと。

追加条件：

- `./gradlew test`相当と`./gradlew build`相当が成功する。
- 管理画面のPhase 1対応ボタンが実APIを呼び、未対応ボタンは無効または未実装表示になる。
- プラグイン停止時にHTTPサーバーと実行中タスクを安全停止する。
- 既存仕様書を不要に書き換えない。実装で判明した必要な訂正だけ別項目として報告する。

## セルフレビュー範囲

全面的な一般レビューは不要です。次だけ確認してください。

- HTTPスレッドからPaper APIを直接触っていないか
- 不正な状態遷移の抜けがないか
- 認証・CSRFがmutation全体へ適用されているか
- 保存失敗で正常版を失わないか
- 未実装機能を成功表示していないか
- 同種実装の重複がないか

## テスト実行順

1. Domain単体テスト
2. Persistence/API/Application関連テスト
3. 全テスト
4. ビルド
5. 節目として可能なら最小Paper起動確認を1回

同じ失敗のためにフルテストを繰り返さず、関連テストで直してから全体を1回確認してください。

## Git運用

- ブランチ：`phase/1-foundation`
- コミットは意味のある単位にするが、細分化しすぎない。
- 実装とテストが完了したらPushする。
- `main`へのマージは行わない。
- PR作成が可能なら作成し、URLを報告する。権限がなければPushしたブランチ名と最終コミットIDを報告する。

## 完了報告

`AGENTS.md`の形式に従い、次を必ず含めてください。

1. 実装した内容
2. 変更した主なファイル
3. 実行したテストと結果
4. 未実装・既知の制限
5. 仕様確認が必要な事項
6. ブランチ、コミットID、PR URL
