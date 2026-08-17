# Phase 4.1 ゲームライフサイクル・参加管理基盤 handoff

## 実装範囲

- 管理画面でオンラインプレイヤーから1～4人のゲーム参加者を選択・解除する。
- 有効かつセットアップ済みのマップと、制限時間・攻略通常コア数・敵人数倍率を確認する。
- `IDLE → PREPARING → ACTIVE` を明示操作で進め、準備完了前に進行中へ移行しない。
- 中止・時間切れを `ABORTING → RECOVERING → IDLE` の共通清掃契約へ流す。
- 参加者、開始時人数、マップ、設定、開始・終了時刻、オフライン清掃待ちをactive-sessionへ保存する。
- PREPARING以降の未完了セッションを再起動時に検出し、診断モードで新規操作を拒否する。
- Paper join/quitを参加者の接続状態へ反映し、サーバー基準の期限を非同期タイマーで監視する。
- join/quitは専用single-thread dispatcherへPaperイベント順で投入し、古い非同期イベントが再接続後の状態を上書きしない。plugin停止時はキューをflushして閉じる。
- prepare step失敗時は、失敗したstepを含む実行済みstepを逆順補償する。補償不能時はRECOVERINGへ隔離する。
- cleanup開始前にRECOVERINGを保存し、失敗時も同状態から復旧清掃を再試行できる。

## API

- `GET /api/v1/game/lifecycle?mapId=...`
- `PUT|DELETE /api/v1/game/lifecycle/participants`
- `PUT /api/v1/game/lifecycle/settings`
- `POST /api/v1/game/lifecycle/prepare`
- `POST /api/v1/game/lifecycle/activate`
- `POST /api/v1/game/lifecycle/abort`

mutationは既存と同じlocalhost、認証、Host、Origin、CSRF、diagnostic mode拒否を通す。状態更新はactive-sessionへの保存完了後だけ成功として返す。

## 永続化と既定値

- active-session schemaVersion 1へ後方互換な追加キーとして保存する。旧ファイルでキーがない場合は既定値を使う。
- active-sessionとgame-settingsはschemaVersion 1をRepositoryでも明示検証し、未知・欠落・不正schemaを拒否する。Phase 3のschemaVersion 1 active-sessionだけを既定値補完対象とする。
- マップ別上書きは `maps/<mapId>/game-settings.yml` にAtomicYamlStoreで保存する。
- 空欄（キーなし）は上書きなしを意味し、制限時間60分、攻略通常コア2個、敵人数倍率1.0を使う。
- 開始時の参加者数は固定し、切断で減算しない。

## Phase 4.2以降へ残す契約

- `GameLifecycleStep.prepare/cleanup` がワールド作成、転送、所有物清掃を接続する拡張点である。
- Phase 4.1ではゲーム用一時ワールド作成、インベントリ退避・初期化、PvE敵・コア・資源・ショップを実装しない。
- オフライン参加者はUUID単位のpending cleanupとして保存する。実際のプレイヤー状態復元は、安全な退避情報を持つ後続Phaseで接続する。
- CLEARINGの内部互換名は既存`CLEAR`、RECOVERYの内部互換名は既存`RECOVERING`を維持し、利用者表示だけ正式な日本語名にする。

## 手動確認

1. `START_SERVER.bat`を起動し管理画面を開く。
2. Minecraftで1～4人を接続し、「ゲーム操作」で参加者を追加する。
3. 有効なセットアップ済みマップを選び、空欄時の既定値を確認する。
4. 任意の上書きを保存し、再読込み後も維持されることを確認する。
5. 確認画面から準備を開始し、表示が「ゲーム準備中」のままであることを確認する。
6. 明示操作で「ゲーム進行中」にし、残り時間が減ることを確認する。
7. 参加者を切断しても開始時人数が変わらないことを確認する。
8. 中止後に待機中へ戻ることを確認する。
9. サーバーをYキーで安全停止する。

## 既知の制限

- Phase 4.1はライフサイクルと参加管理基盤のみで、ゲーム用ワールドの作成・転送やPvEプレイは行わない。
- 参加者データ構造は可変長だが、今回の標準UI/Application制限は1～4人である。
- 参加上限は`ParticipantPolicy`を正本とし、標準値4を使用する。Application、status API、lifecycle API、UIは同じPolicy値を参照する。
