# Phase 2 管理ホーム handoff

## 実装概要

- 管理画面のホームへ、オンラインプレイヤー、ホワイトリスト状態、登録一覧を表示する。
- 管理画面からホワイトリストのON/OFF、プレイヤー追加・削除を行う。
- Application Serviceでプレイヤー名検証、重複追加、未登録削除を扱う。
- Paper API操作はすべてサーバーメインスレッドへ委譲し、操作完了後の実状態を返す。
- 読込み失敗や操作失敗を、日本語メッセージと安全なtraceIdで画面へ通知する。

## API

| Method | Path | 用途 |
|---|---|---|
| GET | `/api/v1/players` | オンラインプレイヤー取得（既存APIを新サービスへ統合） |
| GET | `/api/v1/server/whitelist` | ON/OFF状態と登録一覧取得 |
| PUT | `/api/v1/server/whitelist` | ON/OFF切替。Body: `{ "enabled": true }` |
| POST | `/api/v1/server/whitelist/players` | 追加。Body: `{ "name": "Player_1" }` |
| DELETE | `/api/v1/server/whitelist/players` | 削除。Body: `{ "name": "Player_1" }` |

mutationはPhase 1の認証、Host、Origin、CSRF、診断モード拒否、監査ログを通る。

## テスト観点

- オンライン0人／複数人
- whitelist ON／OFF
- 追加／削除／重複追加／未登録削除／不正プレイヤー名
- API正常系、入力不足、Minecraft利用不能時の安全な503応答
- UIの空状態、即時再読込み、確認付き削除、成功・エラー・ローディング表示
- Phase 1全テストの回帰

## 実行結果

- Phase 2関連テスト：成功
- 全体回帰テスト：55件成功、失敗0件
- Gradle build：成功

## 既知の制限・確認事項

- プレイヤー名による追加はPaperの`OfflinePlayer`解決を使用する。Mojangアカウントの存在を外部APIで検証しない。
- Paper実機での表示・操作確認は、テスト用サーバー環境が利用できる場合のみ実施する。
- 既存`docs/05_DEVELOPMENT_PLAN.md`と`docs/06_ACCEPTANCE_TESTS.md`ではPhase 2をマップ管理としているが、今回の正式依頼では管理ホームをPhase 2とする。ユーザー明示指示を優先し、既存仕様書は変更していない。

## 意図的な対象外

PvEゲーム本体、マップ管理、妨害、YouTube、OBS、外部DB、依存関係更新、Phase 1基盤の再設計は行っていない。
