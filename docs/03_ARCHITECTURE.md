# アーキテクチャ仕様 v1

## 1. 採用構成

初期版は、PaperプラグインJARへ管理HTTPサーバーと静的Web UIを同梱する。Node.js、外部DB、常駐クラウドは使用しない。

```text
Browser UI
   │ localhost HTTP/JSON
   ▼
Admin API Adapter ── Auth / Validation / DTO
   ▼
Application Services ── Game / Map / Setup / Disruption / Recovery
   ▼
Domain Core ── State machine / Rules / Models
   ▼
Paper Adapter ── World / Player / Entity / Scheduler / Events
   ▼
Paper Server
```

保存はRepository Interface経由でYAMLファイルへ行い、監査ログだけをJSON Linesとする。UIやDomainは直接ファイルへ触れない。

## 2. 技術基準

- サーバー：Paper 1.21.11。
- Java：21。
- ビルド：Gradle Wrapper + Kotlin DSL。
- フロントエンド：同梱するHTML/CSS/Vanilla JavaScript。Node/npmビルドを必須にしない。
- HTTP：JDK標準HTTPサーバー。要件を満たせない根拠がある場合だけ、小規模な組込みサーバー依存を実装前に提案する。
- 永続化：ローカルファイル。初期版にSQLite等を導入しない。
- テスト：JUnit 5、Domain/Applicationの単体テスト、Paper境界の統合テスト。
- NMS不使用。

Paper公式もGradle/Kotlin DSLを中心に案内している。バージョン更新は別作業として扱う。

## 3. モジュール責務

### 3.1 Domain

- `GameState`と許可された状態遷移
- `GameSession`, `Participant`, `MapProfile`, `CoreDefinition`
- 人数倍率、抽選、HP、妨害対象選択など純粋なルール
- Bukkit、HTTP、ファイルへの依存禁止

### 3.2 Application

- `GameApplicationService`
- `MapApplicationService`
- `SetupApplicationService`
- `DisruptionApplicationService`
- `RecoveryApplicationService`
- Use Case単位の入力検証、状態検証、トランザクション相当の制御
- 全操作にoperationIdとsessionIdを付ける

### 3.3 Paper Adapter

- Event Listener
- Entity/World/Player Gateway
- メインスレッド実行Gateway
- Boss Bar、Title、Particle、Potion Effect
- PDC所有タグ
- Paper起動・停止イベント

### 3.4 Admin API Adapter

- REST風JSON API
- 認証、Origin検証、レート制御
- DTOとDomain入力の変換
- Application Service完了まで待って結果を返す
- 任意サーバーコマンド実行機能を持たない

### 3.5 Persistence Adapter

- 設定、マッププロファイル、セッション、スナップショット、結果、ログ
- 一時ファイル→flush→置換
- 正常版バックアップ
- schemaVersionとマイグレーション

## 4. スレッド設計

### メインスレッド限定

- World load/unloadのPaper API呼出
- Player teleport、inventory、effect
- Entity生成・削除
- Eventに伴うゲーム状態の最終反映

### 非同期

- ZIP検証・展開
- ワールドディレクトリコピー・削除
- 設定/結果のファイルI/O
- 静的ファイル配信

### 境界ルール

- HTTPスレッドはPaper APIを呼ばない。
- Application Serviceは`GameThreadExecutor`経由でPaper操作を行う。
- 非同期処理完了時にsessionId/operationIdが現在も有効か確認する。
- タイムアウトで処理を二重実行しない。結果不明状態を明示する。

## 5. ワールドライフサイクル

1. mapIdから正規化済み原本パスをRepositoryで解決。
2. 原本に所有メタデータと検証済みハッシュがあることを確認。
3. 新しいランダムrunIdを生成。
4. サーバーワールドコンテナ直下の許可された接頭辞パスへ非同期コピー。
5. コピー完了後、メインスレッドでロード。
6. ゲーム内変更は一時ワールドだけに行う。
7. Recoveryで参加者を外へ出す。
8. メインスレッドで保存せずアンロード。
9. 所有メタデータ、実パス、接頭辞、ルート境界を再検証。
10. 非同期削除。失敗時はpending-cleanupへ登録。

原本と一時ワールドのパスを文字列置換で推測しない。

## 6. 所有タグ

プラグイン生成物に最低限次を付ける。

- `orushio:owned = true`
- `orushio:session_id`
- `orushio:entity_type`（enemy/core/core_hitbox/shop/display）
- `orushio:definition_id`

クリーンアップは`owned=true`かつ一致するsessionIdだけを対象とする。セットアップ用生成物はsetupSessionIdを使用する。

## 7. プレイヤースナップショット

- ゲーム転送前に永続保存を完了する。
- UUIDごとに1つの未復元スナップショットだけを許可する。
- 既存の未復元データがあれば新ゲームへ参加させず、先に復元する。
- 復元は冪等にし、完了マーカーを書いてから削除する。
- オフライン終了者は次回ログイン時にロビーで復元する。
- インベントリの直列化はPaper/Bukkitの安定APIを使用し、Javaオブジェクトの無制限デシリアライズを避ける。

## 8. 管理画面セキュリティ

- 初期版は`127.0.0.1`へだけbindする。
- 起動ごとまたはインストールごとの管理トークンを使用し、HttpOnly/SameSite Cookieへ交換する。
- 状態変更APIはOrigin/HostとCSRFトークンを検証する。
- CORSは許可しない。
- 認証失敗回数を制限する。
- UIから任意コマンド、任意クラス名、任意ファイルパスを受け取らない。
- mapIdは`[a-z0-9-]{1,40}`に制限する。
- ZIPはファイル数、圧縮前後サイズ、展開率、パス深度に上限を設ける。
- HTMLへログやプレイヤー名を出す際はエスケープする。

## 9. 障害復旧

起動時に次を順に確認する。

1. 未復元プレイヤースナップショット
2. `PREPARING`以降で残ったセッション記録
3. 所有メタデータ付き一時ワールド
4. 未完了operation
5. 破損した設定のバックアップ

復旧できない項目があってもPaperプラグイン全体を無言で停止せず、管理画面を診断モードで起動して対処を表示する。危険な自動削除は行わない。

## 10. 将来のYouTube連携境界

将来のYouTube Adapterは外部イベントを次の正規化入力へ変換するだけにする。

```text
AudienceEvent {
  eventId
  platform
  occurredAt
  actorHash
  kind
  normalizedText
  metadata
}
```

ルールエンジンが`AudienceEvent`を`DisruptionRequest`へ変換し、v1と同じ`DisruptionApplicationService`を呼ぶ。YouTube AdapterからPaper APIを直接操作しない。

## 11. パッケージ構成例

```text
src/main/java/.../orushio/
  domain/
  application/
  adapter/paper/
  adapter/http/
  adapter/persistence/
  bootstrap/
src/main/resources/
  plugin.yml
  web/
  defaults/
src/test/java/.../
```

これは責務境界の指定であり、細かなクラス数を増やすこと自体を目的にしない。
