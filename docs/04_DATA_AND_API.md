# データ・管理API契約 v1

## 1. 永続データ

設定、MapProfile、Session、Snapshot、Result、operationはYAML、監査ログは日次JSON Linesで保存する。保存場所は`00_FIXED_DECISIONS.md`を正本とする。

| データ | 保持期間 | 目的 |
|---|---|---|
| system-config | 永続 | HTTP、管理者、保存上限 |
| game-config | 永続 | ゲーム開始設定（制限時間、攻略Core数、敵人数倍率） |
| gameplay-settings | 永続 | Map別の資源、Point、Enemy、Core、Shop、初期装備 |
| map-profile | 永続 | mapIdと登録地点・範囲 |
| map-template | 永続 | 原本ワールド |
| active-session | ゲーム中 | 状態、runId、参加者、進捗 |
| pending-operation | 完了まで | コピー、削除、ZIP取込 |
| game-result | 永続/保持上限あり | 履歴 |
| audit-log | ローテーション | 管理操作・警告・エラー |

すべての設定文書に`schemaVersion`を持たせる。未知の新しいschemaVersionは上書きせず、読込みを停止して診断表示する。

## 2. 主なモデル

### MapProfile

```text
mapId
displayName
enabled
templateRevision
validationStatus
farmRegion
farmSpawn
combatEntry
normalCoreCandidates[]
finalCore
gateRegions[]
finalRegion
finalEntryTrigger
checkpoints[]
enemyZones[]
resourceZones[]
shopPoints[]
createdAt
updatedAt
```

Phase 3で永続化するMapProfile契約は、`mapId`、`displayName`、`enabled`、原本位置を示す`templateDirectory`、地点・範囲、`createdAt`である。`templateRevision`、`validationStatus`、`updatedAt`は実マップ更新・再検証を扱うPhase 6で追加する将来フィールドであり、Phase 3のschemaVersion 1には保存しない。未知の新しいschemaVersionを拒否する既存方針は維持する。

地点は`x,y,z,yaw,pitch`、範囲は正規化済み`minX,minY,minZ,maxX,maxY,maxZ`で保存する。原本ワールド名やUUIDを地点ごとに重複保存せず、mapIdへ従属させる。

### GameSession

```text
sessionId
state
mapId
runId
runtimeWorldName
participants[]
participantCountAtStart
difficultySnapshot
startedAt
activeAt
endedAt
selectedNormalCoreIds[]
destroyedCoreIds[]
finalCoreHp
teamCheckpointIndex
disruptionCounts
endReason
```

### Participant

```text
playerUuid
lastKnownName
connected
deathCount
lastSafeLocationType
orePoints
farmingPoints
```

PointはParticipant UUIDごとの個人残高とし、GameSession全体の共有財布として保存しない。Shop購入は購入者本人の残高だけを消費する。active-sessionへ追加する場合はschemaVersion 1の安全なoptional keyとしてPhase 4.4で具体形式を定義し、旧Sessionの進行を推測復元しない。

## 3. 設定の適用

- ゲーム開始時に`game-config`から`difficultySnapshot`と使用設定をセッションへ複製する。
- 実行中ゲームは原則としてスナップショットを使う。
- 表示時間やログレベルなど安全な項目だけ即時反映する。
- UIの保存APIは、各項目が即時か次回適用かを返す。

## 4. API共通形式

ベース：`http://127.0.0.1:8765/api/v1`

成功：

```json
{
  "ok": true,
  "data": {},
  "operationId": "optional",
  "applied": "immediate|next_game"
}
```

失敗：

```json
{
  "ok": false,
  "error": {
    "code": "GAME_STATE_CONFLICT",
    "message": "現在はゲーム準備中のため実行できません。",
    "fields": {},
    "traceId": "safe-id"
  }
}
```

HTTPステータスを適切に使い、常に200でエラーを包まない。

## 5. 主要エンドポイント

### 状態・プレイヤー

- `GET /status`
- `GET /players`
- `POST /players/{uuid}/participant`
- `DELETE /players/{uuid}/participant`
- `POST /players/{uuid}/restore`

### ゲーム

- `POST /game/recruiting/start`
- `POST /game/recruiting/close`
- `POST /game/prepare`
- `POST /game/activate`
- `POST /game/pause`
- `POST /game/resume`
- `POST /game/abort`
- `POST /game/rescue`
- `GET /game/current`

各mutationに期待する現在状態またはsessionIdを渡し、古い画面からの操作を拒否する。

### 妨害

- `GET /disruptions`
- `POST /disruptions/{id}/trigger`

入力は対象方式と定義済みパラメータだけ。Potion名やコマンド文字列を直接受け取らない。

### マップ

- `GET /maps`
- `POST /maps/import`（multipart、非同期operation）
- `GET /maps/{mapId}`
- `PATCH /maps/{mapId}`
- `POST /maps/{mapId}/validate`
- `POST /maps/{mapId}/test-load`
- `POST /maps/{mapId}/setup/start`
- `POST /maps/{mapId}/enable`
- `POST /maps/{mapId}/disable`
- `POST /maps/{mapId}/select-next`
- `DELETE /maps/{mapId}`

### セットアップ

- `GET /setup/current`
- `POST /setup/select-mode`
- `POST /setup/selection/confirm`
- `POST /setup/selection/cancel`
- `POST /setup/validate`
- `POST /setup/save-and-close`
- `POST /setup/discard-and-close`

ゲーム内クリック結果はWebSocketまたは短いポーリングでUIへ通知する。v1は実装量が少ない方法を採用し、UI契約を変えない。

### 設定・履歴

- `GET /settings/{group}`
- `PUT /settings/{group}`
- `POST /settings/{group}/reset-defaults`
- `GET /history`
- `GET /history/{sessionId}`
- `GET /logs`

### システム

- `GET /operations/{operationId}`
- `GET /system/diagnostics`
- `POST /system/recover`
- `POST /system/shutdown`

`shutdown`は二段階確認トークンを要求し、ゲーム中はRecovery完了後にPaperを安全停止する。

## 6. 冪等性・競合

- 長時間mutationはoperationIdを返し、同じidempotency keyで再送された場合は同じ結果を返す。
- `prepare`, `abort`, `shutdown`, map import/deleteは二重実行しない。
- UIは409を受けたら現在状態を再取得して表示を更新する。
- ゲーム操作はsessionId一致を必須とする。

## 7. 入力制限の初期標準

- mapId：英小文字、数字、ハイフン、1～40文字
- 表示名：1～60文字
- ZIP：Phase 3の正式上限は512 MiB。展開後上限2 GiB、ファイル数20,000。HTTP受信とZIP検証は同じ定数を使用する。
- 範囲：各軸と体積に上限。巨大範囲は警告または拒否
- 数値：仕様の意味に応じたmin/maxをDTOで検証
- 文字列：制御文字を拒否、HTML出力時にエスケープ

## 8. エラーコード例

- `AUTH_REQUIRED`
- `FORBIDDEN`
- `INVALID_INPUT`
- `GAME_STATE_CONFLICT`
- `SESSION_MISMATCH`
- `MAP_NOT_READY`
- `MAP_IMPORT_REJECTED`
- `SETUP_NOT_ACTIVE`
- `PLAYER_NOT_ONLINE`
- `SNAPSHOT_RESTORE_REQUIRED`
- `OPERATION_IN_PROGRESS`
- `WORLD_LOAD_FAILED`
- `RECOVERY_REQUIRED`
- `INTERNAL_ERROR`
